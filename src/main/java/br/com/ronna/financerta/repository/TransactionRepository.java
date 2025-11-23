package br.com.ronna.financerta.repository;

import br.com.ronna.financerta.dto.DailySummaryDto;
import br.com.ronna.financerta.dto.TransactionDto;
import br.com.ronna.financerta.model.Transaction;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends CrudRepository<Transaction, UUID> {
    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);
    List<Transaction> findByUserId(UUID userId);

    List<Transaction> findByPurchaseGroupId(UUID purchaseGroupId);
    List<Transaction> findByUserIdAndDateBetween(UUID userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT new br.com.ronna.financerta.dto.DailySummaryDto(t.date, SUM(t.amount)) " +
            "FROM Transaction t " +
            "WHERE t.user.id = :userId AND t.paymentMethod <> 'CREDIT_CARD' AND t.date BETWEEN :startDate AND :endDate " +
            "GROUP BY t.date")
    List<DailySummaryDto> findDailySummariesForNonCreditCard(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.creditCardStatement.id = :statementId")
    Optional<BigDecimal> getTotalAmountForStatement(@Param("statementId") UUID statementId);
}
