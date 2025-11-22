package br.com.ronna.financerta.service;

import br.com.ronna.financerta.dto.CreditCardDto;
import br.com.ronna.financerta.model.CreditCard;

import java.util.List;
import java.util.UUID;

public interface CreditCardService {

    List<CreditCardDto> findAll(UUID userId);
    CreditCardDto findById(UUID id, UUID userId);
    CreditCardDto save(CreditCardDto creditCardDto, UUID userId);
    CreditCardDto update(UUID id, CreditCardDto creditCardDto, UUID userId);
    void delete(UUID id, UUID userId);
}
