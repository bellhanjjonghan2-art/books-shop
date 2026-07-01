package com.booksshop.shopback.cart;

import com.booksshop.shopback.book.Book;
import com.booksshop.shopback.book.BookRepository;
import com.booksshop.shopback.cart.dto.CartApiResult;
import com.booksshop.shopback.cart.dto.CartItemResponse;
import com.booksshop.shopback.cart.dto.CartRegisterRequest;
import com.booksshop.shopback.cart.dto.CartUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CartService {

    private static final String ID_PREFIX = "item-";
    private static final int ID_RANDOM_DIGITS = 8;
    private static final int ID_GENERATE_MAX_ATTEMPTS = 5;
    private static final int FIXED_REGISTER_QUANTITY = 1;

    private static final String MESSAGE_QUANTITY_UPDATED = "수량 변경 완료";
    private static final String MESSAGE_DELETED = "삭제 완료";
    private static final String MESSAGE_ITEM_NOT_FOUND = "장바구니 항목을 찾을 수 없습니다.";
    private static final String MESSAGE_INVALID_QUANTITY = "수량은 1 이상이어야 합니다.";
    private static final String MESSAGE_BOOK_NOT_FOUND = "도서를 찾을 수 없습니다.";

    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public CartService(CartItemRepository cartItemRepository, BookRepository bookRepository) {
        this.cartItemRepository = cartItemRepository;
        this.bookRepository = bookRepository;
    }

    public List<CartItemResponse> getCartItems(String userId) {
        return cartItemRepository.findCartItemsByUserId(userId);
    }

    @Transactional
    public CartApiResult<Void> updateQuantity(String userId, CartUpdateRequest request) {
        Optional<CartItem> found = cartItemRepository.findByIdAndUserId(request.itemId(), userId);
        if (found.isEmpty()) {
            return CartApiResult.ofMessage(HttpStatus.NOT_FOUND.value(), MESSAGE_ITEM_NOT_FOUND);
        }
        if (request.quantity() == null || request.quantity() <= 0) {
            return CartApiResult.ofMessage(HttpStatus.BAD_REQUEST.value(), MESSAGE_INVALID_QUANTITY);
        }

        found.get().changeQuantity(request.quantity());
        return CartApiResult.ofMessage(HttpStatus.OK.value(), MESSAGE_QUANTITY_UPDATED);
    }

    @Transactional
    public CartApiResult<Void> deleteItems(String userId, List<String> itemIds) {
        List<CartItem> found = cartItemRepository.findByIdInAndUserId(itemIds, userId);
        // 이미 없는 id는 조용히 무시하고, 존재하는 것만 삭제한다.
        cartItemRepository.deleteAll(found);
        return CartApiResult.ofMessage(HttpStatus.OK.value(), MESSAGE_DELETED);
    }

    @Transactional
    public CartApiResult<CartItemResponse> addOrIncrement(String userId, CartRegisterRequest request) {
        Optional<Book> bookOptional = bookRepository.findById(request.bookId());
        if (bookOptional.isEmpty()) {
            return CartApiResult.ofMessage(HttpStatus.NOT_FOUND.value(), MESSAGE_BOOK_NOT_FOUND);
        }
        Book book = bookOptional.get();

        CartItem cartItem = cartItemRepository.findByUserIdAndBook_Id(userId, request.bookId())
                .map(existing -> {
                    existing.increaseQuantity(FIXED_REGISTER_QUANTITY);
                    return existing;
                })
                .orElseGet(() -> cartItemRepository.save(
                        new CartItem(generateId(), userId, book, FIXED_REGISTER_QUANTITY)
                ));

        return CartApiResult.ofData(HttpStatus.OK.value(), CartItemResponse.from(cartItem));
    }

    private String generateId() {
        for (int attempt = 0; attempt < ID_GENERATE_MAX_ATTEMPTS; attempt++) {
            String candidate = ID_PREFIX + randomDigits();
            if (!cartItemRepository.existsById(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("장바구니 아이템 ID 생성에 실패했습니다.");
    }

    private String randomDigits() {
        int bound = (int) Math.pow(10, ID_RANDOM_DIGITS);
        int value = secureRandom.nextInt(bound);
        return String.format("%0" + ID_RANDOM_DIGITS + "d", value);
    }
}
