package com.booksshop.shopback.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserSummaryResponse user
) {
}
