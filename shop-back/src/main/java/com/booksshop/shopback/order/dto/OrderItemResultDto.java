package com.booksshop.shopback.order.dto;

public class OrderItemResultDto {

    private final String bookId;
    private final String title;
    private final String author;
    private final String publisher;
    private final String coverImage;
    private final Integer quantity;
    private final Integer amount;

    public OrderItemResultDto(String bookId, String title, String author, String publisher,
                               String coverImage, Integer quantity, Integer amount) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.coverImage = coverImage;
        this.quantity = quantity;
        this.amount = amount;
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

    public String getCoverImage() {
        return coverImage;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Integer getAmount() {
        return amount;
    }
}
