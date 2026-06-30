package com.booksshop.shopback.auth;

import com.booksshop.shopback.auth.dto.LoginRequest;
import com.booksshop.shopback.auth.dto.LoginResponse;
import com.booksshop.shopback.auth.dto.UserSummaryResponse;
import com.booksshop.shopback.common.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserSummaryResponse> me(Authentication authentication) {
        String userId = authentication.getName();
        return ApiResponse.ok(authService.getCurrentUser(userId));
    }
}
