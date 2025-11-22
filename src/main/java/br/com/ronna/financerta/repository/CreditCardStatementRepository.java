package br.com.ronna.financerta.repository;

import br.com.ronna.financerta.model.CreditCardStatement;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface CreditCardStatementRepository extends CrudRepository<CreditCardStatement, UUID> {
    Optional<CreditCardStatement> findByCreditCardIdAndMonthAndYear(UUID creditCardId, int month, int year);
}
