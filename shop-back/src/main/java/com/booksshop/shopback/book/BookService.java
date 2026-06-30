package com.booksshop.shopback.book;

import com.booksshop.shopback.book.dto.BookDetailResponse;
import com.booksshop.shopback.book.dto.BookSummaryDto;
import com.booksshop.shopback.book.dto.MainPageBooksResponse;
import com.booksshop.shopback.book.dto.ReviewDto;
import com.booksshop.shopback.common.BusinessException;
import com.booksshop.shopback.common.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookService {

    private static final int TOP_N = 5;

    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;

    public BookService(BookRepository bookRepository, ReviewRepository reviewRepository) {
        this.bookRepository = bookRepository;
        this.reviewRepository = reviewRepository;
    }

    public MainPageBooksResponse getMainPageBooks() {
        PageRequest pageRequest = PageRequest.of(0, TOP_N, Sort.unsorted());

        List<BookSummaryDto> bestTopN = bookRepository.findBestTopN(pageRequest);
        List<BookSummaryDto> newTopN = bookRepository.findNewTopN(pageRequest);
        List<BookSummaryDto> itTopN = bookRepository.findItTopN(pageRequest);
        List<BookSummaryDto> novelTopN = bookRepository.findNovelTopN(pageRequest);
        List<BookSummaryDto> selfTopN = bookRepository.findSelfTopN(pageRequest);

        return new MainPageBooksResponse(bestTopN, newTopN, itTopN, novelTopN, selfTopN);
    }

    public BookDetailResponse getBookDetail(String bookId, int page, int size) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reviewDate"));
        Page<Review> reviewPage = reviewRepository.findByBookId(bookId, pageRequest);

        List<ReviewDto> reviewList = reviewPage.getContent().stream()
                .map(ReviewDto::new)
                .collect(Collectors.toList());

        return new BookDetailResponse(book, reviewList, reviewPage.getTotalElements());
    }
}
