package com.booksshop.shopback.order.dto;

import java.util.List;

public record OrderListResponse(
        Long totalCount,
        int page,
        int size,
        int totalPages,
        List<OrderSummaryDto> items
) {
}
