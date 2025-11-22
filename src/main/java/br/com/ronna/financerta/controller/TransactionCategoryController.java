package br.com.ronna.financerta.controller;

import br.com.ronna.financerta.dto.TransactionCategoryDto;
import br.com.ronna.financerta.service.TransactionCategoryService;
import br.com.ronna.financerta.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transaction-categories")
@RequiredArgsConstructor
@Tag(name = "Transaction Categories", description = "Endpoints de categorias de transações")
public class TransactionCategoryController {

    private final TransactionCategoryService transactionCategoryService;

    @GetMapping
    public ResponseEntity<List<TransactionCategoryDto>> getTransactionCategories(Principal principal) {
        var userId = SecurityUtils.getUserId(principal);
        return ResponseEntity.status(HttpStatus.OK).body(transactionCategoryService.findAllByUserId(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionCategoryDto> getTransactionCategoryById(@PathVariable UUID id, Principal principal) {
        var userId = SecurityUtils.getUserId(principal);
        return ResponseEntity.status(HttpStatus.OK).body(transactionCategoryService.findById(id, userId));
    }

    @PostMapping
    public ResponseEntity<TransactionCategoryDto> createTransactionCategory(Principal principal, @RequestBody TransactionCategoryDto transactionCategoryDto) {
        var userId = SecurityUtils.getUserId(principal);
        var createdTransactionCategory = transactionCategoryService.save(transactionCategoryDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTransactionCategory);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionCategoryDto> updateTransactionCategory(Principal principal, @PathVariable UUID id, @RequestBody TransactionCategoryDto transactionCategoryDto) {
        var userId = SecurityUtils.getUserId(principal);
        var updatedTransactionCategory = transactionCategoryService.update(id,transactionCategoryDto, userId);
        return ResponseEntity.status(HttpStatus.OK).body(updatedTransactionCategory);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransactionCategory(Principal principal, @PathVariable UUID id) {
        var userId = SecurityUtils.getUserId(principal);
        transactionCategoryService.delete(id, userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
