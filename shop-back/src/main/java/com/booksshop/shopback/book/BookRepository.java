package com.booksshop.shopback.book;

import com.booksshop.shopback.book.dto.BookListItemDto;
import com.booksshop.shopback.book.dto.BookSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, String> {

    // Review는 Book과 JPA 연관관계가 없고 bookId 문자열 컬럼으로만 연결되므로, JPQL의 명시적 ON 절로 ad-hoc left join한다.
    // 리뷰 0건 도서도 포함해야 하므로 LEFT JOIN + GROUP BY로 도서당 리뷰 수/평균 평점을 함께 집계한다.
    String CATEGORY_BOOK_LIST_SELECT =
            "SELECT new com.booksshop.shopback.book.dto.BookListItemDto(" +
            "b.id, b.title, b.subtitle, b.author, b.publisher, b.publishedDate, " +
            "COUNT(r.id), AVG(r.rating), b.listPrice, b.salePrice, b.coverImage, b.bestYn, b.newYn) " +
            "FROM Book b LEFT JOIN Review r ON r.bookId = b.id " +
            "WHERE b.category.types = :types " +
            "GROUP BY b.id, b.title, b.subtitle, b.author, b.publisher, b.publishedDate, " +
            "b.listPrice, b.salePrice, b.coverImage, b.bestYn, b.newYn ";

    String CATEGORY_BOOK_COUNT = "SELECT COUNT(b) FROM Book b WHERE b.category.types = :types";

    @Query(value = CATEGORY_BOOK_LIST_SELECT + "ORDER BY b.publishedDate DESC, b.title ASC",
            countQuery = CATEGORY_BOOK_COUNT)
    Page<BookListItemDto> findByCategoryOrderByNew(@Param("types") String types, Pageable pageable);

    @Query(value = CATEGORY_BOOK_LIST_SELECT + "ORDER BY b.salePrice ASC, b.title ASC",
            countQuery = CATEGORY_BOOK_COUNT)
    Page<BookListItemDto> findByCategoryOrderByLowerPrice(@Param("types") String types, Pageable pageable);

    @Query(value = CATEGORY_BOOK_LIST_SELECT + "ORDER BY b.salePrice DESC, b.title ASC",
            countQuery = CATEGORY_BOOK_COUNT)
    Page<BookListItemDto> findByCategoryOrderByHighPrice(@Param("types") String types, Pageable pageable);

    @Query(value = CATEGORY_BOOK_LIST_SELECT + "ORDER BY COUNT(r.id) DESC, b.title ASC",
            countQuery = CATEGORY_BOOK_COUNT)
    Page<BookListItemDto> findByCategoryOrderByReviewCnt(@Param("types") String types, Pageable pageable);

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
