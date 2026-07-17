package com.ledgerservice.controller;

import com.ledgerservice.service.TransferResult;
import com.ledgerservice.service.TransferService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/transfer")
    public TransferResult transfer(@RequestParam Long fromAccount,
                                   @RequestParam Long toAccount,
                                   @RequestParam BigDecimal amount,
                                   @RequestParam String idempotencyKey) {
        return transferService.transfer(fromAccount, toAccount, amount, idempotencyKey);
    }
}
