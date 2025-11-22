package br.com.ronna.financerta.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreditCardDto {

    private UUID id;
    private String name;
    private Integer closingDay;
    private Integer dueDay;
    private BigDecimal limitValue;
    private boolean active;
}
