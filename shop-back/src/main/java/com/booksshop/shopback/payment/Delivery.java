package com.booksshop.shopback.payment;

import com.booksshop.shopback.common.BaseEntity;
import com.booksshop.shopback.order.Order;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries")
@AttributeOverrides({
        @AttributeOverride(name = "createAt", column = @Column(name = "created_at", updatable = false)),
        @AttributeOverride(name = "updateAt", column = @Column(name = "updated_at"))
})
public class Delivery extends BaseEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "receiver_name", length = 50, nullable = false)
    private String receiverName;

    @Column(name = "receiver_phone", length = 20, nullable = false)
    private String receiverPhone;

    @Column(name = "post_code", length = 10, nullable = false)
    private String postCode;

    @Column(name = "address", length = 300, nullable = false)
    private String address;

    @Column(name = "addr_detail", length = 300)
    private String addrDetail;

    @Column(name = "delivery_memo", length = 200)
    private String deliveryMemo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private DeliveryStatus status;

    @Column(name = "courier_company", length = 50)
    private String courierCompany;

    @Column(name = "tracking_number", length = 50)
    private String trackingNumber;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    protected Delivery() {
    }

    public Delivery(String id, Order order, String receiverName, String receiverPhone, String postCode,
                     String address, String addrDetail, String deliveryMemo) {
        this.id = id;
        this.order = order;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.postCode = postCode;
        this.address = address;
        this.addrDetail = addrDetail;
        this.deliveryMemo = deliveryMemo;
        this.status = DeliveryStatus.PREPARING;
    }

    public String getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public String getPostCode() {
        return postCode;
    }

    public String getAddress() {
        return address;
    }

    public String getAddrDetail() {
        return addrDetail;
    }

    public String getDeliveryMemo() {
        return deliveryMemo;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public String getCourierCompany() {
        return courierCompany;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void markCanceled() {
        this.status = DeliveryStatus.CANCELED;
    }
}
