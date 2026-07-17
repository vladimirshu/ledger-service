package com.ledgerservice.service;

import com.ledgerservice.persistence.entity.Account;
import com.ledgerservice.persistence.entity.MoneyTransfer;
import com.ledgerservice.persistence.repository.AccountRepository;
import com.ledgerservice.persistence.repository.MoneyTransferRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final AccountRepository accountRepository;
    private final MoneyTransferRepository transferRepository;
    private final EntityManager entityManager;

    public TransferService(AccountRepository accountRepository, MoneyTransferRepository transferRepository,
                           EntityManager entityManager) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    @Retryable(
            retryFor = {DataIntegrityViolationException.class, OptimisticLockingFailureException.class,
                    OptimisticLockException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 25, multiplier = 2, maxDelay = 100)
    )
    public TransferResult transfer(Long fromAccountId, Long toAccountId, BigDecimal amount,
                                   String idempotencyKey) {
        log.info("Transfer requested: fromAccount={}, toAccount={}, amount={}",
                fromAccountId, toAccountId, amount);
        validateRequest(fromAccountId, toAccountId, amount, idempotencyKey);

        MoneyTransfer previous = transferRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (previous != null) {
            verifySameRequest(previous, fromAccountId, toAccountId, amount);
            log.info("Returning completed idempotent transfer: transferId={}", previous.getId());
            return toResult(previous);
        }

        List<Account> accounts = accountRepository.findAllByIdInOrderByIdAsc(
                List.of(fromAccountId, toAccountId));
        if (accounts.size() != 2) {
            log.warn("Transfer rejected because an account was not found: fromAccount={}, toAccount={}",
                    fromAccountId, toAccountId);
            throw new TransferException("ACCOUNT_NOT_FOUND", "One or both accounts do not exist");
        }

        Account first = accounts.get(0);
        Account second = accounts.get(1);
        Account from = first.getId().equals(fromAccountId) ? first : second;
        Account to = first.getId().equals(toAccountId) ? first : second;
        if (from.getBalance().compareTo(amount) < 0) {
            log.warn("Transfer rejected because of insufficient funds: fromAccount={}, toAccount={}, amount={}",
                    fromAccountId, toAccountId, amount);
            throw new TransferException("INSUFFICIENT_FUNDS", "Source account has insufficient funds");
        }

        applyBalanceChange(first, fromAccountId, amount);
        entityManager.flush();
        applyBalanceChange(second, fromAccountId, amount);

        MoneyTransfer transfer = transferRepository.save(new MoneyTransfer(
                from, to, amount, idempotencyKey));
        logAfterCommit(transfer.getId(), fromAccountId, toAccountId, amount);
        return toResult(transfer);
    }

    private static void validateRequest(Long fromAccountId, Long toAccountId, BigDecimal amount,
                                        String idempotencyKey) {
        if (fromAccountId == null || fromAccountId <= 0 || toAccountId == null || toAccountId <= 0) {
            log.warn("Transfer rejected because account IDs are invalid: fromAccount={}, toAccount={}",
                    fromAccountId, toAccountId);
            throw new TransferException("INVALID_REQUEST", "Account IDs must be positive");
        }
        if (fromAccountId.equals(toAccountId)) {
            log.warn("Transfer rejected because source and destination are identical: account={}", fromAccountId);
            throw new TransferException("INVALID_REQUEST", "Source and destination accounts must be different");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || amount.scale() > 2
                || amount.precision() - amount.scale() > 17) {
            log.warn("Transfer rejected because amount is invalid: fromAccount={}, toAccount={}, amount={}",
                    fromAccountId, toAccountId, amount);
            throw new TransferException("INVALID_REQUEST", "Amount must be positive and fit NUMERIC(19,2)");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            log.warn("Transfer rejected because the idempotency key is invalid: fromAccount={}, toAccount={}",
                    fromAccountId, toAccountId);
            throw new TransferException("INVALID_REQUEST", "Idempotency key must contain 1 to 100 characters");
        }
    }

    private static void verifySameRequest(MoneyTransfer transfer, Long fromAccountId, Long toAccountId,
                                          BigDecimal amount) {
        boolean sameRequest = transfer.getFromAccount().getId().equals(fromAccountId)
                && transfer.getToAccount().getId().equals(toAccountId)
                && transfer.getAmount().compareTo(amount) == 0;
        if (!sameRequest) {
            log.warn("Transfer rejected because the idempotency key was reused with different parameters: "
                    + "existingTransferId={}", transfer.getId());
            throw new TransferException("IDEMPOTENCY_CONFLICT",
                    "Idempotency key was already used for a different transfer");
        }
    }

    private static void applyBalanceChange(Account account, Long fromAccountId, BigDecimal amount) {
        account.setBalance(account.getId().equals(fromAccountId)
                ? account.getBalance().subtract(amount)
                : account.getBalance().add(amount));
    }

    private static void logAfterCommit(Long transferId, Long fromAccountId, Long toAccountId,
                                       BigDecimal amount) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.info("Transfer completed: transferId={}, fromAccount={}, toAccount={}, amount={}",
                    transferId, fromAccountId, toAccountId, amount);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("Transfer completed: transferId={}, fromAccount={}, toAccount={}, amount={}",
                        transferId, fromAccountId, toAccountId, amount);
            }
        });
    }

    private static TransferResult toResult(MoneyTransfer transfer) {
        return new TransferResult(transfer.getId(), transfer.getFromAccount().getId(),
                transfer.getToAccount().getId(), transfer.getAmount(), transfer.getIdempotencyKey(),
                transfer.getStatus(), transfer.getCreatedAt());
    }
}
