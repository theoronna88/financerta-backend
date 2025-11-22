package br.com.ronna.financerta.controller;

import br.com.ronna.financerta.dto.WalletDto;
import br.com.ronna.financerta.service.WalletService;
import br.com.ronna.financerta.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Endpoints da carteira")
public class WalletController {

    private final WalletService walletService;

    @GetMapping
   public ResponseEntity<List<WalletDto>> getWallet(java.security.Principal principal) {
        UUID userId = SecurityUtils.getUserId(principal);
       return ResponseEntity.status(HttpStatus.OK).body(walletService.getWallets(userId));
   }

   @GetMapping("/{id}")
   public ResponseEntity<WalletDto> getWalletById(@PathVariable UUID id, java.security.Principal principal) {
        UUID userId = SecurityUtils.getUserId(principal);
        return ResponseEntity.status(HttpStatus.OK).body(walletService.findById(id, userId));
    }

    @PostMapping("")
    public ResponseEntity<WalletDto> createWallet(java.security.Principal principal, @RequestBody WalletDto walletDto) {
        UUID userId = SecurityUtils.getUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(walletService.createWallet(walletDto, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WalletDto> updateWallet(@PathVariable UUID id, @RequestBody WalletDto walletDto, java.security.Principal principal) {
        UUID userId = SecurityUtils.getUserId(principal);
        return ResponseEntity.status(HttpStatus.OK).body(walletService.updateWallet(id, walletDto, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWallet(@PathVariable UUID id, java.security.Principal principal) {
        UUID userId = SecurityUtils.getUserId(principal);
        walletService.deleteWalletById(id, userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
