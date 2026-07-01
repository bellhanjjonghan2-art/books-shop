package com.booksshop.shopback.cart;

import com.booksshop.shopback.cart.dto.CartApiResult;
import com.booksshop.shopback.cart.dto.CartItemResponse;
import com.booksshop.shopback.cart.dto.CartRegisterRequest;
import com.booksshop.shopback.cart.dto.CartUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 장바구니 API. 프론트(shop-front useCarts.js)의 PUT/DELETE 뮤테이션은 onError 핸들러가 없어
 * axios가 4xx/5xx를 받으면 사용자에게 아무 피드백도 가지 않는다. 그래서 PUT/DELETE(및 동일 원칙을
 * 적용하는 POST)는 논리적 오류 상황에서도 항상 HTTP 200을 유지하고, 의미상 오류 코드는 응답
 * body의 code/message로 전달한다. 응답 포맷도 공통 ApiResponse가 아닌 docs/cartItem.md에
 * 명시된 리터럴 포맷({"code":..,"data":..} / {"code":..,"message":..})을 그대로 사용한다.
 */
@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartApiResult<List<CartItemResponse>> getCarts(Authentication authentication) {
        String userId = authentication.getName();
        return CartApiResult.ofData(HttpStatus.OK.value(), cartService.getCartItems(userId));
    }

    @PutMapping
    public CartApiResult<Void> updateQuantity(Authentication authentication, @RequestBody CartUpdateRequest request) {
        String userId = authentication.getName();
        return cartService.updateQuantity(userId, request);
    }

    @DeleteMapping
    public CartApiResult<Void> deleteItems(Authentication authentication, @RequestParam("items") String items) {
        String userId = authentication.getName();
        List<String> itemIds = Arrays.stream(items.split(","))
                .map(String::trim)
                .filter(itemId -> !itemId.isEmpty())
                .toList();
        return cartService.deleteItems(userId, itemIds);
    }

    @PostMapping
    public CartApiResult<CartItemResponse> registerCartItem(Authentication authentication,
                                                              @RequestBody CartRegisterRequest request) {
        String userId = authentication.getName();
        return cartService.addOrIncrement(userId, request);
    }
}
