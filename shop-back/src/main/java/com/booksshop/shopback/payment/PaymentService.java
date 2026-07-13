package com.booksshop.shopback.payment;

import com.booksshop.shopback.book.BookRepository;
import com.booksshop.shopback.common.BusinessException;
import com.booksshop.shopback.common.ErrorCode;
import com.booksshop.shopback.order.Order;
import com.booksshop.shopback.order.OrderItem;
import com.booksshop.shopback.order.OrderItemRepository;
import com.booksshop.shopback.order.OrderRepository;
import com.booksshop.shopback.order.OrderStatus;
import com.booksshop.shopback.payment.dto.PaymentConfirmRequest;
import com.booksshop.shopback.payment.dto.PaymentConfirmResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private static final String OUT_OF_STOCK_CANCEL_REASON = "재고 부족";
    private static final String DEFAULT_CURRENCY = "KRW";
    private static final String DEFAULT_CANCEL_STATUS = "DONE";
    private static final int PAYMENT_KEY_LOG_PREFIX_LENGTH = 8;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookRepository bookRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentCancelRepository paymentCancelRepository;
    private final DeliveryRepository deliveryRepository;
    private final TossPaymentClient tossPaymentClient;

    public PaymentService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                           BookRepository bookRepository, PaymentRepository paymentRepository,
                           PaymentCancelRepository paymentCancelRepository, DeliveryRepository deliveryRepository,
                           TossPaymentClient tossPaymentClient) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.bookRepository = bookRepository;
        this.paymentRepository = paymentRepository;
        this.paymentCancelRepository = paymentCancelRepository;
        this.deliveryRepository = deliveryRepository;
        this.tossPaymentClient = tossPaymentClient;
    }

    // BusinessException 발생 시에도 그 직전까지의 변경(예: ABORTED/CANCELED 기록)은 커밋되어야 하므로
    // 기본 롤백 정책(RuntimeException 전체 롤백)에서 BusinessException만 예외로 둔다.
    @Transactional(noRollbackFor = BusinessException.class)
    public PaymentConfirmResponse confirm(String userId, PaymentConfirmRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() == OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID);
        }
        if (!order.getTotalAmount().equals(request.amount())) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        log.info("토스 결제 승인 요청: orderId={}, amount={}, paymentKey={}...",
                request.orderId(), request.amount(), maskPaymentKey(request.paymentKey()));

        TossConfirmResult confirmResult = tossPaymentClient.confirm(request.paymentKey(), request.orderId(), request.amount());
        if (!confirmResult.success()) {
            // 여기가 핵심 — 토스가 실제로 뭐라고 거부했는지(UNAUTHORIZED_KEY 등)는 이 rawBody 안의 code/message에 있다.
            log.warn("토스 결제 승인 실패: orderId={}, httpStatus={}, rawBody={}",
                    request.orderId(), confirmResult.statusCode(), confirmResult.rawBody());
            recordAbortedPayment(order, request, confirmResult.rawBody());
            order.markFailed();
            throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }

        JSONObject confirmJson = new JSONObject(confirmResult.rawBody());
        Payment payment = buildDonePayment(order, confirmJson, confirmResult.rawBody());
        paymentRepository.save(payment);
        order.markPaid();

        // deliveries는 주문 생성 시점에 이미 선(先)생성되어 있으므로 여기서는 새로 만들지 않는다.
        decreaseStockOrRefund(order, payment);

        return PaymentConfirmResponse.of(order, payment);
    }

    private void decreaseStockOrRefund(Order order, Payment payment) {
        List<OrderItem> orderItems = orderItemRepository.findByOrder_Id(order.getId());
        List<OrderItem> decreasedSoFar = new ArrayList<>();
        for (OrderItem item : orderItems) {
            int updatedRows = bookRepository.decreaseStock(item.getBook().getId(), item.getQuantity());
            if (updatedRows == 0) {
                handleOutOfStock(order, payment, decreasedSoFar);
                throw new BusinessException(ErrorCode.OUT_OF_STOCK);
            }
            decreasedSoFar.add(item);
        }
    }

    // 주문 전체가 취소되므로, 이미 차감된 다른 항목의 재고를 원복하고 토스 결제를 자동 환불한다.
    private void handleOutOfStock(Order order, Payment payment, List<OrderItem> decreasedSoFar) {
        for (OrderItem decreased : decreasedSoFar) {
            bookRepository.increaseStock(decreased.getBook().getId(), decreased.getQuantity());
        }

        String cancelRawBody = tossPaymentClient.cancel(payment.getPaymentKey(), OUT_OF_STOCK_CANCEL_REASON);
        PaymentCancel paymentCancel = buildPaymentCancel(payment, cancelRawBody);
        paymentCancelRepository.save(paymentCancel);

        // increaseStock() 호출(위 for문)의 clearAutomatically=true 때문에 order/payment는 이미 영속성
        // 컨텍스트에서 detach된 상태다. 여기서 markCanceled()만 호출하면 dirty checking이 안 걸려 커밋 시
        // 유실되므로, save()(merge)로 명시적으로 재부착해 변경을 반영한다.
        payment.markCanceled();
        paymentRepository.save(payment);
        order.markCanceled();
        orderRepository.save(order);

        // 주문 생성 시점에 선(先)생성해 둔 deliveries 행도 취소 상태로 전환한다.
        // findByOrder_Id는 (detach 이후) 새로 조회하는 쿼리라 결과 엔티티는 이미 관리 상태이므로 save() 불필요.
        deliveryRepository.findByOrder_Id(order.getId())
                .ifPresent(Delivery::markCanceled);
    }

    private void recordAbortedPayment(Order order, PaymentConfirmRequest request, String rawBody) {
        String failCode = null;
        String failMessage = null;
        JSONObject json = parseJsonSafely(rawBody);
        if (json != null) {
            failCode = json.optString("code", null);
            failMessage = json.optString("message", null);
        }

        Payment payment = Payment.aborted(
                UUID.randomUUID().toString(), order, request.paymentKey(),
                request.amount(), DEFAULT_CURRENCY, failCode, failMessage, rawBody
        );
        paymentRepository.save(payment);
    }

    private Payment buildDonePayment(Order order, JSONObject json, String rawBody) {
        String paymentKey = json.optString("paymentKey", null);
        String method = json.optString("method", null);

        String easyPayProvider = null;
        JSONObject easyPay = json.optJSONObject("easyPay");
        if (easyPay != null) {
            easyPayProvider = easyPay.optString("provider", null);
        }

        Integer totalAmount = json.has("totalAmount") ? (Integer) json.optInt("totalAmount") : null;
        Integer balanceAmount = json.has("balanceAmount") ? (Integer) json.optInt("balanceAmount") : null;
        Integer suppliedAmount = json.has("suppliedAmount") ? (Integer) json.optInt("suppliedAmount") : null;
        Integer vat = json.has("vat") ? (Integer) json.optInt("vat") : null;
        Integer taxFreeAmount = json.has("taxFreeAmount") ? (Integer) json.optInt("taxFreeAmount") : null;
        String currency = json.optString("currency", DEFAULT_CURRENCY);
        OffsetDateTime requestedAt = parseOffsetDateTime(json.optString("requestedAt", null));
        OffsetDateTime approvedAt = parseOffsetDateTime(json.optString("approvedAt", null));

        String receiptUrl = null;
        JSONObject receipt = json.optJSONObject("receipt");
        if (receipt != null) {
            receiptUrl = receipt.optString("url", null);
        }
        String lastTransactionKey = json.optString("lastTransactionKey", null);

        return Payment.done(
                UUID.randomUUID().toString(), order, paymentKey, method, easyPayProvider,
                totalAmount, balanceAmount, suppliedAmount, vat, taxFreeAmount, currency,
                requestedAt, approvedAt, receiptUrl, lastTransactionKey, rawBody
        );
    }

    private PaymentCancel buildPaymentCancel(Payment payment, String rawBody) {
        JSONObject json = new JSONObject(rawBody);
        JSONArray cancels = json.optJSONArray("cancels");
        JSONObject latestCancel = (cancels != null && !cancels.isEmpty())
                ? cancels.getJSONObject(cancels.length() - 1)
                : json;

        String transactionKey = latestCancel.optString("transactionKey", UUID.randomUUID().toString());
        int cancelAmount = latestCancel.has("cancelAmount") ? latestCancel.optInt("cancelAmount") : payment.getTotalAmount();
        String cancelReason = latestCancel.optString("cancelReason", OUT_OF_STOCK_CANCEL_REASON);
        Integer taxFreeAmount = latestCancel.has("taxFreeAmount") ? (Integer) latestCancel.optInt("taxFreeAmount") : null;
        String cancelStatus = latestCancel.optString("cancelStatus", DEFAULT_CANCEL_STATUS);
        OffsetDateTime canceledAt = parseOffsetDateTime(latestCancel.optString("canceledAt", null));

        return new PaymentCancel(
                UUID.randomUUID().toString(), payment.getId(), transactionKey,
                cancelAmount, cancelReason, taxFreeAmount, cancelStatus,
                canceledAt != null ? canceledAt : OffsetDateTime.now()
        );
    }

    private OffsetDateTime parseOffsetDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(value);
    }

    private JSONObject parseJsonSafely(String rawBody) {
        try {
            return new JSONObject(rawBody);
        } catch (Exception e) {
            return null;
        }
    }

    private String maskPaymentKey(String paymentKey) {
        if (paymentKey == null || paymentKey.isBlank()) {
            return "(empty)";
        }
        return paymentKey.substring(0, Math.min(PAYMENT_KEY_LOG_PREFIX_LENGTH, paymentKey.length()));
    }
}
