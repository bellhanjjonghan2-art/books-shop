package com.booksshop.shopback.book.dto;

import com.booksshop.shopback.book.Book;

import java.time.LocalDate;
import java.util.List;

public class BookDetailResponse {

    private final String bookId;
    private final String title;
    private final String coverImage;
    private final String author;
    private final Integer listPrice;
    private final Integer salePrice;
    private final String publisher;
    private final Integer stocks;
    private final LocalDate publishedDate;
    private final String description;
    private final long reviewTotalCount;
    private final List<ReviewDto> reviewList;

    public BookDetailResponse(Book book, List<ReviewDto> reviewList, long reviewTotalCount) {
        this.bookId = book.getId();
        this.title = book.getTitle();
        this.coverImage = book.getCoverImage();
        this.author = book.getAuthor();
        this.listPrice = book.getListPrice();
        this.salePrice = book.getSalePrice();
        this.publisher = book.getPublisher();
        this.stocks = book.getStocks();
        this.publishedDate = book.getPublishedDate();
        this.description = book.getDescription();
        this.reviewTotalCount = reviewTotalCount;
        this.reviewList = reviewList;
    }

    public String getBookId() {
        return bookId;
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

    public Integer getListPrice() {
        return listPrice;
    }

    public Integer getSalePrice() {
        return salePrice;
    }

    public String getPublisher() {
        return publisher;
    }

    public Integer getStocks() {
        return stocks;
    }

    public LocalDate getPublishedDate() {
        return publishedDate;
    }

    public String getDescription() {
        return description;
    }

    public long getReviewTotalCount() {
        return reviewTotalCount;
    }

    public List<ReviewDto> getReviewList() {
        return reviewList;
    }
}
