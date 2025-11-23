package br.com.ronna.financerta.service.impl;

import br.com.ronna.financerta.dto.CreditCardStatementDto;
import br.com.ronna.financerta.dto.DailySummaryDto;
import br.com.ronna.financerta.dto.TransactionDto;
import br.com.ronna.financerta.enums.PaymentMethod;
import br.com.ronna.financerta.exception.TransactionException;
import br.com.ronna.financerta.model.*;
import br.com.ronna.financerta.repository.*;
import br.com.ronna.financerta.service.CreditCardStatementService;
import br.com.ronna.financerta.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository repo;
    private final UserRepository userRepo;
    private final WalletRepository walletRepo;
    private final CreditCardRepository creditCardRepo;
    private final TransactionCategoryRepository categoryRepo;
    private final CreditCardStatementRepository statementRepo;

    private final CreditCardStatementService  statementService;

    @Override
    public TransactionDto createTransaction(TransactionDto transactionDto, UUID userId) {
        // Validações
        var userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) {
            throw new TransactionException("Usuário não encontrado");
        }
        var walletOpt = walletRepo.findByIdAndUserId(transactionDto.getWalletId(), userId);
        if (walletOpt.isEmpty()) {
            throw new TransactionException("Carteira não encontrada para o usuário");
        }
        var categoryOpt = categoryRepo.findByIdAndUserId(transactionDto.getCategoryId(), userId);
        if (categoryOpt.isEmpty()) {
            throw new TransactionException("Categoria de transação não encontrada para o usuário");
        }
        Optional<CreditCard> creditCardOpt = Optional.empty();
        if (transactionDto.getPaymentMethod().equals(PaymentMethod.CREDIT_CARD)) {
            if (transactionDto.getCreditCardId() == null) {
                throw new TransactionException("ID do cartão de crédito é obrigatório para este método de pagamento");
            }
            creditCardOpt = creditCardRepo.findByIdAndUserId(transactionDto.getCreditCardId(), userId);
            if (creditCardOpt.isEmpty()) {
                throw new TransactionException("Cartão de crédito não encontrado para o usuário");
            }
        }

        // É uma compra parcelada?
        if (transactionDto.getTotalInstallments() != null && transactionDto.getTotalInstallments() > 1) {
            return installmentLogic(transactionDto, userOpt, creditCardOpt, categoryOpt, walletOpt);
        } else {
            // Lógica para transação única
            var transaction = new Transaction();

            transaction.setUser(userOpt.get());
            transaction.setWallet(walletOpt.get());

            transaction.setDescription(transactionDto.getDescription());
            transaction.setAmount(transactionDto.getAmount());
            transaction.setDate(transactionDto.getDate());
            transaction.setPaymentMethod(transactionDto.getPaymentMethod());
            transaction.setCategory(categoryOpt.get());
            transaction.setType(transactionDto.getType());

            if (transactionDto.getPaymentMethod().equals(PaymentMethod.CREDIT_CARD)) {
                transaction.setCreditCard(creditCardOpt.get());
                transaction.setCreditCardStatement(getOrCreateStatementForTransaction(transaction));
            }

            transaction.setCreatedAt(LocalDateTime.now());
            transaction.setUpdatedAt(LocalDateTime.now());
            var savedTransaction = repo.save(transaction);
            return convertToDto(savedTransaction);
        }
    }


    @Override
    public List<TransactionDto> getTransactions(UUID userId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            // List<Transaction> transactionList = repo.findByUserId(userId);
            // return transactionList.stream().map(this::convertToDto).toList();
            // Para não sobrecarregar o sistema, fazer o filtro se tornar o mês atual
            LocalDate now = LocalDate.now();
            startDate = now.withDayOfMonth(1);
            endDate = now.withDayOfMonth(now.lengthOfMonth());
        }
        List<Transaction> transactionList = repo.findByUserIdAndDateBetween(userId, startDate, endDate);
        return transactionList.stream().map(this::convertToDto).toList();
    }

    @Override
    public TransactionDto getTransactionById(UUID id, UUID userId) {
        return convertToDto(repo.findByIdAndUserId(id, userId).orElseThrow());
    }

    @Override
    public TransactionDto updateTransaction(UUID id, TransactionDto transactionDto, UUID userId) {
        // Validações
        var userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) {
            throw new TransactionException("Usuário não encontrado");
        }
        var walletOpt = walletRepo.findByIdAndUserId(transactionDto.getWalletId(), userId);
        if (walletOpt.isEmpty()) {
            throw new TransactionException("Carteira não encontrada para o usuário");
        }
        var categoryOpt = categoryRepo.findByIdAndUserId(transactionDto.getCategoryId(), userId);
        if (categoryOpt.isEmpty()) {
            throw new TransactionException("Categoria de transação não encontrada para o usuário");
        }
        Optional<CreditCard> creditCardOpt = Optional.empty();
        if (transactionDto.getPaymentMethod().equals(PaymentMethod.CREDIT_CARD)) {
            if (transactionDto.getCreditCardId() == null) {
                throw new TransactionException("ID do cartão de crédito é obrigatório para este método de pagamento");
            }
            creditCardOpt = creditCardRepo.findByIdAndUserId(transactionDto.getCreditCardId(), userId);
            if (creditCardOpt.isEmpty()) {
                throw new TransactionException("Cartão de crédito não encontrado para o usuário");
            }
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
            // if (transactionDto.getPaymentMethod().equals(PaymentMethod.CREDIT_CARD)) {
                // creditCardOpt = creditCardRepo.findByIdAndUserId(transactionDto.getCreditCardId(), userId);
            // }
            return installmentLogic(transactionDto, userOpt, creditCardOpt, categoryOpt, walletOpt);
        } else {
            if (isInstallmentToUnique) {
                var newTransaction = new Transaction();

                newTransaction.setUser(userOpt.get());
                newTransaction.setWallet(walletOpt.get());
                newTransaction.setDescription(transactionDto.getDescription());
                newTransaction.setAmount(transactionDto.getAmount());
                newTransaction.setDate(transactionDto.getDate());
                newTransaction.setPaymentMethod(transactionDto.getPaymentMethod());
                newTransaction.setCategory(categoryOpt.get());
                newTransaction.setType(transactionDto.getType());

                if (transactionDto.getPaymentMethod().equals(PaymentMethod.CREDIT_CARD)) {
                    newTransaction.setCreditCard(creditCardOpt.get());
                    newTransaction.setCreditCardStatement(getOrCreateStatementForTransaction(newTransaction));
                }

                newTransaction.setCreatedAt(existingTransaction.getCreatedAt());
                newTransaction.setUpdatedAt(LocalDateTime.now());
                var updatedTransaction = repo.save(newTransaction);
                return convertToDto(updatedTransaction);
            }
            // Atualizar transação única
            existingTransaction.setInstallmentNumber(transactionDto.getInstallmentNumber());
            existingTransaction.setTotalInstallments(transactionDto.getTotalInstallments());
            existingTransaction.setPaymentMethod(transactionDto.getPaymentMethod());
            existingTransaction.setAmount(transactionDto.getAmount());
            existingTransaction.setDate(transactionDto.getDate());
            existingTransaction.setPurchaseGroupId(transactionDto.getPurchaseGroupId());
            if (transactionDto.getPaymentMethod().equals(PaymentMethod.CREDIT_CARD)) {
                existingTransaction.setCreditCard(creditCardOpt.get());
                existingTransaction.setCreditCardStatement(getOrCreateStatementForTransaction(existingTransaction));
            } else {
                existingTransaction.setCreditCard(null);
                existingTransaction.setCreditCardStatement(null);
            }
            existingTransaction.setWallet(walletOpt.get());
            existingTransaction.setCategory(categoryOpt.get());
            existingTransaction.setUser(userOpt.get());
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

        dto.setId(transaction.getId() != null ? transaction.getId() : null);
        dto.setUserId(transaction.getUser().getId());
        dto.setWalletId(transaction.getWallet().getId());
        dto.setCreditCardId(transaction.getCreditCard() != null ? transaction.getCreditCard().getId() : null);
        dto.setCategoryId(transaction.getCategory().getId());

        dto.setType(transaction.getType());

        dto.setAmount(transaction.getAmount());
        dto.setDate(transaction.getDate());
        dto.setDescription(transaction.getDescription());
        dto.setPaymentMethod(transaction.getPaymentMethod());

        dto.setInstallmentNumber(transaction.getInstallmentNumber());
        dto.setPurchaseGroupId(transaction.getPurchaseGroupId());
        dto.setTotalInstallments(transaction.getTotalInstallments());

        return dto;
    }

    public List<DailySummaryDto> getDailySummariesExcludingCreditCard(UUID userId, LocalDate startDate, LocalDate endDate) {

        Map<LocalDate, BigDecimal> dailyTotals = new HashMap<>();

        List<DailySummaryDto> nonCreditCardSummaries = repo.findDailySummariesForNonCreditCard(userId, startDate, endDate);
        nonCreditCardSummaries.forEach(summary -> dailyTotals.put(summary.getDate(), summary.getAmount()));

        List<CreditCardStatement> allStatements = statementRepo.findAllByUser_Id(userId);
        for (CreditCardStatement statement : allStatements) {
            CreditCard card = statement.getCreditCard();

            int maxDayOfMonth = YearMonth.of(statement.getYear(), statement.getMonth()).lengthOfMonth();
            int validDueDay = Math.min(card.getDueDay(), maxDayOfMonth);
            LocalDate dueDate = LocalDate.of(statement.getYear(), statement.getMonth(), validDueDay);

            if(!dueDate.isBefore(startDate) && !dueDate.isAfter(endDate)){
                BigDecimal statementTotal = repo.getTotalAmountForStatement(statement.getId()).orElse(BigDecimal.ZERO);
                dailyTotals.merge(dueDate, statementTotal, BigDecimal::add);
            }
        }
        return dailyTotals.entrySet().stream()
                .map(entry -> new DailySummaryDto(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(DailySummaryDto::getDate))
                .collect(Collectors.toList());
    }

    private LocalDate getInstallmentDate(LocalDate date, int installmentNumber) {
        return date.plusMonths(installmentNumber - 1);
    }


    private TransactionDto installmentLogic(TransactionDto transactionDto, Optional<User> user, Optional<CreditCard> creditCard,
                                            Optional<TransactionCategory> category, Optional<Wallet> wallet) {
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
                transaction.setUser(user.get());
                transaction.setWallet(wallet.get());
                transaction.setCategory(category.get());
                transaction.setType(transactionDto.getType());
                transaction.setAmount(installmentAmount);

                // Ajusta a data da transação conforme o número da parcela
                transaction.setDate(getInstallmentDate(transactionDto.getDate(), i + 1));
                transaction.setDescription(transactionDto.getDescription() + " - Parcela " + (i + 1) + " de " + transactionDto.getTotalInstallments());
                transaction.setPurchaseGroupId(purchaseGroupId);
                transaction.setInstallmentNumber(i + 1);
                transaction.setTotalInstallments(transactionDto.getTotalInstallments());

                transaction.setCreatedAt(LocalDateTime.now());
                transaction.setUpdatedAt(LocalDateTime.now());

                // Verifica método de pagamento
                if (transactionDto.getPaymentMethod() == PaymentMethod.CREDIT_CARD) {
                    // Lógica para adicionar a transação na fatura correta do cartão de crédito
                    transaction.setCreditCard(creditCard.get());
                    transaction.setPaymentMethod(transactionDto.getPaymentMethod());
                    var st = getOrCreateStatementForTransaction(transaction);
                    transaction.setCreditCardStatement(st);
/*
                    // Lógica para vincular à fatura do cartão de crédito
                    // Verificar se existe fatura para o mês/ano da transação
                    int closingDay = creditCard.getClosingDay();
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
                        creditCardStatementDto.setCreditCardId(transactionDto.getCreditCardId());
                        creditCardStatementDto.setTransactions(List.of(transaction));
                        creditCardStatementDto = statementService.save(creditCardStatementDto, user.getId());
                        transaction.setCreditCardStatement(statementService.convertDtoToEntity(creditCardStatementDto));
                    } else {
                        transaction.setCreditCardStatement(statementOpt.get());
                    } */
                    savedTransaction = repo.save(transaction);
                } else {
                    // Lógica para boleto
                    LocalDate transactionDate = transaction.getDate();
                    if (transactionDate.getDayOfMonth() > 28 && transactionDate.getMonth().equals(Month.FEBRUARY)) {
                        transactionDate = transactionDate.withDayOfMonth(28);
                    }
                    if (transactionDate.getDayOfMonth() == 31) {
                        transactionDate = transactionDate.withDayOfMonth(transactionDate.lengthOfMonth());
                    }
                    transaction.setDate(transactionDate);
                    transaction.setPaymentMethod(transactionDto.getPaymentMethod());

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

    private CreditCardStatement getOrCreateStatementForTransaction(Transaction transaction) {
        CreditCard card = transaction.getCreditCard();
        LocalDate transactionDate = transaction.getDate();

        int closingDay = card.getClosingDay();
        int dueDay = card.getDueDay();

        // Definir a data de fechamento no mês da transação
        // Cuidado: Se for dia 31 e o mês tiver 30 dias, LocalDate.of dá erro.
        // Tratamos isso pegando o último dia válido do mês se necessário.
        int validClosingDay = Math.min(closingDay, transactionDate.lengthOfMonth());
        LocalDate closingDateThisMonth = transactionDate.withDayOfMonth(validClosingDay);

        // Determinar a "Data Base" para o cálculo do vencimento
        // Se a compra foi DEPOIS do fechamento, ela pertence ao mês seguinte.
        LocalDate referenceDate;
        if (transactionDate.isAfter(closingDateThisMonth)) {
            referenceDate = transactionDate.plusMonths(1);
        } else {
            referenceDate = transactionDate;
        }

        // Calcular a Data de Vencimento Real
        // Tentamos fixar o dia do vencimento no mês de referência
        int maxDayOfRefMonth = referenceDate.lengthOfMonth();
        int validDueDay = Math.min(dueDay, maxDayOfRefMonth);

        LocalDate finalDueDate = referenceDate.withDayOfMonth(validDueDay);

        // Se o dia do vencimento for menor que o fechamento,
        // significa que o vencimento é no mês seguinte ao da referência da fatura.
        if (dueDay < closingDay) {
            finalDueDate = finalDueDate.plusMonths(1);
        }

        //Cria o statement se não existir
        var st = statementRepo.findByCreditCardIdAndMonthAndYear(card.getId(), finalDueDate.getMonthValue(), finalDueDate.getYear());
        Optional<CreditCardStatement> stOpt;
        if(st.isEmpty()) {
            var newStatement = new CreditCardStatement();
            newStatement.setUser(transaction.getUser());
            newStatement.setCreditCard(card);
            newStatement.setTransactions(List.of(transaction));
            newStatement.setMonth(finalDueDate.getMonthValue());
            newStatement.setYear(finalDueDate.getYear());
            stOpt = Optional.of(statementRepo.save(newStatement));
        } else {
            stOpt = st;
        }

        return stOpt.get();
    }


}
