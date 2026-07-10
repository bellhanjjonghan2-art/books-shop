package com.booksshop.shopback.payment.dto;

import com.booksshop.shopback.order.Order;
import com.booksshop.shopback.payment.Payment;

import java.time.OffsetDateTime;

public record PaymentConfirmResponse(String orderId, String orderName, String method,
                                      Integer totalAmount, OffsetDateTime approvedAt, String receiptUrl) {

    public static PaymentConfirmResponse of(Order order, Payment payment) {
        return new PaymentConfirmResponse(
                order.getId(), order.getOrderName(), payment.getMethod(),
                payment.getTotalAmount(), payment.getApprovedAt(), payment.getReceiptUrl()
        );
    }
}
