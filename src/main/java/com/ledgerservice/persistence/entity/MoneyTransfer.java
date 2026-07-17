package com.ledgerservice.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "money_transfer")
public class MoneyTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Account fromAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Account toAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 100, unique = true)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 16)
    private MoneyTransferStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MoneyTransfer() {
    }

    public MoneyTransfer(Account fromAccount, Account toAccount, BigDecimal amount,
                         String idempotencyKey) {
        this.fromAccount = Objects.requireNonNull(fromAccount, "fromAccount must not be null");
        this.toAccount = Objects.requireNonNull(toAccount, "toAccount must not be null");
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        this.status = MoneyTransferStatus.COMPLETED;
    }

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public Account getFromAccount() { return fromAccount; }
    public Account getToAccount() { return toAccount; }
    public BigDecimal getAmount() { return amount; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public MoneyTransferStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

}
