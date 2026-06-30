package com.booksshop.shopback.book;

import com.booksshop.shopback.book.dto.BookSummaryDto;
import com.booksshop.shopback.book.dto.MainPageBooksResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    private static final int TOP_N = 5;

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
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
}
