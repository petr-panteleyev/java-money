/*
 Copyright © 2021-2022 Petr Panteleyev <petr@panteleyev.org>
 SPDX-License-Identifier: BSD-2-Clause
 */
package org.panteleyev.money.backend.controller;

import org.panteleyev.money.backend.repository.TransactionRepository;
import org.panteleyev.money.model.Transaction;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

import static org.panteleyev.money.backend.WebmoneyApplication.TRANSACTION_ROOT;

@Controller
@CrossOrigin
@RequestMapping(TRANSACTION_ROOT)
public class TransactionController {
    private final TransactionRepository transactionRepository;

    public TransactionController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Transaction>> getTransactions() {
        return ResponseEntity.ok(transactionRepository.getAll());
    }

    @GetMapping(
            value = "/{uuid}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Transaction> getTransaction(@PathVariable("uuid") UUID uuid) {
        return transactionRepository.get(uuid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(
            value = "/{uuid}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Transaction> putTransaction(@PathVariable UUID uuid, @RequestBody Transaction transaction) {
        if (!uuid.equals(transaction.uuid())) {
            return ResponseEntity.badRequest().build();
        }

        var rows = 0;
        if (transactionRepository.get(uuid).isEmpty()) {
            rows = transactionRepository.insert(transaction);
        } else {
            rows = transactionRepository.update(transaction);
        }
        return rows == 1 ? ResponseEntity.ok(transaction) : ResponseEntity.internalServerError().build();
    }
}
