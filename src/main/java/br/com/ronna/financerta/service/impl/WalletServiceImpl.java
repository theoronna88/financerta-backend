package br.com.ronna.financerta.service.impl;

import br.com.ronna.financerta.dto.WalletDto;
import br.com.ronna.financerta.model.Wallet;
import br.com.ronna.financerta.repository.WalletRepository;
import br.com.ronna.financerta.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository repo;

    @Override
    public WalletDto findById(UUID id, UUID userId) {
        return convertToDto(repo.findByIdAndUserId(id, userId).orElse(null));
    }

    @Override
    public List<WalletDto> getWallets(UUID userId) {
        List<Wallet> walletList = repo.findAllByUserId(userId);
        return walletList.stream().map(this::convertToDto).toList();
    }

    @Override
    public WalletDto createWallet(WalletDto walletDto, UUID userId) {
        var wallet = new Wallet();
        BeanUtils.copyProperties(walletDto, wallet);
        wallet.setCreatedAt(LocalDateTime.now());
        wallet.setActive(true);
        wallet.setUpdatedAt(LocalDateTime.now());
        wallet.setUserId(userId);
        return convertToDto(repo.save(wallet));
    }

    @Override
    public WalletDto updateWallet(UUID id, WalletDto walletDto, UUID userId) {
        var existingWallet = repo.findByIdAndUserId(id, userId).orElseThrow();

        existingWallet.setName(walletDto.getName());
        existingWallet.setColor(walletDto.getColor());
        existingWallet.setInitialBalance(walletDto.getInitialBalance());
        existingWallet.setUpdatedAt(LocalDateTime.now());
        // TODO: Ao atualizar o saldo inicial, ajustar as transações associadas, se necessário
        return convertToDto(repo.save(existingWallet));
    }

    @Override
    public void deleteWalletById(UUID walletId, UUID userId) {
        var wallet = repo.findByIdAndUserId(walletId, userId).orElseThrow();

        wallet.setUpdatedAt(LocalDateTime.now());
        wallet.setActive(false);
        repo.save(wallet);

        // TODO: Verificar se há transações associadas e desativá-las também, se necessário
    }

    private WalletDto convertToDto(Wallet wallet) {
        WalletDto dto = new WalletDto();
        BeanUtils.copyProperties(wallet, dto);
        return dto;
    }

}
