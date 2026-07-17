package com.ledgerservice.service;

import com.ledgerservice.persistence.entity.MoneyTransferStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResult(
        Long transferId,
        Long fromAccount,
        Long toAccount,
        BigDecimal amount,
        String idempotencyKey,
        MoneyTransferStatus status,
        Instant createdAt
) {
}
