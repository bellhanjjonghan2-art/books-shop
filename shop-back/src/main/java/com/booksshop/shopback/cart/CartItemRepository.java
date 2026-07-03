package com.booksshop.shopback.cart;

import com.booksshop.shopback.cart.dto.CartItemResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, String> {

    @Query("SELECT new com.booksshop.shopback.cart.dto.CartItemResponse(" +
           "c.id, b.id, b.title, b.author, b.publisher, b.salePrice, c.quantity, b.coverImage) " +
           "FROM CartItem c JOIN c.book b " +
           "WHERE c.userId = :userId " +
           "ORDER BY c.createAt DESC")
    List<CartItemResponse> findCartItemsByUserId(@Param("userId") String userId);

    Optional<CartItem> findByIdAndUserId(String id, String userId);

    List<CartItem> findByIdInAndUserId(List<String> ids, String userId);

    Optional<CartItem> findByUserIdAndBook_Id(String userId, String bookId);
}
