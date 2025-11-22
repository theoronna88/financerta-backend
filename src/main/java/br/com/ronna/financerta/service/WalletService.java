package br.com.ronna.financerta.service;

import br.com.ronna.financerta.dto.WalletDto;
import br.com.ronna.financerta.model.Wallet;

import java.util.List;
import java.util.UUID;

public interface WalletService {

    WalletDto findById(UUID id, UUID userId);

    List<WalletDto> getWallets(UUID userId);

    WalletDto createWallet(WalletDto walletDto, UUID userId);

    WalletDto updateWallet(UUID id, WalletDto walletDto, UUID userId);

    void deleteWalletById(UUID walletId, UUID userId);


}
