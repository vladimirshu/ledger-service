package com.ledgerservice.persistence;

import com.ledgerservice.persistence.entity.Account;
import com.ledgerservice.persistence.entity.MoneyTransfer;
import com.ledgerservice.persistence.entity.MoneyTransferStatus;
import com.ledgerservice.persistence.repository.AccountRepository;
import com.ledgerservice.persistence.repository.MoneyTransferRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ledger-test;DB_CLOSE_DELAY=-1",
        "spring.h2.console.enabled=false"
})
@Transactional
class PersistenceIntegrationTests {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MoneyTransferRepository transferRepository;

    @Test
    void persistsAccounts() {
        Account first = accountRepository.save(new Account(new BigDecimal("100.00")));
        Account second = accountRepository.save(new Account(new BigDecimal("50.00")));
        accountRepository.flush();

        assertThat(accountRepository.findAllById(java.util.List.of(first.getId(), second.getId())))
                .extracting(Account::getId)
                .containsExactlyInAnyOrder(first.getId(), second.getId());
        assertThat(first.getModifiedAt()).isNotNull();
        assertThat(first.getVersion()).isZero();
    }

    @Test
    void findsAuditedTransferByIdempotencyKey() {
        Account from = accountRepository.save(new Account(new BigDecimal("100.00")));
        Account to = accountRepository.save(new Account(new BigDecimal("25.00")));
        MoneyTransfer transfer = transferRepository.saveAndFlush(new MoneyTransfer(
                from, to, new BigDecimal("10.00"), "request-123", MoneyTransferStatus.PENDING));

        MoneyTransfer persisted = transferRepository.findByIdempotencyKey("request-123").orElseThrow();

        assertThat(persisted.getId()).isEqualTo(transfer.getId());
        assertThat(persisted.getStatus()).isEqualTo(MoneyTransferStatus.PENDING);
        assertThat(persisted.getCreatedAt()).isNotNull();
    }
}
