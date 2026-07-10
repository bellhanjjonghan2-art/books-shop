package com.booksshop.shopback.order.dto;

public record DeliveryRequest(String receiverName, String receiverPhone, String postCode,
                               String address, String addrDetail, String deliveryMemo) {
}
