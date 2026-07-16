package com.ledgerservice.persistence.repository;

import com.ledgerservice.persistence.entity.MoneyTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MoneyTransferRepository extends JpaRepository<MoneyTransfer, Long> {

    Optional<MoneyTransfer> findByIdempotencyKey(String idempotencyKey);
}
