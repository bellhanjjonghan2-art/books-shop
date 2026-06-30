package com.booksshop.shopback.book;

import com.booksshop.shopback.common.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;

import java.time.LocalDate;

@Entity
@Table(name = "books")
@AttributeOverrides({
        @AttributeOverride(name = "createAt", column = @Column(name = "created_at", updatable = false)),
        @AttributeOverride(name = "updateAt", column = @Column(name = "updated_at"))
})
public class Book extends BaseEntity {

    @Id
    @Column(name = "id", length = 40)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "subtitle", length = 200)
    private String subtitle;

    @Column(name = "author", length = 100)
    private String author;

    @Column(name = "publisher", length = 100)
    private String publisher;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image", length = 500)
    private String coverImage;

    @Column(name = "list_price")
    private Integer listPrice;

    @Column(name = "sale_price")
    private Integer salePrice;

    @Column(name = "stocks", nullable = false)
    private Integer stocks;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "published_date")
    private LocalDate publishedDate;

    @Column(name = "edition", length = 50)
    private String edition;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "best_yn", columnDefinition = "CHAR(1)", nullable = false)
    private String bestYn;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "new_yn", columnDefinition = "CHAR(1)", nullable = false)
    private String newYn;

    protected Book() {
    }

    public String getId() {
        return id;
    }

    public Category getCategory() {
        return category;
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

    public String getDescription() {
        return description;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public Integer getListPrice() {
        return listPrice;
    }

    public Integer getSalePrice() {
        return salePrice;
    }

    public Integer getStocks() {
        return stocks;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public LocalDate getPublishedDate() {
        return publishedDate;
    }

    public String getEdition() {
        return edition;
    }

    public String getBestYn() {
        return bestYn;
    }

    public String getNewYn() {
        return newYn;
    }
}
