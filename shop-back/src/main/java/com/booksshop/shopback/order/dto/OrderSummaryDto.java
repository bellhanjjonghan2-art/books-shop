package com.booksshop.shopback.order.dto;

import java.util.List;

public record OrderSummaryDto(
        String orderId,
        String orderDate,
        String deliveryStatus,
        Integer totalAmount,
        List<OrderListItemDto> items
) {
}
