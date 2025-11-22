package br.com.ronna.financerta.repository;

import br.com.ronna.financerta.model.CreditCard;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditCardRepository extends CrudRepository<CreditCard, UUID> {
    List<CreditCard> findAllByUserId(UUID userId);

    Optional<CreditCard> findByIdAndUserId(UUID id, UUID userId);

    List<CreditCard> findAllByActiveTrueAndUserId(UUID userId);
}
