package com.booksshop.shopback.auth.dto;

import com.booksshop.shopback.user.User;

public record UserSummaryResponse(
        String userId,
        String names,
        String roles
) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(user.getUserId(), user.getNames(), user.getRoles());
    }
}
