package com.booksshop.shopback.book;

import com.booksshop.shopback.book.dto.BookDetailResponse;
import com.booksshop.shopback.book.dto.BookListItemDto;
import com.booksshop.shopback.book.dto.BookListResponse;
import com.booksshop.shopback.book.dto.BookOrderItemDto;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookService {

    private static final int TOP_N = 5;

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final Set<String> VALID_CATEGORY_TYPES = Set.of("IT", "NOVEL", "SELF");
    private static final Set<String> VALID_ORDER_TYPES = Set.of("new", "lower", "high", "reviewCnt");

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

    public BookListResponse getBooksByCategory(String types, int page, int size, String orderType) {
        if (!VALID_CATEGORY_TYPES.contains(types)) {
            throw new BusinessException(ErrorCode.INVALID_CATEGORY);
        }
        if (!VALID_ORDER_TYPES.contains(orderType)) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_TYPE);
        }

        int safePage = Math.max(page, DEFAULT_PAGE);
        int safeSize = size < 1 ? DEFAULT_PAGE_SIZE : size;
        PageRequest pageRequest = PageRequest.of(safePage, safeSize);

        Page<BookListItemDto> result = switch (orderType) {
            case "lower" -> bookRepository.findByCategoryOrderByLowerPrice(types, pageRequest);
            case "high" -> bookRepository.findByCategoryOrderByHighPrice(types, pageRequest);
            case "reviewCnt" -> bookRepository.findByCategoryOrderByReviewCnt(types, pageRequest);
            default -> bookRepository.findByCategoryOrderByNew(types, pageRequest);
        };

        return new BookListResponse(
                result.getContent(),
                safePage,
                safeSize,
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public List<BookOrderItemDto> getBooksByIds(List<String> bookIds) {
        return bookRepository.findByIdIn(bookIds);
    }
}
