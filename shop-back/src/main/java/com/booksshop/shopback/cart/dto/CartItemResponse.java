package com.booksshop.shopback.cart.dto;

import com.booksshop.shopback.book.Book;
import com.booksshop.shopback.cart.CartItem;

public class CartItemResponse {

    private final String itemId;
    private final String bookId;
    private final String title;
    private final String author;
    private final String publisher;
    private final Integer salePrice;
    private final Integer quantity;

    public CartItemResponse(String itemId, String bookId, String title, String author,
                             String publisher, Integer salePrice, Integer quantity) {
        this.itemId = itemId;
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.salePrice = salePrice;
        this.quantity = quantity;
    }

    public static CartItemResponse from(CartItem cartItem) {
        Book book = cartItem.getBook();
        return new CartItemResponse(
                cartItem.getId(),
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublisher(),
                book.getSalePrice(),
                cartItem.getQuantity()
        );
    }

    public String getItemId() {
        return itemId;
    }

    public String getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getPublisher() {
        return publisher;
    }

    public Integer getSalePrice() {
        return salePrice;
    }

    public Integer getQuantity() {
        return quantity;
    }
}
