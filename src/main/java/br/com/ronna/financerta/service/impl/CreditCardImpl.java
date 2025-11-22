package br.com.ronna.financerta.service.impl;

import br.com.ronna.financerta.dto.CreditCardDto;
import br.com.ronna.financerta.model.CreditCard;
import br.com.ronna.financerta.repository.CreditCardRepository;
import br.com.ronna.financerta.service.CreditCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class CreditCardImpl implements CreditCardService {

    private final CreditCardRepository repo;


    @Override
    public List<CreditCardDto> findAll(UUID userId) {
        List<CreditCard> creditCardList = repo.findAllByActiveTrueAndUserId(userId);
        return creditCardList.stream().map(this::convertToDto).toList();
    }

    @Override
    public CreditCardDto findById(UUID id, UUID userId) {
        return convertToDto(repo.findByIdAndUserId(id, userId).orElseThrow());
    }

    @Override
    public CreditCardDto save(CreditCardDto creditCardDto, UUID userId) {
        var creditCard = new CreditCard();
        creditCard.setName(creditCardDto.getName());
        creditCard.setClosingDay(creditCardDto.getClosingDay());
        creditCard.setDueDay(creditCardDto.getDueDay());
        creditCard.setLimitValue(creditCardDto.getLimitValue());
        creditCard.setUserId(userId);

        creditCard.setCreatedAt(LocalDateTime.now());
        creditCard.setUpdatedAt(LocalDateTime.now());

        return convertToDto(repo.save(creditCard));
    }

    @Override
    public CreditCardDto update(UUID id, CreditCardDto creditCardDto, UUID userId) {
        var existingCard = repo.findByIdAndUserId(id, userId).orElseThrow();
        existingCard.setName(creditCardDto.getName());
        existingCard.setClosingDay(creditCardDto.getClosingDay());
        existingCard.setDueDay(creditCardDto.getDueDay());
        existingCard.setLimitValue(creditCardDto.getLimitValue());
        return convertToDto(repo.save(existingCard));
    }

    @Override
    public void delete(UUID id, UUID userId) {
        var creditCard = repo.findByIdAndUserId(id, userId).orElseThrow();
        creditCard.setActive(false);
        repo.save(creditCard);

        // TODO: Verificar se há transações associadas e desativá-las também, se necessário
    }

    private CreditCardDto convertToDto(CreditCard creditCard) {
        var creditCardDto = new CreditCardDto();
        BeanUtils.copyProperties(creditCard, creditCardDto);
        return creditCardDto;
    }
}
