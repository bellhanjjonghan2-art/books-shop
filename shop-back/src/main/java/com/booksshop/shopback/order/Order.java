package com.booksshop.shopback.order;

import com.booksshop.shopback.common.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
@AttributeOverrides({
        @AttributeOverride(name = "createAt", column = @Column(name = "created_at", updatable = false)),
        @AttributeOverride(name = "updateAt", column = @Column(name = "updated_at"))
})
public class Order extends BaseEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", length = 100, nullable = false)
    private String userId;

    @Column(name = "order_name", length = 100, nullable = false)
    private String orderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    protected Order() {
    }

    public Order(String id, String userId, String orderName, Integer totalAmount) {
        this.id = id;
        this.userId = userId;
        this.orderName = orderName;
        this.status = OrderStatus.PENDING;
        this.totalAmount = totalAmount;
    }

    public void markPaid() {
        this.status = OrderStatus.PAID;
    }

    public void markFailed() {
        this.status = OrderStatus.FAILED;
    }

    public void markCanceled() {
        this.status = OrderStatus.CANCELED;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getOrderName() {
        return orderName;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Integer getTotalAmount() {
        return totalAmount;
    }
}
