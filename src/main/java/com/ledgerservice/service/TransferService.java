package com.ledgerservice.service;

import com.ledgerservice.persistence.entity.Account;
import com.ledgerservice.persistence.entity.MoneyTransfer;
import com.ledgerservice.persistence.entity.MoneyTransferStatus;
import com.ledgerservice.persistence.repository.AccountRepository;
import com.ledgerservice.persistence.repository.MoneyTransferRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransferService {

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
    public TransferResult transfer(Long fromAccountId, Long toAccountId, BigDecimal amount,
                                   String idempotencyKey) {
        validateRequest(fromAccountId, toAccountId, amount, idempotencyKey);

        MoneyTransfer previous = transferRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (previous != null) {
            verifySameRequest(previous, fromAccountId, toAccountId, amount);
            return toResult(previous);
        }

        List<Account> accounts = accountRepository.findAllByIdInOrderByIdAsc(
                List.of(fromAccountId, toAccountId));
        if (accounts.size() != 2) {
            throw new TransferException("ACCOUNT_NOT_FOUND", "One or both accounts do not exist");
        }

        Account first = accounts.get(0);
        Account second = accounts.get(1);
        Account from = first.getId().equals(fromAccountId) ? first : second;
        Account to = first.getId().equals(toAccountId) ? first : second;
        if (from.getBalance().compareTo(amount) < 0) {
            throw new TransferException("INSUFFICIENT_FUNDS", "Source account has insufficient funds");
        }

        MoneyTransfer transfer = transferRepository.save(new MoneyTransfer(
                from, to, amount, idempotencyKey, MoneyTransferStatus.PENDING));

        applyBalanceChange(first, fromAccountId, amount);
        entityManager.flush();
        applyBalanceChange(second, fromAccountId, amount);

        transfer.setStatus(MoneyTransferStatus.FINISHED);
        return toResult(transfer);
    }

    private static void validateRequest(Long fromAccountId, Long toAccountId, BigDecimal amount,
                                        String idempotencyKey) {
        if (fromAccountId == null || fromAccountId <= 0 || toAccountId == null || toAccountId <= 0) {
            throw new TransferException("INVALID_REQUEST", "Account IDs must be positive");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new TransferException("INVALID_REQUEST", "Source and destination accounts must be different");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || amount.scale() > 2
                || amount.precision() - amount.scale() > 17) {
            throw new TransferException("INVALID_REQUEST", "Amount must be positive and fit NUMERIC(19,2)");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new TransferException("INVALID_REQUEST", "Idempotency key must contain 1 to 100 characters");
        }
    }

    private static void verifySameRequest(MoneyTransfer transfer, Long fromAccountId, Long toAccountId,
                                          BigDecimal amount) {
        boolean sameRequest = transfer.getFromAccount().getId().equals(fromAccountId)
                && transfer.getToAccount().getId().equals(toAccountId)
                && transfer.getAmount().compareTo(amount) == 0;
        if (!sameRequest) {
            throw new TransferException("IDEMPOTENCY_CONFLICT",
                    "Idempotency key was already used for a different transfer");
        }
    }

    private static void applyBalanceChange(Account account, Long fromAccountId, BigDecimal amount) {
        account.setBalance(account.getId().equals(fromAccountId)
                ? account.getBalance().subtract(amount)
                : account.getBalance().add(amount));
    }

    private static TransferResult toResult(MoneyTransfer transfer) {
        return new TransferResult(transfer.getId(), transfer.getFromAccount().getId(),
                transfer.getToAccount().getId(), transfer.getAmount(), transfer.getIdempotencyKey(),
                transfer.getStatus(), transfer.getCreatedAt());
    }
}
