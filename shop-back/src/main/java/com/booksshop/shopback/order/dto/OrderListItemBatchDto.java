package com.booksshop.shopback.order.dto;

// 여러 주문의 상품을 order_id IN (...)으로 한 번에 조회하기 위한 내부 프로젝션.
// 조회 후 orderId 기준으로 그룹핑해 OrderListItemDto(orderId 없는 응답용 DTO)로 변환한다.
public record OrderListItemBatchDto(
        String orderId,
        String bookId,
        String title,
        String coverImage,
        Integer quantity,
        Integer amount
) {

    public OrderListItemDto toItemDto() {
        return new OrderListItemDto(bookId, title, coverImage, quantity, amount);
    }
}
