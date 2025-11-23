package br.com.ronna.financerta.controller;

import br.com.ronna.financerta.dto.TransactionDto;
import br.com.ronna.financerta.service.TransactionService;
import br.com.ronna.financerta.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction", description = "Endpoints das transações")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<TransactionDto>> findAll(Principal principal, @RequestParam(required = false)LocalDate startDate,
                                                        @RequestParam(required = false)LocalDate endDate) {
        var userId = SecurityUtils.getUserId(principal);
        return ResponseEntity.ok(transactionService.getTransactions(userId, startDate, endDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto> findById(@PathVariable UUID id, Principal principal) {
        var userId = SecurityUtils.getUserId(principal);
        return ResponseEntity.ok(transactionService.getTransactionById(id, userId));
    }

    @PostMapping
    public ResponseEntity<TransactionDto> createTransaction(@RequestBody TransactionDto transactionDto, Principal principal) {
        var userId = SecurityUtils.getUserId(principal);
        var createdTransaction = transactionService.createTransaction(transactionDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTransaction);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionDto> updateTransaction(@PathVariable UUID id, @RequestBody TransactionDto transactionDto, Principal principal) {
        var userId = SecurityUtils.getUserId(principal);
        var updatedTransaction = transactionService.updateTransaction(id, transactionDto, userId);
        return ResponseEntity.ok(updatedTransaction);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable UUID id, Principal principal) {
        var userId = SecurityUtils.getUserId(principal);
        transactionService.deleteTransaction(id, userId);
        return ResponseEntity.noContent().build();
    }
}
