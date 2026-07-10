package com.booksshop.shopback.payment;

import com.booksshop.shopback.common.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "payment_cancels")
@AttributeOverrides({
        @AttributeOverride(name = "createAt", column = @Column(name = "created_at", updatable = false)),
        @AttributeOverride(name = "updateAt", column = @Column(name = "updated_at"))
})
public class PaymentCancel extends BaseEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "payment_id", length = 36, nullable = false)
    private String paymentId;

    @Column(name = "transaction_key", length = 64, nullable = false, unique = true)
    private String transactionKey;

    @Column(name = "cancel_amount", nullable = false)
    private Integer cancelAmount;

    @Column(name = "cancel_reason", length = 200, nullable = false)
    private String cancelReason;

    @Column(name = "tax_free_amount")
    private Integer taxFreeAmount;

    @Column(name = "cancel_status", length = 20, nullable = false)
    private String cancelStatus;

    @Column(name = "canceled_at", nullable = false)
    private OffsetDateTime canceledAt;

    protected PaymentCancel() {
    }

    public PaymentCancel(String id, String paymentId, String transactionKey, Integer cancelAmount,
                          String cancelReason, Integer taxFreeAmount, String cancelStatus,
                          OffsetDateTime canceledAt) {
        this.id = id;
        this.paymentId = paymentId;
        this.transactionKey = transactionKey;
        this.cancelAmount = cancelAmount;
        this.cancelReason = cancelReason;
        this.taxFreeAmount = taxFreeAmount;
        this.cancelStatus = cancelStatus;
        this.canceledAt = canceledAt;
    }

    public String getId() {
        return id;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getTransactionKey() {
        return transactionKey;
    }

    public Integer getCancelAmount() {
        return cancelAmount;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public Integer getTaxFreeAmount() {
        return taxFreeAmount;
    }

    public String getCancelStatus() {
        return cancelStatus;
    }

    public OffsetDateTime getCanceledAt() {
        return canceledAt;
    }
}
