package com.booksshop.shopback.order.dto;

public record OrderCreateResponse(String orderId, String orderName, Integer amount) {
}
