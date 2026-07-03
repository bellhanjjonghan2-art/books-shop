package com.booksshop.shopback.book.dto;

import java.time.LocalDate;

public class BookListItemDto {

    private final String bookId;
    private final String title;
    private final String subtitle;
    private final String author;
    private final String publisher;
    private final LocalDate publishedDate;
    private final long totalReviewCnt;
    private final Double avgRating;
    private final Integer listPrice;
    private final Integer salePrice;
    private final String coverImage;
    private final String bestYn;
    private final String newYn;

    public BookListItemDto(
            String bookId,
            String title,
            String subtitle,
            String author,
            String publisher,
            LocalDate publishedDate,
            Long totalReviewCnt,
            Double avgRating,
            Integer listPrice,
            Integer salePrice,
            String coverImage,
            String bestYn,
            String newYn
    ) {
        this.bookId = bookId;
        this.title = title;
        this.subtitle = subtitle;
        this.author = author;
        this.publisher = publisher;
        this.publishedDate = publishedDate;
        this.totalReviewCnt = totalReviewCnt == null ? 0L : totalReviewCnt;
        this.avgRating = avgRating;
        this.listPrice = listPrice;
        this.salePrice = salePrice;
        this.coverImage = coverImage;
        this.bestYn = bestYn;
        this.newYn = newYn;
    }

    public String getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getAuthor() {
        return author;
    }

    public String getPublisher() {
        return publisher;
    }

    public LocalDate getPublishedDate() {
        return publishedDate;
    }

    public long getTotalReviewCnt() {
        return totalReviewCnt;
    }

    // Review.rating은 1~5 만점이라, 화면 표시 스케일(10점 만점)에 맞춰 평균값에 2를 곱해 내려준다.
    public Double getReviewRating() {
        return avgRating == null ? null : Math.round(avgRating * 2 * 10) / 10.0;
    }

    public Integer getListPrice() {
        return listPrice;
    }

    public Integer getSalePrice() {
        return salePrice;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public String getBestYn() {
        return bestYn;
    }

    public String getNewYn() {
        return newYn;
    }
}
