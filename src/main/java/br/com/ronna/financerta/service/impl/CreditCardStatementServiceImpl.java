package br.com.ronna.financerta.service.impl;

import br.com.ronna.financerta.dto.CreditCardStatementDto;
import br.com.ronna.financerta.model.CreditCardStatement;
import br.com.ronna.financerta.service.CreditCardStatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreditCardStatementServiceImpl implements CreditCardStatementService {
    @Override
    public CreditCardStatementDto save(CreditCardStatementDto creditCardStatementDto, UUID userId) {
        CreditCardStatement statement = new CreditCardStatement();
        BeanUtils.copyProperties(creditCardStatementDto, statement);
        statement.setCreatedAt(LocalDateTime.now());
        statement.setUpdatedAt(LocalDateTime.now());

        return null;
    }

    @Override
    public CreditCardStatementDto getById(UUID id, UUID userId) {
        return null;
    }

    @Override
    public List<CreditCardStatementDto> getAllByCreditCardId(UUID creditCardId, UUID userId) {
        return List.of();
    }

    @Override
    public CreditCardStatementDto update(CreditCardStatementDto creditCardStatementDto, UUID userId) {
        return null;
    }

    @Override
    public CreditCardStatement convertDtoToEntity(CreditCardStatementDto dto) {
        CreditCardStatement entity = new CreditCardStatement();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
