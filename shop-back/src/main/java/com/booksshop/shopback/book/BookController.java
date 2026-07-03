package com.booksshop.shopback.book;

import com.booksshop.shopback.book.dto.BookDetailResponse;
import com.booksshop.shopback.book.dto.BookListResponse;
import com.booksshop.shopback.book.dto.MainPageBooksResponse;
import com.booksshop.shopback.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public ApiResponse<MainPageBooksResponse> getMainPageBooks() {
        return ApiResponse.ok(bookService.getMainPageBooks());
    }

    @GetMapping("/{bookId}")
    public ApiResponse<BookDetailResponse> getBook(
            @PathVariable String bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(bookService.getBookDetail(bookId, page, size));
    }

    // /api/books/{bookId}(세그먼트 1개)와 세그먼트 개수가 달라 패턴이 겹치지 않는다.
    @GetMapping("/category/{types}")
    public ApiResponse<BookListResponse> getBooksByCategory(
            @PathVariable String types,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "new") String orderType
    ) {
        return ApiResponse.ok(bookService.getBooksByCategory(types, page, size, orderType));
    }
}
