package com.booksshop.shopback.order;

import com.booksshop.shopback.common.ApiResponse;
import com.booksshop.shopback.order.dto.OrderCreateRequest;
import com.booksshop.shopback.order.dto.OrderCreateResponse;
import com.booksshop.shopback.order.dto.OrderResultResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderCreateResponse> createOrder(Authentication authentication,
                                                         @RequestBody OrderCreateRequest request) {
        String userId = authentication.getName();
        return ApiResponse.ok(orderService.createOrder(userId, request));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResultResponse> getOrderResult(Authentication authentication,
                                                            @PathVariable String orderId) {
        String userId = authentication.getName();
        return ApiResponse.ok(orderService.getOrderResult(userId, orderId));
    }
}
