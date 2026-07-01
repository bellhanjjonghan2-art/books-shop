package com.booksshop.shopback.cart.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 카트 API 전용 응답 포맷. 프로젝트 공통 ApiResponse({"success":..,"data":..}) 대신
 * docs/cartItem.md 에 명시된 {"code":..,"data":..} / {"code":..,"message":..} 리터럴 포맷을 그대로 표현한다.
 * 값이 없는 필드(data 또는 message)는 직렬화에서 제외한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartApiResult<T> {

    private final int code;
    private final T data;
    private final String message;

    private CartApiResult(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public static <T> CartApiResult<T> ofData(int code, T data) {
        return new CartApiResult<>(code, data, null);
    }

    public static <T> CartApiResult<T> ofMessage(int code, String message) {
        return new CartApiResult<>(code, null, message);
    }

    public int getCode() {
        return code;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
}
