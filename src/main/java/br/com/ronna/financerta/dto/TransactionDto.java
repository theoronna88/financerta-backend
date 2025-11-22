package br.com.ronna.financerta.dto;

import br.com.ronna.financerta.enums.PaymentMethod;
import br.com.ronna.financerta.enums.TransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class TransactionDto {

    private UUID id;
    private UUID userId;
    private UUID walletId;
    private UUID creditCardId;
    private UUID categoryId;

    private TransactionType type;

    private BigDecimal amount;
    private LocalDate date;
    private String description;
    private PaymentMethod paymentMethod;

    private UUID purchaseGroupId;
    private Integer installmentNumber;
    private Integer totalInstallments;

    private UUID creditCardStatementId;

}
