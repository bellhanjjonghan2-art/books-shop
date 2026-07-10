package com.booksshop.shopback.order;

import com.booksshop.shopback.book.Book;
import com.booksshop.shopback.book.BookRepository;
import com.booksshop.shopback.common.BusinessException;
import com.booksshop.shopback.common.ErrorCode;
import com.booksshop.shopback.order.dto.DeliveryRequest;
import com.booksshop.shopback.order.dto.OrderCreateRequest;
import com.booksshop.shopback.order.dto.OrderCreateResponse;
import com.booksshop.shopback.order.dto.OrderItemRequest;
import com.booksshop.shopback.order.dto.OrderItemResultDto;
import com.booksshop.shopback.order.dto.OrderResultResponse;
import com.booksshop.shopback.payment.Delivery;
import com.booksshop.shopback.payment.DeliveryRepository;
import com.booksshop.shopback.payment.Payment;
import com.booksshop.shopback.payment.PaymentRepository;
import com.booksshop.shopback.payment.PaymentStatus;
import com.booksshop.shopback.user.User;
import com.booksshop.shopback.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final String ORDER_NAME_SUFFIX_FORMAT = "%s 외 %d건";
    private static final int DELIVERY_FEE_FREE = 0;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final DeliveryRepository deliveryRepository;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                         BookRepository bookRepository, UserRepository userRepository,
                         PaymentRepository paymentRepository, DeliveryRepository deliveryRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.deliveryRepository = deliveryRepository;
    }

    @Transactional
    public OrderCreateResponse createOrder(String userId, OrderCreateRequest request) {
        List<String> bookIds = request.items().stream()
                .map(OrderItemRequest::bookId)
                .toList();
        Map<String, Book> bookMap = bookRepository.findAllById(bookIds).stream()
                .collect(Collectors.toMap(Book::getId, Function.identity()));

        // 가격은 클라이언트를 신뢰하지 않고 서버가 지금 DB에서 다시 조회해 확정한다.
        List<ResolvedItem> resolvedItems = new ArrayList<>();
        int totalAmount = 0;
        for (OrderItemRequest itemRequest : request.items()) {
            Book book = bookMap.get(itemRequest.bookId());
            if (book == null) {
                throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
            }
            int amount = book.getSalePrice() * itemRequest.quantity();
            totalAmount += amount;
            resolvedItems.add(new ResolvedItem(book, itemRequest.quantity(), amount));
        }

        String orderName = buildOrderName(resolvedItems);
        DeliveryRequest deliveryRequest = request.delivery();

        Order order = new Order(UUID.randomUUID().toString(), userId, orderName, totalAmount);
        orderRepository.save(order);

        for (ResolvedItem resolvedItem : resolvedItems) {
            orderItemRepository.save(new OrderItem(
                    UUID.randomUUID().toString(), order, resolvedItem.book(),
                    resolvedItem.book().getTitle(), resolvedItem.book().getSalePrice(),
                    resolvedItem.quantity(), resolvedItem.amount()
            ));
        }

        // deliveries는 스키마 변경 없이도 배송 정보를 전부 담을 수 있으므로,
        // 결제 승인을 기다리지 않고 주문 생성 시점에 바로 선(先)생성해 둔다(주문당 1건, order_id UNIQUE).
        Delivery delivery = new Delivery(
                UUID.randomUUID().toString(), order,
                deliveryRequest.receiverName(), deliveryRequest.receiverPhone(), deliveryRequest.postCode(),
                deliveryRequest.address(), deliveryRequest.addrDetail(), deliveryRequest.deliveryMemo()
        );
        deliveryRepository.save(delivery);

        return new OrderCreateResponse(order.getId(), order.getOrderName(), order.getTotalAmount());
    }

    public OrderResultResponse getOrderResult(String userId, String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        // 타인 주문 조회는 존재 노출 방지를 위해 404(ORDER_NOT_FOUND)로 동일하게 응답한다.
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        List<OrderItemResultDto> items = orderItemRepository.findResultItemsByOrderId(orderId);
        int productAmount = items.stream().mapToInt(OrderItemResultDto::getAmount).sum();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("주문자 사용자 정보를 찾을 수 없습니다: " + userId));
        String method = paymentRepository.findByOrder_IdAndStatus(orderId, PaymentStatus.DONE)
                .map(Payment::getMethod)
                .orElse(null);
        OrderResultResponse.OrdererDto orderer = new OrderResultResponse.OrdererDto(
                user.getNames(), user.getPhone(), user.getEmail(), method);

        OrderResultResponse.DeliveryResultDto deliveryDto = deliveryRepository.findByOrder_Id(orderId)
                .map(this::toDeliveryResultDto)
                .orElse(null);

        return new OrderResultResponse(
                order.getId(),
                order.getOrderName(),
                order.getStatus().name(),
                order.getCreateAt().toLocalDate().toString(),
                items,
                productAmount,
                DELIVERY_FEE_FREE,
                order.getTotalAmount(),
                orderer,
                deliveryDto
        );
    }

    private OrderResultResponse.DeliveryResultDto toDeliveryResultDto(Delivery delivery) {
        return new OrderResultResponse.DeliveryResultDto(
                delivery.getReceiverName(), delivery.getReceiverPhone(), delivery.getPostCode(),
                delivery.getAddress(), delivery.getAddrDetail(), delivery.getDeliveryMemo(),
                delivery.getStatus().name()
        );
    }

    private String buildOrderName(List<ResolvedItem> resolvedItems) {
        String firstTitle = resolvedItems.get(0).book().getTitle();
        if (resolvedItems.size() == 1) {
            return firstTitle;
        }
        return String.format(ORDER_NAME_SUFFIX_FORMAT, firstTitle, resolvedItems.size() - 1);
    }

    private record ResolvedItem(Book book, Integer quantity, Integer amount) {
    }
}
