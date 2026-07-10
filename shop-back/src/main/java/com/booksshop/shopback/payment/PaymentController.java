package com.booksshop.shopback.payment;

import com.booksshop.shopback.common.ApiResponse;
import com.booksshop.shopback.payment.dto.PaymentConfirmRequest;
import com.booksshop.shopback.payment.dto.PaymentConfirmResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/confirm")
    public ApiResponse<PaymentConfirmResponse> confirm(Authentication authentication,
                                                        @RequestBody PaymentConfirmRequest request) {
        String userId = authentication.getName();
        return ApiResponse.ok(paymentService.confirm(userId, request));
    }
}
