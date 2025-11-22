package br.com.ronna.financerta.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class WalletDto {

    private UUID id;
    private String name;
    private String color;
    private BigDecimal initialBalance;
    private boolean active;
}