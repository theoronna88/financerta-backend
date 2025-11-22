package br.com.ronna.financerta.service;

import br.com.ronna.financerta.dto.TransactionCategoryDto;

import java.util.List;

public interface TransactionCategoryService {

    List<TransactionCategoryDto> findAllByUserId(java.util.UUID userId);
    TransactionCategoryDto findById(java.util.UUID id, java.util.UUID userId);
    TransactionCategoryDto save(TransactionCategoryDto transactionCategoryDto, java.util.UUID userId);
    TransactionCategoryDto update(java.util.UUID id, TransactionCategoryDto transactionCategoryDto, java.util.UUID userId);
    void delete(java.util.UUID id, java.util.UUID userId);
}
