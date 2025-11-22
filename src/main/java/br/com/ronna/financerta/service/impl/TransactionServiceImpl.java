package br.com.ronna.financerta.service.impl;

import br.com.ronna.financerta.dto.CreditCardStatementDto;
import br.com.ronna.financerta.dto.TransactionDto;
import br.com.ronna.financerta.enums.PaymentMethod;
import br.com.ronna.financerta.exception.TransactionException;
import br.com.ronna.financerta.model.CreditCard;
import br.com.ronna.financerta.model.CreditCardStatement;
import br.com.ronna.financerta.model.Transaction;
import br.com.ronna.financerta.repository.*;
import br.com.ronna.financerta.service.CreditCardStatementService;
import br.com.ronna.financerta.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository repo;
    private final UserRepository userRepo;
    private final WalletRepository walletRepo;
    private final CreditCardRepository creditCardRepo;
    private final TransactionCategoryRepository categoryRepo;
    private final CreditCardStatementRepository statementRepo;

    private final CreditCardStatementService statementService;

    @Override
    public TransactionDto createTransaction(TransactionDto transactionDto, UUID userId) {
        // Verificar se usuário existe x
        // Verificar se carteira existe e pertence ao usuário x
        // Verificar se cartão de crédito != null, existe e pertence ao usuário x
        // Verificar se categoria != null, existe e pertence ao usuário x
        // Verificar se é compra parcelada
        // Criar grupo de compra se necessário
        // Verificar em qual fatura do cartão de crédito a transação deve ser adicionada, se aplicável
        // Salvar transação(s)

        // Validações
        if (!isValidTransactionDto(transactionDto, userId)){
            throw new TransactionException("Dados inválidos para criação de transação");
        }

        // É uma compra parcelada?
        if (transactionDto.getTotalInstallments() != null && transactionDto.getTotalInstallments() > 1) {
            Optional<CreditCard> creditCardOpt = Optional.empty();
            if (transactionDto.getPaymentMethod().equals(PaymentMethod.CREDIT_CARD)) {
                creditCardOpt = creditCardRepo.findByIdAndUserId(transactionDto.getCreditCardId(), userId);
            }
            return installmentLogic(transactionDto, userId, creditCardOpt);
        } else {
            // Lógica para transação única
            var transaction = new Transaction();
            BeanUtils.copyProperties(transactionDto, transaction);
            transaction.setCreatedAt(LocalDateTime.now());
            transaction.setUpdatedAt(LocalDateTime.now());
            var savedTransaction = repo.save(transaction);
            return convertToDto(savedTransaction);
        }
    }

    @Override
    public List<TransactionDto> getTransactions(UUID userId) {
        List<Transaction> transactionList = repo.findByUserId(userId);
        return transactionList.stream().map(this::convertToDto).toList();
    }

    @Override
    public TransactionDto getTransactionById(UUID id, UUID userId) {
        return convertToDto(repo.findByIdAndUserId(id, userId).orElseThrow());
    }

    @Override
    public TransactionDto updateTransaction(UUID id, TransactionDto transactionDto, UUID userId) {
        // Realizar primeiro as validações semelhantes ao createTransaction
        // Depois atualizar os campos permitidos
        if (!isValidTransactionDto(transactionDto, userId)){
            throw new TransactionException("Dados inválidos para atualização de transação");
        }

        var existingTransaction = repo.findByIdAndUserId(id, userId).orElseThrow();

        boolean isInstallmentToUnique = false;
        if (existingTransaction.getPurchaseGroupId() != null) {
            // Deleta todas as parcelas do grupo de compra
            var transactions = repo.findByPurchaseGroupId(existingTransaction.getPurchaseGroupId());
            if(transactions.isEmpty()) {
                throw new TransactionException("Nenhuma transação encontrada para o grupo de compra");
            }
            repo.deleteAll(transactions.stream().toList());
            isInstallmentToUnique = true;
        }
        if (transactionDto.getTotalInstallments() != null && transactionDto.getTotalInstallments() > 1 ) {
            Optional<CreditCard> creditCardOpt = Optional.empty();
            if (transactionDto.getPaymentMethod().equals(PaymentMethod.CREDIT_CARD)) {
                creditCardOpt = creditCardRepo.findByIdAndUserId(transactionDto.getCreditCardId(), userId);
            }
            return installmentLogic(transactionDto, userId, creditCardOpt);
        } else {
            if (isInstallmentToUnique) {
                var newTransaction = new Transaction();
                BeanUtils.copyProperties(transactionDto, newTransaction);
                newTransaction.setCreatedAt(existingTransaction.getCreatedAt());
                newTransaction.setUpdatedAt(LocalDateTime.now());
                var updatedTransaction = repo.save(newTransaction);
                return convertToDto(updatedTransaction);
            }
            // Atualizar transação única
            existingTransaction.setInstallmentNumber(transactionDto.getInstallmentNumber());
            existingTransaction.setTotalInstallments(transactionDto.getTotalInstallments());
            existingTransaction.setAmount(transactionDto.getAmount());
            existingTransaction.setDate(transactionDto.getDate());
            existingTransaction.setPurchaseGroupId(transactionDto.getPurchaseGroupId());
            existingTransaction.setCreditCardId(transactionDto.getCreditCardId());
            existingTransaction.setCategoryId(transactionDto.getCategoryId());
            existingTransaction.setUserId(userId);
            existingTransaction.setDescription(transactionDto.getDescription());
            existingTransaction.setUpdatedAt(LocalDateTime.now());
            var updatedTransaction = repo.save(existingTransaction);
            return convertToDto(updatedTransaction);
        }
    }

    @Override
    public void deleteTransaction(UUID id, UUID userId) {
        var transaction = repo.findByIdAndUserId(id, userId).orElseThrow();
        if (transaction.getPurchaseGroupId() != null) {
            // Deletar todas as parcelas do grupo de compra
            var transactions = repo.findByPurchaseGroupId(transaction.getPurchaseGroupId());
            if(transactions.isEmpty()) {
                throw new TransactionException("Nenhuma transação encontrada para o grupo de compra");
            }
            repo.deleteAll(transactions.stream().toList());
        } else {
            repo.delete(transaction);
        }
    }

    private TransactionDto convertToDto(Transaction transaction) {
        var dto = new TransactionDto();
        BeanUtils.copyProperties(transaction, dto);
        return dto;
    }

    private LocalDate getInstallmentDate(LocalDate date, int installmentNumber) {
        return date.plusMonths(installmentNumber - 1);
    }

    private CreditCardStatementDto convertToDto(CreditCardStatement creditCardStatement) {
        var dto = new CreditCardStatementDto();
        BeanUtils.copyProperties(creditCardStatement, dto);
        return dto;
    }

    private boolean isValidTransactionDto(TransactionDto transactionDto, UUID userId) {
        // Implementar validações necessárias
        if (!userRepo.existsById(userId)) {
            throw new TransactionException("Usuário não encontrado");
        }
        if (!walletRepo.existsByIdAndUserId(transactionDto.getWalletId(), userId)) {
            throw new TransactionException("Carteira não encontrada para o usuário");
        }

        if (transactionDto.getCategoryId() == null) {
            throw new TransactionException("Categoria de transação é obrigatória");
        }
        if (!categoryRepo.existsByIdAndUserId(transactionDto.getCategoryId(), userId)) {
            throw new TransactionException("Categoria de transação não encontrada para o usuário");
        }

        Optional<CreditCard> creditCardOpt = Optional.empty();
        if (transactionDto.getPaymentMethod().equals(PaymentMethod.CREDIT_CARD)) {
            if(transactionDto.getCreditCardId() == null) {
                throw new TransactionException("Cartão de crédito é obrigatório para compra com cartão de crédito");
            }
            creditCardOpt = creditCardRepo.findByIdAndUserId(transactionDto.getCreditCardId(), userId);
            if (creditCardOpt.isEmpty()) {
                throw new TransactionException("Cartão de crédito não encontrado para o usuário");
            }
        }
        return true;
    }

    private TransactionDto installmentLogic(TransactionDto transactionDto, UUID userId, Optional<CreditCard> creditCardOpt) {
            var savedTransaction = new Transaction();
            // Criação do grupo de compra parcelada
            var purchaseGroupId = UUID.randomUUID();

            // Lógica para compra parcelada
            if (!transactionDto.getPaymentMethod().equals(PaymentMethod.BOLETO) &&
                    !transactionDto.getPaymentMethod().equals(PaymentMethod.CREDIT_CARD)) {
                throw new TransactionException("Método de pagamento inválido para compra parcelada");
            }

            BigDecimal installmentAmount = transactionDto.getAmount().divide(new BigDecimal(transactionDto.getTotalInstallments()), 2, RoundingMode.HALF_UP);

            for (int i = 0; i < transactionDto.getTotalInstallments(); i++) {
                var transaction = new Transaction();
                // Verifica método de pagamento
                if (transactionDto.getPaymentMethod() == PaymentMethod.CREDIT_CARD) {
                    // Lógica para adicionar a transação na fatura correta do cartão de crédito
                    BeanUtils.copyProperties(transactionDto, transaction);
                    transaction.setCreatedAt(LocalDateTime.now());
                    transaction.setUpdatedAt(LocalDateTime.now());
                    transaction.setInstallmentNumber(i + 1);
                    // Ajustar a data da transação conforme o número da parcela
                    transaction.setDate(getInstallmentDate(transactionDto.getDate(), i + 1));
                    transaction.setPurchaseGroupId(purchaseGroupId);
                    transaction.setCreditCardId(transactionDto.getCreditCardId());
                    transaction.setAmount(installmentAmount);

                    // Lógica para vincular à fatura do cartão de crédito
                    // Verificar se existe fatura para o mês/ano da transação
                    // TODO: verificar a fatura correta considerando a data de fechamento do cartão
                    int closingDay = creditCardOpt.get().getClosingDay();
                    // Se closingDay == 31, considerar o último dia do mês
                    LocalDate transactionDate = transaction.getDate();
                    if (transactionDate.getMonth().equals(Month.FEBRUARY) && closingDay > 28) {
                        closingDay = transactionDate.lengthOfMonth();
                    }
                    if (!transactionDate.getMonth().equals(Month.FEBRUARY) && closingDay == 31) {
                        closingDay = transactionDate.lengthOfMonth();
                    }
                    if (transactionDate.getDayOfMonth() > closingDay) {
                        // Adicionar à fatura do próximo mês
                        transactionDate = transactionDate.plusMonths(1);
                    }

                    var statementOpt = statementRepo.findByCreditCardIdAndMonthAndYear(
                            transactionDto.getCreditCardId(),
                            transactionDate.getMonthValue(),
                            transactionDate.getYear()
                    );
                    CreditCardStatementDto creditCardStatementDto;
                    if (statementOpt.isEmpty()) {
                        // Criar nova fatura
                        creditCardStatementDto = new CreditCardStatementDto();
                        creditCardStatementDto.setYear(transaction.getDate().getYear());
                        creditCardStatementDto.setMonth(transaction.getDate().getMonthValue());
                        creditCardStatementDto.setCreditCardId(transaction.getCreditCardId());
                        creditCardStatementDto.setTransactions(List.of(transaction));
                        creditCardStatementDto = statementService.save(creditCardStatementDto, userId);
                    } else {
                        creditCardStatementDto = convertToDto(statementOpt.get());
                    }
                    transaction.setCreditCardStatementId(creditCardStatementDto.getId());
                    savedTransaction = repo.save(transaction);
                } else {
                    // Lógica para boleto
                    BeanUtils.copyProperties(transactionDto, transaction);
                    transaction.setCreatedAt(LocalDateTime.now());
                    transaction.setUpdatedAt(LocalDateTime.now());
                    transaction.setInstallmentNumber(i + 1);
                    // Ajustar a data da transação conforme o número da parcela
                    transaction.setDate(getInstallmentDate(transactionDto.getDate(), i + 1));
                    transaction.setPurchaseGroupId(purchaseGroupId);
                    transaction.setAmount(installmentAmount);
                    savedTransaction = repo.save(transaction);
                }
            }
            return convertToDto(savedTransaction);
    }


}
