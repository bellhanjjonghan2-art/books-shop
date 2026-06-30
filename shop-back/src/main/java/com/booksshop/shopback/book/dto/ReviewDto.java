package com.booksshop.shopback.book.dto;

import com.booksshop.shopback.book.Review;

import java.time.LocalDateTime;

public class ReviewDto {

    private final Long id;
    private final String reviewerName;
    private final Integer rating;
    private final String content;
    private final LocalDateTime reviewDate;

    public ReviewDto(Review review) {
        this.id = review.getId();
        this.reviewerName = review.getReviewerName();
        this.rating = review.getRating();
        this.content = review.getContent();
        this.reviewDate = review.getReviewDate();
    }

    public Long getId() {
        return id;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public Integer getRating() {
        return rating;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getReviewDate() {
        return reviewDate;
    }
}
