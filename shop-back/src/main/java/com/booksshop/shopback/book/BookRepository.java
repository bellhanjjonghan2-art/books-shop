package com.booksshop.shopback.book;

import com.booksshop.shopback.book.dto.BookSummaryDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, String> {

    @Query("SELECT new com.booksshop.shopback.book.dto.BookSummaryDto(b.id, b.title, b.coverImage, b.author, b.salePrice) " +
           "FROM Book b " +
           "WHERE b.bestYn = 'Y' " +
           "ORDER BY b.publishedDate DESC, b.title ASC")
    List<BookSummaryDto> findBestTopN(Pageable pageable);

    @Query("SELECT new com.booksshop.shopback.book.dto.BookSummaryDto(b.id, b.title, b.coverImage, b.author, b.salePrice) " +
           "FROM Book b " +
           "WHERE b.newYn = 'Y' " +
           "ORDER BY b.publishedDate DESC, b.title ASC")
    List<BookSummaryDto> findNewTopN(Pageable pageable);

    @Query("SELECT new com.booksshop.shopback.book.dto.BookSummaryDto(b.id, b.title, b.coverImage, b.author, b.salePrice) " +
           "FROM Book b " +
           "WHERE b.category.types = 'IT' " +
           "ORDER BY b.publishedDate DESC, b.title ASC")
    List<BookSummaryDto> findItTopN(Pageable pageable);

    @Query("SELECT new com.booksshop.shopback.book.dto.BookSummaryDto(b.id, b.title, b.coverImage, b.author, b.salePrice) " +
           "FROM Book b " +
           "WHERE b.category.types = 'NOVEL' " +
           "ORDER BY b.publishedDate DESC, b.title ASC")
    List<BookSummaryDto> findNovelTopN(Pageable pageable);

    @Query("SELECT new com.booksshop.shopback.book.dto.BookSummaryDto(b.id, b.title, b.coverImage, b.author, b.salePrice) " +
           "FROM Book b " +
           "WHERE b.category.types = 'SELF' " +
           "ORDER BY b.publishedDate DESC, b.title ASC")
    List<BookSummaryDto> findSelfTopN(Pageable pageable);
}
