package br.com.ronna.financerta.controller;

import br.com.ronna.financerta.dto.CreditCardDto;
import br.com.ronna.financerta.model.CreditCard;
import br.com.ronna.financerta.service.CreditCardService;
import br.com.ronna.financerta.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/credit-cards")
@RequiredArgsConstructor
@Tag(name = "Credit Cards", description = "Endpoints de cartões de crédito")
public class CreditCardsController {

    private final CreditCardService creditCardService;

    @GetMapping
    public ResponseEntity<List<CreditCardDto>> getCreditCards(java.security.Principal principal) {
        UUID userId = SecurityUtils.getUserId(principal);
        var creditCards = creditCardService.findAll(userId);
        return ResponseEntity.ok(creditCards);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreditCardDto> getCreditCardById(@PathVariable UUID id, java.security.Principal principal) {
        UUID userId = SecurityUtils.getUserId(principal);
        var creditCard = creditCardService.findById(id, userId);
        return ResponseEntity.ok(creditCard);
    }

    @PostMapping("")
    public ResponseEntity<CreditCardDto> createCreditCard(java.security.Principal principal, @RequestBody CreditCardDto creditCardDto) {
        UUID userId = SecurityUtils.getUserId(principal);
        var createdCreditCard = creditCardService.save(creditCardDto, userId);
        return ResponseEntity.status(201).body(createdCreditCard);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CreditCardDto> updateCreditCard(@PathVariable java.util.UUID id, java.security.Principal principal, @RequestBody CreditCardDto creditCardDto) {
        UUID userId = SecurityUtils.getUserId(principal);
        return ResponseEntity.ok(creditCardService.update(id, creditCardDto, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCreditCard(@PathVariable java.util.UUID id, java.security.Principal principal) {
        UUID userId = SecurityUtils.getUserId(principal);
        creditCardService.delete(id, userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
