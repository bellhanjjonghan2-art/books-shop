package com.booksshop.shopback.order.dto;

import java.util.List;

public record OrderCreateRequest(List<OrderItemRequest> items, DeliveryRequest delivery) {
}
