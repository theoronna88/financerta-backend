package br.com.ronna.financerta.repository;

import br.com.ronna.financerta.model.TransactionCategory;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionCategoryRepository extends CrudRepository<TransactionCategory, UUID> {
    List<TransactionCategory> findByUserId(java.util.UUID userId);
    Optional<TransactionCategory> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByIdAndUserId(UUID id, UUID userId);
}
