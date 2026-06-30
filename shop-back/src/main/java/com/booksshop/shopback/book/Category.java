package com.booksshop.shopback.book;

import com.booksshop.shopback.common.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;

@Entity
@Table(name = "categories")
@AttributeOverrides({
        @AttributeOverride(name = "createAt", column = @Column(name = "created_at", updatable = false)),
        @AttributeOverride(name = "updateAt", column = @Column(name = "updated_at"))
})
public class Category extends BaseEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "types", length = 30)
    private String types;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "use_yn", columnDefinition = "CHAR(1)", nullable = false)
    private String useYn;

    protected Category() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public String getTypes() {
        return types;
    }

    public String getUseYn() {
        return useYn;
    }
}
