package com.ledgerservice.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "modified_at", nullable = false)
    private Instant modifiedAt;

    protected Account() {
    }

    public Account(BigDecimal balance) {
        this.balance = Objects.requireNonNull(balance, "balance must not be null");
    }

    @PrePersist
    @PreUpdate
    void updateModifiedAt() {
        modifiedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public long getVersion() {
        return version;
    }

    public Instant getModifiedAt() {
        return modifiedAt;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = Objects.requireNonNull(balance, "balance must not be null");
    }
}
