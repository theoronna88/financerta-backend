package br.com.ronna.financerta.service;

import br.com.ronna.financerta.dto.CreditCardStatementDto;
import br.com.ronna.financerta.model.CreditCardStatement;

import java.util.List;
import java.util.UUID;

public interface CreditCardStatementService {

    CreditCardStatementDto save(CreditCardStatementDto creditCardStatementDto, UUID userId);
    CreditCardStatementDto getById(UUID id, UUID userId);
    List<CreditCardStatementDto> getAllByCreditCardId(UUID creditCardId, UUID userId);
    CreditCardStatementDto update(CreditCardStatementDto creditCardStatementDto, UUID userId);


    CreditCardStatement convertDtoToEntity(CreditCardStatementDto creditCardStatementDto);
}
