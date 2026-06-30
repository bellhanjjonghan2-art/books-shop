package com.booksshop.shopback.auth.dto;

public record LoginRequest(
        String userId,
        String passwd
) {
    public boolean isInvalid() {
        return userId == null || userId.isBlank() || passwd == null || passwd.isBlank();
    }
}
