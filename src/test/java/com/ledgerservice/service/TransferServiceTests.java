package com.ledgerservice.service;

import com.ledgerservice.persistence.entity.Account;
import com.ledgerservice.persistence.entity.MoneyTransfer;
import com.ledgerservice.persistence.entity.MoneyTransferStatus;
import com.ledgerservice.persistence.repository.AccountRepository;
import com.ledgerservice.persistence.repository.MoneyTransferRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTests {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private MoneyTransferRepository transferRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private Account lowerIdAccount;
    @Mock
    private Account higherIdAccount;

    @Test
    void transfersMoneyAndFlushesAfterChangingTheLowerIdAccount() {
        TransferService service = service();
        when(transferRepository.findByIdempotencyKey("request-1")).thenReturn(Optional.empty());
        when(accountRepository.findAllByIdInOrderByIdAsc(List.of(2L, 1L)))
                .thenReturn(List.of(lowerIdAccount, higherIdAccount));
        when(lowerIdAccount.getId()).thenReturn(1L);
        when(lowerIdAccount.getBalance()).thenReturn(new BigDecimal("25.00"));
        when(higherIdAccount.getId()).thenReturn(2L);
        when(higherIdAccount.getBalance()).thenReturn(new BigDecimal("100.00"));
        when(transferRepository.save(any(MoneyTransfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransferResult result = service.transfer(2L, 1L, new BigDecimal("10.00"), "request-1");

        assertThat(result.status()).isEqualTo(MoneyTransferStatus.FINISHED);
        assertThat(result.fromAccount()).isEqualTo(2L);
        assertThat(result.toAccount()).isEqualTo(1L);
        InOrder order = inOrder(lowerIdAccount, entityManager, higherIdAccount);
        order.verify(lowerIdAccount).setBalance(new BigDecimal("35.00"));
        order.verify(entityManager).flush();
        order.verify(higherIdAccount).setBalance(new BigDecimal("90.00"));
    }

    @Test
    void returnsAnExistingTransferForTheSameIdempotentRequest() {
        TransferService service = service();
        when(lowerIdAccount.getId()).thenReturn(1L);
        when(higherIdAccount.getId()).thenReturn(2L);
        MoneyTransfer existing = new MoneyTransfer(lowerIdAccount, higherIdAccount,
                new BigDecimal("10.00"), "request-1", MoneyTransferStatus.FINISHED);
        when(transferRepository.findByIdempotencyKey("request-1")).thenReturn(Optional.of(existing));

        TransferResult result = service.transfer(1L, 2L, new BigDecimal("10.0"), "request-1");

        assertThat(result.status()).isEqualTo(MoneyTransferStatus.FINISHED);
        verify(accountRepository, never()).findAllByIdInOrderByIdAsc(any());
        verify(transferRepository, never()).save(any());
    }

    @Test
    void rejectsReuseOfAnIdempotencyKeyForDifferentParameters() {
        TransferService service = service();
        when(lowerIdAccount.getId()).thenReturn(1L);
        when(higherIdAccount.getId()).thenReturn(2L);
        MoneyTransfer existing = new MoneyTransfer(lowerIdAccount, higherIdAccount,
                new BigDecimal("10.00"), "request-1", MoneyTransferStatus.FINISHED);
        when(transferRepository.findByIdempotencyKey("request-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.transfer(1L, 2L, new BigDecimal("11.00"), "request-1"))
                .isInstanceOf(TransferException.class)
                .extracting(exception -> ((TransferException) exception).getCode())
                .isEqualTo("IDEMPOTENCY_CONFLICT");
    }

    @Test
    void rejectsTransferWhenFundsAreInsufficient() {
        TransferService service = service();
        when(transferRepository.findByIdempotencyKey("request-1")).thenReturn(Optional.empty());
        when(accountRepository.findAllByIdInOrderByIdAsc(List.of(1L, 2L)))
                .thenReturn(List.of(lowerIdAccount, higherIdAccount));
        when(lowerIdAccount.getId()).thenReturn(1L);
        when(lowerIdAccount.getBalance()).thenReturn(new BigDecimal("5.00"));

        assertThatThrownBy(() -> service.transfer(1L, 2L, new BigDecimal("10.00"), "request-1"))
                .isInstanceOf(TransferException.class)
                .extracting(exception -> ((TransferException) exception).getCode())
                .isEqualTo("INSUFFICIENT_FUNDS");

        verify(transferRepository, never()).save(any());
        verify(entityManager, never()).flush();
    }

    @Test
    void rejectsInvalidRequestBeforeAccessingPersistence() {
        TransferService service = service();

        assertThatThrownBy(() -> service.transfer(1L, 1L, BigDecimal.ONE, "request-1"))
                .isInstanceOf(TransferException.class)
                .extracting(exception -> ((TransferException) exception).getCode())
                .isEqualTo("INVALID_REQUEST");

        verify(transferRepository, never()).findByIdempotencyKey(any());
    }

    private TransferService service() {
        return new TransferService(accountRepository, transferRepository, entityManager);
    }
}
