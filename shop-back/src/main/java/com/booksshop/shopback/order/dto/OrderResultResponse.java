package com.booksshop.shopback.order.dto;

import java.util.List;

public record OrderResultResponse(
        String orderId,
        String orderName,
        String status,
        String createdAt,
        List<OrderItemResultDto> items,
        Integer productAmount,
        Integer deliveryFee,
        Integer totalAmount,
        OrdererDto orderer,
        DeliveryResultDto delivery
) {

    public record OrdererDto(String name, String phone, String email, String method) {
    }

    public record DeliveryResultDto(String receiverName, String receiverPhone, String postCode,
                                     String address, String addrDetail, String deliveryMemo, String status) {
    }
}
