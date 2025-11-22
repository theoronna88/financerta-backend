package br.com.ronna.financerta.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class TransactionCategoryDto {

    private UUID id;
    private String name;
}
