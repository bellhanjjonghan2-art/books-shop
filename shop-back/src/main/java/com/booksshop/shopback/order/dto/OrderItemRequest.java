package com.booksshop.shopback.order.dto;

public record OrderItemRequest(String bookId, Integer quantity) {
}
