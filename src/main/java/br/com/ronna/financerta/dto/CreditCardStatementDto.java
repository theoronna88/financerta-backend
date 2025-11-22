package br.com.ronna.financerta.dto;

import br.com.ronna.financerta.model.Transaction;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreditCardStatementDto {
    private UUID id;
    private UUID userId;
    private UUID creditCardId;

    private Integer month;
    private Integer year;

    private List<Transaction> transactions;
}
