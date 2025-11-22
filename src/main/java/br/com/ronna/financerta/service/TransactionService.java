package br.com.ronna.financerta.service;

import br.com.ronna.financerta.dto.TransactionDto;

import java.util.List;
import java.util.UUID;

public interface TransactionService {

    TransactionDto createTransaction(TransactionDto transactionDto, UUID userId);
    List<TransactionDto> getTransactions(UUID userId);
    TransactionDto getTransactionById(UUID id, UUID userId);
    TransactionDto updateTransaction(UUID id, TransactionDto transactionDto, UUID userId);
    void deleteTransaction(UUID id, UUID userId);

}
