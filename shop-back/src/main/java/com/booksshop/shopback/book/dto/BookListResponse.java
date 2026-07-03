package com.booksshop.shopback.book.dto;

import java.util.List;

public class BookListResponse {

    private final List<BookListItemDto> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public BookListResponse(
            List<BookListItemDto> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public List<BookListItemDto> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
