package com.ledgerservice.controller;

import com.ledgerservice.persistence.entity.MoneyTransferStatus;
import com.ledgerservice.service.TransferResult;
import com.ledgerservice.service.TransferService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferControllerTests {

    @Mock
    private TransferService transferService;

    @InjectMocks
    private TransferController controller;

    @Test
    void delegatesTransferRequestToTheService() {
        BigDecimal amount = new BigDecimal("10.00");
        TransferResult expected = new TransferResult(42L, 1L, 2L, amount, "request-1",
                MoneyTransferStatus.FINISHED, Instant.parse("2026-07-17T10:00:00Z"));
        when(transferService.transfer(1L, 2L, amount, "request-1")).thenReturn(expected);

        TransferResult actual = controller.transfer(1L, 2L, amount, "request-1");

        assertThat(actual).isSameAs(expected);
        verify(transferService).transfer(1L, 2L, amount, "request-1");
    }
}
