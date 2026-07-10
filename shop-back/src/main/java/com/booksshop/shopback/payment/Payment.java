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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "payments")
@AttributeOverrides({
        @AttributeOverride(name = "createAt", column = @Column(name = "created_at", updatable = false)),
        @AttributeOverride(name = "updateAt", column = @Column(name = "updated_at"))
})
public class Payment extends BaseEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "payment_key", length = 200, nullable = false, unique = true)
    private String paymentKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private PaymentStatus status;

    @Column(name = "method", length = 30)
    private String method;

    @Column(name = "easy_pay_provider", length = 50)
    private String easyPayProvider;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(name = "balance_amount", nullable = false)
    private Integer balanceAmount;

    @Column(name = "supplied_amount")
    private Integer suppliedAmount;

    @Column(name = "vat")
    private Integer vat;

    @Column(name = "tax_free_amount")
    private Integer taxFreeAmount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "requested_at")
    private OffsetDateTime requestedAt;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    @Column(name = "last_transaction_key", length = 64)
    private String lastTransactionKey;

    @Column(name = "fail_code", length = 50)
    private String failCode;

    @Column(name = "fail_message", length = 500)
    private String failMessage;

    // 토스 Payment 객체 원본(JSON 문자열)을 그대로 저장. Hibernate 6의 JSON JdbcType은
    // 매핑 타입이 String이면 재직렬화 없이 원문 그대로 jsonb에 바인딩한다.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response", columnDefinition = "jsonb")
    private String rawResponse;

    protected Payment() {
    }

    private Payment(String id, Order order, String paymentKey, PaymentStatus status, String method,
                     String easyPayProvider, Integer totalAmount, Integer balanceAmount, Integer suppliedAmount,
                     Integer vat, Integer taxFreeAmount, String currency, OffsetDateTime requestedAt,
                     OffsetDateTime approvedAt, String receiptUrl, String lastTransactionKey,
                     String failCode, String failMessage, String rawResponse) {
        this.id = id;
        this.order = order;
        this.paymentKey = paymentKey;
        this.status = status;
        this.method = method;
        this.easyPayProvider = easyPayProvider;
        this.totalAmount = totalAmount;
        this.balanceAmount = balanceAmount;
        this.suppliedAmount = suppliedAmount;
        this.vat = vat;
        this.taxFreeAmount = taxFreeAmount;
        this.currency = currency;
        this.requestedAt = requestedAt;
        this.approvedAt = approvedAt;
        this.receiptUrl = receiptUrl;
        this.lastTransactionKey = lastTransactionKey;
        this.failCode = failCode;
        this.failMessage = failMessage;
        this.rawResponse = rawResponse;
    }

    public static Payment done(String id, Order order, String paymentKey, String method, String easyPayProvider,
                                Integer totalAmount, Integer balanceAmount, Integer suppliedAmount, Integer vat,
                                Integer taxFreeAmount, String currency, OffsetDateTime requestedAt,
                                OffsetDateTime approvedAt, String receiptUrl, String lastTransactionKey,
                                String rawResponse) {
        return new Payment(id, order, paymentKey, PaymentStatus.DONE, method, easyPayProvider, totalAmount,
                balanceAmount, suppliedAmount, vat, taxFreeAmount, currency, requestedAt, approvedAt,
                receiptUrl, lastTransactionKey, null, null, rawResponse);
    }

    public static Payment aborted(String id, Order order, String paymentKey, Integer totalAmount, String currency,
                                   String failCode, String failMessage, String rawResponse) {
        return new Payment(id, order, paymentKey, PaymentStatus.ABORTED, null, null, totalAmount,
                0, null, null, null, currency, null, null, null, null, failCode, failMessage, rawResponse);
    }

    public void markCanceled() {
        this.status = PaymentStatus.CANCELED;
    }

    public String getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public String getPaymentKey() {
        return paymentKey;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getMethod() {
        return method;
    }

    public String getEasyPayProvider() {
        return easyPayProvider;
    }

    public Integer getTotalAmount() {
        return totalAmount;
    }

    public Integer getBalanceAmount() {
        return balanceAmount;
    }

    public Integer getSuppliedAmount() {
        return suppliedAmount;
    }

    public Integer getVat() {
        return vat;
    }

    public Integer getTaxFreeAmount() {
        return taxFreeAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public String getReceiptUrl() {
        return receiptUrl;
    }

    public String getLastTransactionKey() {
        return lastTransactionKey;
    }

    public String getFailCode() {
        return failCode;
    }

    public String getFailMessage() {
        return failMessage;
    }

    public String getRawResponse() {
        return rawResponse;
    }
}
