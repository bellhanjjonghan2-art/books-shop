package com.booksshop.shopback.order.dto;

public record OrderListItemDto(
        String bookId,
        String title,
        String coverImage,
        Integer quantity,
        Integer amount
) {
}
