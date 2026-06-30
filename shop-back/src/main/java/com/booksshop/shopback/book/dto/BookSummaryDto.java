package com.booksshop.shopback.book.dto;

public class BookSummaryDto {

    private final String id;
    private final String title;
    private final String coverImage;
    private final String author;
    private final Integer salePrice;

    public BookSummaryDto(String id, String title, String coverImage, String author, Integer salePrice) {
        this.id = id;
        this.title = title;
        this.coverImage = coverImage;
        this.author = author;
        this.salePrice = salePrice;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public String getAuthor() {
        return author;
    }

    public Integer getSalePrice() {
        return salePrice;
    }
}
