package com.booksshop.shopback.book.dto;

public class BookOrderItemDto {

    private final String bookId;
    private final String title;
    private final String author;
    private final String coverImage;
    private final String categoryName;
    private final Integer salePrice;

    public BookOrderItemDto(String bookId, String title, String author, String coverImage,
                             String categoryName, Integer salePrice) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.coverImage = coverImage;
        this.categoryName = categoryName;
        this.salePrice = salePrice;
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

    public String getCoverImage() {
        return coverImage;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public Integer getSalePrice() {
        return salePrice;
    }
}
