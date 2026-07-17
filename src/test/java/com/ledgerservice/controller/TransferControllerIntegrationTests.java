package com.ledgerservice.controller;

import com.ledgerservice.persistence.entity.Account;
import com.ledgerservice.persistence.repository.AccountRepository;
import com.ledgerservice.persistence.repository.MoneyTransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:transfer-controller-test;DB_CLOSE_DELAY=-1",
        "spring.h2.console.enabled=false"
})
class TransferControllerIntegrationTests {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private MoneyTransferRepository transferRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        transferRepository.deleteAll();
        accountRepository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void transfersMoneyAndReturnsTheAuditRecord() throws Exception {
        Account from = accountRepository.save(new Account(new BigDecimal("100.00")));
        Account to = accountRepository.save(new Account(new BigDecimal("25.00")));

        mockMvc.perform(post("/api/v1/transfer")
                        .param("fromAccount", from.getId().toString())
                        .param("toAccount", to.getId().toString())
                        .param("amount", "10.00")
                        .param("idempotencyKey", "transfer-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.idempotencyKey").value("transfer-1"));

        assertThat(accountRepository.findById(from.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("90.00");
        assertThat(accountRepository.findById(to.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("35.00");
    }

    @Test
    void repeatedRequestDoesNotTransferMoneyTwice() throws Exception {
        Account from = accountRepository.save(new Account(new BigDecimal("100.00")));
        Account to = accountRepository.save(new Account(new BigDecimal("25.00")));

        for (int invocation = 0; invocation < 2; invocation++) {
            mockMvc.perform(post("/api/v1/transfer")
                            .param("fromAccount", from.getId().toString())
                            .param("toAccount", to.getId().toString())
                            .param("amount", "10.00")
                            .param("idempotencyKey", "same-key"))
                    .andExpect(status().isOk());
        }

        assertThat(accountRepository.findById(from.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("90.00");
        assertThat(transferRepository.count()).isOne();
    }

    @Test
    void rejectsInsufficientFunds() throws Exception {
        Account from = accountRepository.save(new Account(new BigDecimal("5.00")));
        Account to = accountRepository.save(new Account(new BigDecimal("25.00")));

        mockMvc.perform(post("/api/v1/transfer")
                        .param("fromAccount", from.getId().toString())
                        .param("toAccount", to.getId().toString())
                        .param("amount", "10.00")
                        .param("idempotencyKey", "too-much"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));
    }
}
