package com.booksshop.shopback.cart;

import com.booksshop.shopback.book.Book;
import com.booksshop.shopback.common.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "cart_items")
@AttributeOverrides({
        @AttributeOverride(name = "createAt", column = @Column(name = "created_at", updatable = false)),
        @AttributeOverride(name = "updateAt", column = @Column(name = "updated_at"))
})
public class CartItem extends BaseEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    protected CartItem() {
    }

    public CartItem(String id, String userId, Book book, Integer quantity) {
        this.id = id;
        this.userId = userId;
        this.book = book;
        this.quantity = quantity;
    }

    public void changeQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void increaseQuantity(int amount) {
        this.quantity += amount;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public Book getBook() {
        return book;
    }

    public Integer getQuantity() {
        return quantity;
    }
}
