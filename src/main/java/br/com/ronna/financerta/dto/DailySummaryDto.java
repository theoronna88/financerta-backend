package br.com.ronna.financerta.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailySummaryDto {

    private LocalDate date;
    private BigDecimal amount;
}
