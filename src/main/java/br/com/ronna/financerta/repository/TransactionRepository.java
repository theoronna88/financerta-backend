package br.com.ronna.financerta.repository;

import br.com.ronna.financerta.dto.TransactionDto;
import br.com.ronna.financerta.model.Transaction;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends CrudRepository<Transaction, UUID> {
    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);
    List<Transaction> findByUserId(UUID userId);

    List<Transaction> findByPurchaseGroupId(UUID purchaseGroupId);
}
