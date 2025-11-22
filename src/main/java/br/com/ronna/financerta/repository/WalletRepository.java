package br.com.ronna.financerta.repository;

import br.com.ronna.financerta.model.Wallet;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends CrudRepository<Wallet, UUID> {
    List<Wallet> findAllByUserId(UUID userId);
    Optional<Wallet> findByIdAndUserId(UUID id, UUID userId);
}
