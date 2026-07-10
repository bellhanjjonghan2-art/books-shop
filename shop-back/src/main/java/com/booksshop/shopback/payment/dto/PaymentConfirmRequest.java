package com.booksshop.shopback.payment.dto;

public record PaymentConfirmRequest(String paymentKey, String orderId, Integer amount) {
}
