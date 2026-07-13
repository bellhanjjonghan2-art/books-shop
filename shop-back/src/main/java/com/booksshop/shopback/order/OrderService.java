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
import com.booksshop.shopback.order.dto.OrderListItemBatchDto;
import com.booksshop.shopback.order.dto.OrderListItemDto;
import com.booksshop.shopback.order.dto.OrderListResponse;
import com.booksshop.shopback.order.dto.OrderResultResponse;
import com.booksshop.shopback.order.dto.OrderSummaryDto;
import com.booksshop.shopback.payment.Delivery;
import com.booksshop.shopback.payment.DeliveryRepository;
import com.booksshop.shopback.payment.DeliveryStatus;
import com.booksshop.shopback.payment.Payment;
import com.booksshop.shopback.payment.PaymentRepository;
import com.booksshop.shopback.payment.PaymentStatus;
import com.booksshop.shopback.user.User;
import com.booksshop.shopback.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final String ORDER_NAME_SUFFIX_FORMAT = "%s 외 %d건";
    private static final int DELIVERY_FEE_FREE = 0;

    private static final Set<String> VALID_ORDER_PERIODS = Set.of("1m", "3m", "6m", "all");
    private static final String ORDER_PERIOD_ALL = "all";
    private static final int DEFAULT_ORDER_LIST_PAGE = 0;
    private static final int DEFAULT_ORDER_LIST_SIZE = 5;

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

    public OrderListResponse getOrders(String userId, String period, int page, int size) {
        if (!VALID_ORDER_PERIODS.contains(period)) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_PERIOD);
        }

        int safePage = Math.max(page, DEFAULT_ORDER_LIST_PAGE);
        int safeSize = size < 1 ? DEFAULT_ORDER_LIST_SIZE : size;
        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createAt"));

        Page<Order> orderPage = ORDER_PERIOD_ALL.equals(period)
                ? orderRepository.findByUserIdAndStatus(userId, OrderStatus.PAID, pageRequest)
                : orderRepository.findByUserIdAndStatusAndCreateAtGreaterThanEqual(
                        userId, OrderStatus.PAID, resolvePeriodStart(period), pageRequest);

        List<Order> orders = orderPage.getContent();
        if (orders.isEmpty()) {
            return new OrderListResponse(orderPage.getTotalElements(), safePage, safeSize, orderPage.getTotalPages(), List.of());
        }

        List<String> orderIds = orders.stream().map(Order::getId).toList();

        // N+1 방지: 주문별 상품/배송을 각각 order_id IN (...)으로 한 번에 조회 후 자바에서 그룹핑한다.
        Map<String, List<OrderListItemDto>> itemsByOrderId = orderItemRepository.findListItemsByOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(
                        OrderListItemBatchDto::orderId,
                        Collectors.mapping(OrderListItemBatchDto::toItemDto, Collectors.toList())));

        Map<String, DeliveryStatus> deliveryStatusByOrderId = deliveryRepository.findByOrder_IdIn(orderIds).stream()
                .collect(Collectors.toMap(delivery -> delivery.getOrder().getId(), Delivery::getStatus));

        List<OrderSummaryDto> summaries = orders.stream()
                .map(order -> toOrderSummaryDto(order, itemsByOrderId, deliveryStatusByOrderId))
                .toList();

        return new OrderListResponse(orderPage.getTotalElements(), safePage, safeSize, orderPage.getTotalPages(), summaries);
    }

    private OrderSummaryDto toOrderSummaryDto(Order order, Map<String, List<OrderListItemDto>> itemsByOrderId,
                                               Map<String, DeliveryStatus> deliveryStatusByOrderId) {
        // 정상적으로는 주문 생성 시 deliveries가 항상 선(先)생성되지만, 방어적으로 기본값 PREPARING을 둔다.
        DeliveryStatus deliveryStatus = deliveryStatusByOrderId.getOrDefault(order.getId(), DeliveryStatus.PREPARING);
        List<OrderListItemDto> items = itemsByOrderId.getOrDefault(order.getId(), List.of());
        return new OrderSummaryDto(
                order.getId(),
                order.getCreateAt().toLocalDate().toString(),
                deliveryStatus.name(),
                order.getTotalAmount(),
                items
        );
    }

    private LocalDateTime resolvePeriodStart(String period) {
        int months = switch (period) {
            case "1m" -> 1;
            case "3m" -> 3;
            case "6m" -> 6;
            default -> throw new BusinessException(ErrorCode.INVALID_ORDER_PERIOD);
        };
        return LocalDateTime.now().minusMonths(months);
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
