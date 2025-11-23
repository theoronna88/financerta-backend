package br.com.ronna.financerta.repository;

import br.com.ronna.financerta.model.CreditCard;
import br.com.ronna.financerta.model.CreditCardStatement;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditCardStatementRepository extends CrudRepository<CreditCardStatement, UUID> {
    Optional<CreditCardStatement> findByCreditCardIdAndMonthAndYear(UUID creditCardId, int month, int year);
    List<CreditCardStatement> findAllByUser_Id(UUID userId);

}
