package br.com.ronna.financerta.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "credit_card_statements")
public class CreditCardStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Column(nullable = false)
    private UUID userId;
    @Column(nullable = false)
    private UUID creditCardId;

    @Column(nullable = false)
    private Integer month;
    @Column(nullable = false)
    private Integer year;


    private List<Transaction> transactions;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
