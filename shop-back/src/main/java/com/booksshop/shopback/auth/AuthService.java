package com.booksshop.shopback.auth;

import com.booksshop.shopback.auth.dto.LoginRequest;
import com.booksshop.shopback.auth.dto.LoginResponse;
import com.booksshop.shopback.auth.dto.UserSummaryResponse;
import com.booksshop.shopback.common.BusinessException;
import com.booksshop.shopback.common.ErrorCode;
import com.booksshop.shopback.user.User;
import com.booksshop.shopback.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse login(LoginRequest request) {
        if (request.isInvalid()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        // 아이디 없음/비밀번호 불일치를 구분하지 않고 동일한 AUTH_FAILED로 응답한다 (보안상 정보 노출 방지).
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_FAILED));

        if (!passwordEncoder.matches(request.passwd(), user.getPasswd())) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }

        String accessToken = jwtTokenProvider.createToken(user.getUserId(), user.getNames(), user.getRoles());

        return new LoginResponse(
                accessToken,
                TOKEN_TYPE,
                jwtTokenProvider.getExpirationSeconds(),
                UserSummaryResponse.from(user)
        );
    }

    public UserSummaryResponse getCurrentUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID));
        return UserSummaryResponse.from(user);
    }
}
