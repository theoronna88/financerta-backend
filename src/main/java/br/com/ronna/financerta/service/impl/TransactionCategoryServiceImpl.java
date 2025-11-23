package br.com.ronna.financerta.service.impl;

import br.com.ronna.financerta.dto.TransactionCategoryDto;
import br.com.ronna.financerta.model.TransactionCategory;
import br.com.ronna.financerta.repository.TransactionCategoryRepository;
import br.com.ronna.financerta.repository.UserRepository;
import br.com.ronna.financerta.service.TransactionCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionCategoryServiceImpl implements TransactionCategoryService {

    private final TransactionCategoryRepository repo;
    private final UserRepository userRepo;

    @Override
    public List<TransactionCategoryDto> findAllByUserId(UUID userId) {
        return repo.findByUserId(userId)
                .stream()
                .map(this::convertToDto)
                .toList();
    }

    @Override
    public TransactionCategoryDto findById(UUID id, UUID userId) {
        return repo.findByIdAndUserId(id, userId)
                .map(this::convertToDto)
                .orElse(null);
    }

    @Override
    public TransactionCategoryDto save(TransactionCategoryDto transactionCategoryDto, UUID userId) {
        var transactionCategory = new TransactionCategory();
        var user = userRepo.findById(userId).orElseThrow();
        BeanUtils.copyProperties(transactionCategoryDto, transactionCategory);
        transactionCategory.setUser(user);
        transactionCategory.setCreatedAt(LocalDateTime.now());
        transactionCategory.setUpdatedAt(LocalDateTime.now());
        return convertToDto(repo.save(transactionCategory));
    }

    @Override
    public TransactionCategoryDto update(UUID id, TransactionCategoryDto transactionCategoryDto, UUID userId) {
        var existingTransactionCategory = repo.findByIdAndUserId(id, userId).orElseThrow();
        var user = userRepo.findById(userId).orElseThrow();
        var updatedTransactionCategory = new TransactionCategory();
        updatedTransactionCategory.setId(existingTransactionCategory.getId());
        updatedTransactionCategory.setUser(user);
        updatedTransactionCategory.setName(transactionCategoryDto.getName());
        updatedTransactionCategory.setUpdatedAt(LocalDateTime.now());
        return convertToDto(repo.save(updatedTransactionCategory));
    }

    @Override
    public void delete(UUID id, UUID userId) {
        var transactionCategory = repo.findByIdAndUserId(id, userId);
        // TODO: Verificar se a categoria está associada a alguma transação antes de deletar
        transactionCategory.ifPresent(repo::delete);
    }

    private TransactionCategoryDto convertToDto(TransactionCategory transactionCategory) {
        var dto = new TransactionCategoryDto();
        dto.setId(transactionCategory.getId());
        dto.setName(transactionCategory.getName());
        return dto;
    }
}
