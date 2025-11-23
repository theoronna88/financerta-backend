package br.com.ronna.financerta.service.impl;

import br.com.ronna.financerta.dto.CreditCardStatementDto;
import br.com.ronna.financerta.dto.TransactionDto;
import br.com.ronna.financerta.enums.PaymentMethod;
import br.com.ronna.financerta.enums.TransactionType;
import br.com.ronna.financerta.exception.TransactionException;
import br.com.ronna.financerta.model.*;
import br.com.ronna.financerta.model.CreditCard;
import br.com.ronna.financerta.model.CreditCardStatement;
import br.com.ronna.financerta.model.Transaction;
import br.com.ronna.financerta.model.User;
import br.com.ronna.financerta.model.Wallet;
import br.com.ronna.financerta.model.TransactionCategory;
import br.com.ronna.financerta.repository.*;
import br.com.ronna.financerta.service.CreditCardStatementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para TransactionServiceImpl.
 * <p>
 * Esta classe implementa os 5 testes críticos especificados no plano de testes:
 * 1. Criação de transação única (Happy Path)
 * 2. Validação falhando quando carteira não encontrada (Sad Path)
 * 3. Criação de compra parcelada no cartão de crédito (Cenário complexo)
 * 4. Exclusão de transação única sem grupo de compra (Cenário de exclusão simples)
 * 5. Exclusão de todas as parcelas ao deletar uma (Cenário de exclusão em cascata)
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository repo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private WalletRepository walletRepo;

    @Mock
    private CreditCardRepository creditCardRepo;

    @Mock
    private TransactionCategoryRepository categoryRepo;

    @Mock
    private CreditCardStatementRepository statementRepo;

    @Mock
    private CreditCardStatementService statementService;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    /**
     * Teste 1: Criação de Transação Única (Happy Path)
     * <p>
     * Objetivo: Garantir que uma transação simples, à vista, é criada corretamente.
     * <p>
     * Cenário:
     * - Usuário existe
     * - Carteira existe e pertence ao usuário
     * - Categoria existe e pertence ao usuário
     * - Transação sem parcelas (totalInstallments = 1)
     * - Método de pagamento: PIX
     * <p>
     * Resultado esperado:
     * - Transação é salva com sucesso
     * - DTO retornado contém os dados corretos
     * - Método save do repositório é chamado exatamente 1 vez
     */
    @Test
    void shouldCreateSingleTransactionSuccessfully() {
        // Arrange (Arrumar)
        TransactionDto inputDto = new TransactionDto();
        inputDto.setAmount(new BigDecimal("100.00"));
        inputDto.setTotalInstallments(1);
        inputDto.setDate(LocalDate.now());
        inputDto.setDescription("Compra simples teste");
        inputDto.setPaymentMethod(PaymentMethod.PIX);
        inputDto.setType(TransactionType.EXPENSE);

        UUID userId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        inputDto.setWalletId(walletId);
        inputDto.setCategoryId(categoryId);

        // Simula o retorno para as validações
        User mockUser = new User();
        mockUser.setId(userId);

        Wallet mockWallet = new Wallet();
        mockWallet.setId(walletId);
        mockWallet.setUser(mockUser);

        TransactionCategory mockCategory = new TransactionCategory();
        mockCategory.setId(categoryId);
        mockCategory.setUser(mockUser);

        when(userRepo.findById(userId)).thenReturn(Optional.of(mockUser));
        when(walletRepo.findByIdAndUserId(walletId, userId)).thenReturn(Optional.of(mockWallet));
        when(categoryRepo.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(mockCategory));

        // Quando o repo.save for chamado, retorne uma entidade Transaction mockada
        Transaction savedTransaction = new Transaction();
        savedTransaction.setId(UUID.randomUUID());
        savedTransaction.setUser(mockUser);
        savedTransaction.setWallet(mockWallet);
        savedTransaction.setCategory(mockCategory);
        savedTransaction.setAmount(inputDto.getAmount());
        savedTransaction.setDescription(inputDto.getDescription());
        savedTransaction.setDate(inputDto.getDate());
        savedTransaction.setPaymentMethod(inputDto.getPaymentMethod());
        savedTransaction.setType(inputDto.getType());
        savedTransaction.setTotalInstallments(1);
        savedTransaction.setCreatedAt(LocalDateTime.now());
        savedTransaction.setUpdatedAt(LocalDateTime.now());

        when(repo.save(any(Transaction.class))).thenReturn(savedTransaction);

        // Act (Agir)
        TransactionDto resultDto = transactionService.createTransaction(inputDto, userId);

        // Assert (Afirmar)
        assertNotNull(resultDto, "O resultado não deve ser nulo");
        assertEquals(savedTransaction.getId(), resultDto.getId(), "Os IDs devem ser iguais");
        assertEquals(savedTransaction.getAmount(), resultDto.getAmount(), "Os valores devem ser iguais");
        assertEquals(savedTransaction.getDescription(), resultDto.getDescription(), "As descrições devem ser iguais");
        verify(repo, times(1)).save(any(Transaction.class));
    }

    /**
     * Teste 2: Validação Falhando (Sad Path)
     * <p>
     * Objetivo: Garantir que o serviço lança uma exceção se uma validação falhar.
     * <p>
     * Cenário:
     * - Usuário existe
     * - Carteira NÃO existe para o usuário
     * <p>
     * Resultado esperado:
     * - TransactionException é lançada
     * - Mensagem da exceção indica dados inválidos
     * - Método save do repositório NÃO é chamado
     */
    @Test
    void shouldThrowExceptionWhenWalletNotFound() {
        // Arrange
        TransactionDto inputDto = new TransactionDto();
        inputDto.setAmount(new BigDecimal("50.00"));
        inputDto.setDate(LocalDate.now());
        inputDto.setDescription("Teste validação");
        inputDto.setPaymentMethod(PaymentMethod.CASH);
        inputDto.setType(TransactionType.EXPENSE);

        UUID userId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        inputDto.setWalletId(walletId);
        inputDto.setCategoryId(categoryId);

        User mockUser = new User();
        mockUser.setId(userId);
        when(userRepo.findById(userId)).thenReturn(Optional.of(mockUser));
        when(walletRepo.findByIdAndUserId(walletId, userId)).thenReturn(Optional.empty()); // Simula que a carteira não existe

        // Act & Assert
        // Verifica se a chamada ao método lança a exceção TransactionException
        TransactionException exception = assertThrows(TransactionException.class, () -> {
            transactionService.createTransaction(inputDto, userId);
        });

        // Verifica a mensagem da exceção - a validação lança uma mensagem específica
        assertEquals("Carteira não encontrada para o usuário", exception.getMessage());

        // Verifica que o save nunca foi chamado
        verify(repo, never()).save(any(Transaction.class));
    }

    /**
     * Teste 3: Criação de Compra Parcelada no Cartão
     * <p>
     * Objetivo: Testar o cenário mais complexo: uma compra parcelada no cartão de crédito,
     * forçando a criação de uma nova fatura.
     * <p>
     * Cenário:
     * - Compra parcelada em 3x no cartão de crédito
     * - Valor total: R$ 300,00 (R$ 100,00 por parcela)
     * - Todas as validações passam
     * - Não há faturas existentes (forçando criação de novas)
     * - Cartão tem dia de fechamento = 15
     * <p>
     * Resultado esperado:
     * - 3 transações são salvas (uma para cada parcela)
     * - Cada parcela tem valor de R$ 100,00
     * - Parcelas têm números sequenciais (1, 2, 3)
     * - Todas as parcelas compartilham o mesmo purchaseGroupId
     * - Método save é chamado 3 vezes
     */
    @Test
    void shouldCreateInstallmentPurchaseOnCreditCard() {
        // Arrange
        TransactionDto inputDto = new TransactionDto();
        inputDto.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        inputDto.setTotalInstallments(3); // 3 parcelas
        inputDto.setAmount(new BigDecimal("300.00"));
        inputDto.setDate(LocalDate.of(2025, 11, 10)); // Data antes do fechamento
        inputDto.setDescription("Compra parcelada teste");
        inputDto.setType(TransactionType.EXPENSE);

        UUID userId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID creditCardId = UUID.randomUUID();

        inputDto.setWalletId(walletId);
        inputDto.setCategoryId(categoryId);
        inputDto.setCreditCardId(creditCardId);

        User mockUser = new User();
        mockUser.setId(userId);

        CreditCard mockCreditCard = new CreditCard();
        mockCreditCard.setId(creditCardId);
        mockCreditCard.setUser(mockUser);
        mockCreditCard.setClosingDay(15); // Dia de fechamento da fatura
        mockCreditCard.setDueDay(25);
        mockCreditCard.setName("Cartão Teste");
        mockCreditCard.setLimitValue(new BigDecimal("5000.00"));

        // Mockar todas as validações para retornarem valores válidos
        when(userRepo.findById(userId)).thenReturn(Optional.of(mockUser));
        when(walletRepo.findByIdAndUserId(walletId, userId)).thenReturn(Optional.of(new Wallet()));
        when(categoryRepo.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(new TransactionCategory()));
        when(creditCardRepo.findByIdAndUserId(creditCardId, userId)).thenReturn(Optional.of(mockCreditCard));

        // Simula que não há faturas existentes, forçando a criação de novas
        when(statementRepo.findByCreditCardIdAndMonthAndYear(eq(creditCardId), anyInt(), anyInt()))
                .thenReturn(Optional.empty());

        // Simula a criação de uma nova fatura pelo statementService
        CreditCardStatementDto newStatementDto = new CreditCardStatementDto();
        newStatementDto.setId(UUID.randomUUID());
        newStatementDto.setCreditCardId(creditCardId);
        when(statementService.save(any(CreditCardStatementDto.class), eq(userId))).thenReturn(newStatementDto);

        // Simula a conversão do DTO para entidade
        CreditCardStatement mockStatement = new CreditCardStatement();
        mockStatement.setId(newStatementDto.getId());
        mockStatement.setCreditCard(mockCreditCard);
        when(statementService.convertDtoToEntity(any(CreditCardStatementDto.class))).thenReturn(mockStatement);

        // Quando o repo.save for chamado, apenas retorne o que foi passado
        when(repo.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(UUID.randomUUID());
            return tx;
        });

        // Act
        TransactionDto result = transactionService.createTransaction(inputDto, userId);

        // Assert
        assertNotNull(result, "O resultado não deve ser nulo");

        // Captura todos os argumentos passados para repo.save
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(repo, times(3)).save(transactionCaptor.capture());

        List<Transaction> savedTransactions = transactionCaptor.getAllValues();
        assertEquals(3, savedTransactions.size(), "Devem ser salvas 3 transações");

        // Verifica a primeira parcela
        Transaction firstInstallment = savedTransactions.get(0);
        assertEquals(1, firstInstallment.getInstallmentNumber(), "Primeira parcela deve ter número 1");
        assertEquals(0, new BigDecimal("100.00").compareTo(firstInstallment.getAmount()),
                "Valor da parcela deve ser R$ 100,00");
        assertNotNull(firstInstallment.getPurchaseGroupId(), "purchaseGroupId não deve ser nulo");
        assertEquals(PaymentMethod.CREDIT_CARD, firstInstallment.getPaymentMethod(),
                "Método de pagamento deve ser CREDIT_CARD");
        assertNotNull(firstInstallment.getCreditCardStatement(),
                "creditCardStatement não deve ser nulo");

        // Verifica a segunda parcela
        Transaction secondInstallment = savedTransactions.get(1);
        assertEquals(2, secondInstallment.getInstallmentNumber(), "Segunda parcela deve ter número 2");
        assertEquals(0, new BigDecimal("100.00").compareTo(secondInstallment.getAmount()),
                "Valor da parcela deve ser R$ 100,00");
        assertEquals(firstInstallment.getPurchaseGroupId(), secondInstallment.getPurchaseGroupId(),
                "Deve ter o mesmo purchaseGroupId");

        // Verifica a terceira parcela
        Transaction thirdInstallment = savedTransactions.get(2);
        assertEquals(3, thirdInstallment.getInstallmentNumber(), "Terceira parcela deve ter número 3");
        assertEquals(0, new BigDecimal("100.00").compareTo(thirdInstallment.getAmount()),
                "Valor da parcela deve ser R$ 100,00");
        assertEquals(firstInstallment.getPurchaseGroupId(), thirdInstallment.getPurchaseGroupId(),
                "Deve ter o mesmo purchaseGroupId");

        // Verifica as datas das parcelas (deve incrementar mensalmente)
        assertEquals(LocalDate.of(2025, 11, 10), firstInstallment.getDate(),
                "Data da primeira parcela");
        assertEquals(LocalDate.of(2025, 12, 10), secondInstallment.getDate(),
                "Data da segunda parcela deve ser um mês depois");
        assertEquals(LocalDate.of(2026, 1, 10), thirdInstallment.getDate(),
                "Data da terceira parcela deve ser dois meses depois");

        // Verifica que o serviço de fatura foi chamado para criar as faturas
        verify(statementService, atLeast(1)).save(any(CreditCardStatementDto.class), eq(userId));
    }

    /**
     * Teste 4: Exclusão de Transação sem Grupo de Compra
     * <p>
     * Objetivo: Garantir que ao deletar uma transação que NÃO pertence a um grupo de compra,
     * apenas ela é deletada.
     * <p>
     * Cenário:
     * - Transação NÃO pertence a um grupo de compra (purchaseGroupId é nulo)
     * - Transação é encontrada no repositório
     * - Usuário solicita exclusão da transação
     * <p>
     * Resultado esperado:
     * - Método delete é chamado com a transação específica
     * - Método deleteAll NÃO é chamado
     * - Busca por purchaseGroupId NÃO é realizada
     * <p>
     * Nota: O teste original (shouldDeleteAllInstallmentsWhenDeletingOne) foi substituído
     * porque há uma inconsistência no repositório. O método findByPurchaseGroupId retorna
     * Optional<Transaction> mas o código do serviço trata como se fosse List<Transaction>.
     * Este é um bug que precisa ser corrigido no código de produção.
     */
    @Test
    void shouldDeleteSingleTransactionWhenNoPurchaseGroup() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        Transaction transactionToDelete = new Transaction();
        transactionToDelete.setId(transactionId);
        transactionToDelete.setUser(user);
        transactionToDelete.setPurchaseGroupId(null); // NÃO pertence a um grupo
        transactionToDelete.setInstallmentNumber(null);
        transactionToDelete.setTotalInstallments(1);
        transactionToDelete.setAmount(new BigDecimal("150.00"));
        transactionToDelete.setDate(LocalDate.now());
        transactionToDelete.setDescription("Compra única");
        transactionToDelete.setPaymentMethod(PaymentMethod.PIX);
        transactionToDelete.setType(TransactionType.EXPENSE);

        // Configuração dos mocks
        when(repo.findByIdAndUserId(transactionId, userId)).thenReturn(Optional.of(transactionToDelete));

        // Act
        transactionService.deleteTransaction(transactionId, userId);

        // Assert
        // Verifica que delete foi chamado para esta transação específica
        verify(repo, times(1)).delete(transactionToDelete);

        // Verifica que deleteAll NÃO foi chamado
        verify(repo, never()).deleteAll(anyList());

        // Verifica que a busca por purchaseGroupId NÃO foi realizada
        verify(repo, never()).findByPurchaseGroupId(any());

        // Verifica que a busca pela transação foi realizada
        verify(repo, times(1)).findByIdAndUserId(transactionId, userId);
    }

    /**
     * Teste 5: Exclusão de Todas as Parcelas ao Deletar Uma
     * <p>
     * Objetivo: Garantir que ao deletar uma transação que pertence a um grupo de compra
     * (compra parcelada), TODAS as parcelas do grupo são deletadas em cascata.
     * <p>
     * Cenário:
     * - Transação pertence a um grupo de compra (purchaseGroupId não é nulo)
     * - Existem 3 parcelas no grupo de compra
     * - Usuário solicita exclusão de apenas uma parcela (por exemplo, a 2ª)
     * <p>
     * Resultado esperado:
     * - Método findByPurchaseGroupId é chamado para buscar todas as parcelas
     * - Método deleteAll é chamado com a lista de todas as 3 parcelas
     * - Método delete NÃO é chamado (pois usamos deleteAll)
     * <p>
     * Este teste foi implementado após a correção do bug no TransactionRepository,
     * onde o método findByPurchaseGroupId foi alterado de Optional<Transaction>
     * para List<Transaction>.
     */
    @Test
    void shouldDeleteAllInstallmentsWhenDeletingOne() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID purchaseGroupId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID(); // ID da parcela que será deletada

        User user = new User();
        user.setId(userId);

        // Criar as 3 parcelas que pertencem ao mesmo grupo de compra
        Transaction installment1 = new Transaction();
        installment1.setId(UUID.randomUUID());
        installment1.setUser(user);
        installment1.setPurchaseGroupId(purchaseGroupId);
        installment1.setInstallmentNumber(1);
        installment1.setTotalInstallments(3);
        installment1.setAmount(new BigDecimal("100.00"));
        installment1.setDate(LocalDate.of(2025, 11, 10));
        installment1.setDescription("Compra parcelada 1/3");
        installment1.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        installment1.setType(TransactionType.EXPENSE);

        Transaction installment2 = new Transaction();
        installment2.setId(transactionId); // Esta é a que será deletada
        installment2.setUser(user);
        installment2.setPurchaseGroupId(purchaseGroupId);
        installment2.setInstallmentNumber(2);
        installment2.setTotalInstallments(3);
        installment2.setAmount(new BigDecimal("100.00"));
        installment2.setDate(LocalDate.of(2025, 12, 10));
        installment2.setDescription("Compra parcelada 2/3");
        installment2.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        installment2.setType(TransactionType.EXPENSE);

        Transaction installment3 = new Transaction();
        installment3.setId(UUID.randomUUID());
        installment3.setUser(user);
        installment3.setPurchaseGroupId(purchaseGroupId);
        installment3.setInstallmentNumber(3);
        installment3.setTotalInstallments(3);
        installment3.setAmount(new BigDecimal("100.00"));
        installment3.setDate(LocalDate.of(2026, 1, 10));
        installment3.setDescription("Compra parcelada 3/3");
        installment3.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        installment3.setType(TransactionType.EXPENSE);

        List<Transaction> allInstallments = List.of(installment1, installment2, installment3);

        // Configuração dos mocks
        // Simula que a transação 2 foi encontrada
        when(repo.findByIdAndUserId(transactionId, userId)).thenReturn(Optional.of(installment2));

        // Simula que o repositório retorna todas as 3 parcelas ao buscar pelo purchaseGroupId
        when(repo.findByPurchaseGroupId(purchaseGroupId)).thenReturn(allInstallments);

        // Act
        transactionService.deleteTransaction(transactionId, userId);

        // Assert
        // Verifica que a busca pela transação foi realizada
        verify(repo, times(1)).findByIdAndUserId(transactionId, userId);

        // Verifica que a busca por todas as parcelas do grupo foi realizada
        verify(repo, times(1)).findByPurchaseGroupId(purchaseGroupId);

        // Verifica que deleteAll foi chamado com TODAS as 3 parcelas
        ArgumentCaptor<List<Transaction>> deleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(repo, times(1)).deleteAll(deleteCaptor.capture());

        List<Transaction> deletedTransactions = deleteCaptor.getValue();
        assertEquals(3, deletedTransactions.size(),
                "Devem ser deletadas todas as 3 parcelas do grupo");

        // Verifica que todas as parcelas deletadas pertencem ao mesmo purchaseGroupId
        assertTrue(deletedTransactions.stream()
                .allMatch(tx -> tx.getPurchaseGroupId().equals(purchaseGroupId)),
                "Todas as transações deletadas devem ter o mesmo purchaseGroupId");

        // Verifica que contém as 3 parcelas esperadas
        assertTrue(deletedTransactions.contains(installment1),
                "Deve conter a parcela 1");
        assertTrue(deletedTransactions.contains(installment2),
                "Deve conter a parcela 2 (a que foi solicitada para deletar)");
        assertTrue(deletedTransactions.contains(installment3),
                "Deve conter a parcela 3");

        // Verifica que o método delete simples NÃO foi chamado
        verify(repo, never()).delete(any(Transaction.class));
    }

    /**
     * Teste 6: Buscar Transações Com Período Específico
     * <p>
     * Objetivo: Garantir que o método getTransactions filtra corretamente as transações
     * dentro de um período específico fornecido.
     * <p>
     * Cenário:
     * - Usuário existe
     * - Data inicial: 01/11/2025
     * - Data final: 30/11/2025
     * - Repositório retorna 2 transações dentro do período
     * <p>
     * Resultado esperado:
     * - Método findByUserIdAndDateBetween é chamado com as datas corretas
     * - Retorna lista com 2 transações
     * - Cada transação está dentro do período especificado
     */
    @Test
    void shouldGetTransactionsWithinSpecificPeriod() {
        // Arrange
        UUID userId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2025, 11, 1);
        LocalDate endDate = LocalDate.of(2025, 11, 30);

        User user = new User();
        user.setId(userId);

        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setUser(user);

        TransactionCategory category = new TransactionCategory();
        category.setId(UUID.randomUUID());
        category.setUser(user);

        Transaction transaction1 = new Transaction();
        transaction1.setId(UUID.randomUUID());
        transaction1.setUser(user);
        transaction1.setWallet(wallet);
        transaction1.setCategory(category);
        transaction1.setDescription("Transação no início do período");
        transaction1.setAmount(new BigDecimal("100.00"));
        transaction1.setDate(LocalDate.of(2025, 11, 5));
        transaction1.setType(TransactionType.EXPENSE);
        transaction1.setPaymentMethod(PaymentMethod.PIX);

        Transaction transaction2 = new Transaction();
        transaction2.setId(UUID.randomUUID());
        transaction2.setUser(user);
        transaction2.setWallet(wallet);
        transaction2.setCategory(category);
        transaction2.setDescription("Transação no fim do período");
        transaction2.setAmount(new BigDecimal("200.00"));
        transaction2.setDate(LocalDate.of(2025, 11, 25));
        transaction2.setType(TransactionType.INCOME);
        transaction2.setPaymentMethod(PaymentMethod.DEBIT_CARD);

        List<Transaction> transactions = List.of(transaction1, transaction2);

        when(repo.findByUserIdAndDateBetween(userId, startDate, endDate))
                .thenReturn(transactions);

        // Act
        List<TransactionDto> result = transactionService.getTransactions(userId, startDate, endDate);

        // Assert
        assertNotNull(result, "O resultado não deve ser nulo");
        assertEquals(2, result.size(), "Deve retornar 2 transações");

        assertEquals("Transação no início do período", result.get(0).getDescription(),
                "Primeira transação deve ter a descrição correta");
        assertEquals(0, new BigDecimal("100.00").compareTo(result.get(0).getAmount()),
                "Primeira transação deve ter o valor correto");

        assertEquals("Transação no fim do período", result.get(1).getDescription(),
                "Segunda transação deve ter a descrição correta");
        assertEquals(0, new BigDecimal("200.00").compareTo(result.get(1).getAmount()),
                "Segunda transação deve ter o valor correto");

        verify(repo, times(1)).findByUserIdAndDateBetween(userId, startDate, endDate);
    }

    /**
     * Teste 7: Buscar Transações Sem Período Usa Mês Atual Como Default
     * <p>
     * Objetivo: Garantir que quando as datas são nulas, o serviço aplica
     * automaticamente o filtro para o mês atual.
     * <p>
     * Cenário:
     * - Usuário existe
     * - Data inicial: null
     * - Data final: null
     * - Serviço deve aplicar filtro do mês atual (primeiro dia ao último dia do mês)
     * <p>
     * Resultado esperado:
     * - Método findByUserIdAndDateBetween é chamado com datas do mês atual
     * - Primeira data é o dia 1 do mês atual
     * - Segunda data é o último dia do mês atual
     * - Retorna lista de transações do mês atual
     */
    @Test
    void shouldUseCurrentMonthAsDefaultPeriodWhenDatesAreNull() {
        // Arrange
        UUID userId = UUID.randomUUID();
        LocalDate now = LocalDate.now();
        LocalDate expectedStartDate = now.withDayOfMonth(1);
        LocalDate expectedEndDate = now.withDayOfMonth(now.lengthOfMonth());

        User user = new User();
        user.setId(userId);

        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setUser(user);

        TransactionCategory category = new TransactionCategory();
        category.setId(UUID.randomUUID());
        category.setUser(user);

        Transaction transaction1 = new Transaction();
        transaction1.setId(UUID.randomUUID());
        transaction1.setUser(user);
        transaction1.setWallet(wallet);
        transaction1.setCategory(category);
        transaction1.setDescription("Transação do mês atual");
        transaction1.setAmount(new BigDecimal("150.00"));
        transaction1.setDate(now);
        transaction1.setType(TransactionType.EXPENSE);
        transaction1.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        List<Transaction> transactions = List.of(transaction1);

        when(repo.findByUserIdAndDateBetween(userId, expectedStartDate, expectedEndDate))
                .thenReturn(transactions);

        // Act
        List<TransactionDto> result = transactionService.getTransactions(userId, null, null);

        // Assert
        assertNotNull(result, "O resultado não deve ser nulo");
        assertEquals(1, result.size(), "Deve retornar 1 transação");
        assertEquals("Transação do mês atual", result.get(0).getDescription(),
                "Transação deve ter a descrição correta");

        verify(repo, times(1)).findByUserIdAndDateBetween(
                eq(userId),
                eq(expectedStartDate),
                eq(expectedEndDate)
        );
    }

    /**
     * Teste 8: Buscar Transações Retorna Lista Vazia Quando Não Há Transações no Período
     * <p>
     * Objetivo: Garantir que o serviço retorna lista vazia quando não existem
     * transações dentro do período especificado.
     * <p>
     * Cenário:
     * - Usuário existe
     * - Período específico é fornecido
     * - Repositório não encontra transações no período
     * <p>
     * Resultado esperado:
     * - Retorna lista vazia (não nula)
     * - Método findByUserIdAndDateBetween é chamado
     */
    @Test
    void shouldReturnEmptyListWhenNoTransactionsFoundInPeriod() {
        // Arrange
        UUID userId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2025, 12, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 31);

        when(repo.findByUserIdAndDateBetween(userId, startDate, endDate))
                .thenReturn(List.of());

        // Act
        List<TransactionDto> result = transactionService.getTransactions(userId, startDate, endDate);

        // Assert
        assertNotNull(result, "O resultado não deve ser nulo");
        assertTrue(result.isEmpty(), "A lista deve estar vazia");

        verify(repo, times(1)).findByUserIdAndDateBetween(userId, startDate, endDate);
    }

    /**
     * Teste 9: Buscar Transações Exclui Transações Fora do Período
     * <p>
     * Objetivo: Garantir que apenas transações dentro do período são retornadas,
     * e transações fora do período são excluídas pelo filtro do repositório.
     * <p>
     * Cenário:
     * - Período: 01/11/2025 a 30/11/2025
     * - Transação dentro do período: 15/11/2025
     * - Transação fora do período não é retornada pelo repositório
     * <p>
     * Resultado esperado:
     * - Apenas transações dentro do período são retornadas
     * - Filtro é aplicado corretamente no nível do repositório
     */
    @Test
    void shouldExcludeTransactionsOutsideOfPeriod() {
        // Arrange
        UUID userId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2025, 11, 1);
        LocalDate endDate = LocalDate.of(2025, 11, 30);

        User user = new User();
        user.setId(userId);

        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setUser(user);

        TransactionCategory category = new TransactionCategory();
        category.setId(UUID.randomUUID());
        category.setUser(user);

        // Apenas transação dentro do período (repositório já filtra)
        Transaction transactionInPeriod = new Transaction();
        transactionInPeriod.setId(UUID.randomUUID());
        transactionInPeriod.setUser(user);
        transactionInPeriod.setWallet(wallet);
        transactionInPeriod.setCategory(category);
        transactionInPeriod.setDescription("Dentro do período");
        transactionInPeriod.setAmount(new BigDecimal("100.00"));
        transactionInPeriod.setDate(LocalDate.of(2025, 11, 15));
        transactionInPeriod.setType(TransactionType.EXPENSE);
        transactionInPeriod.setPaymentMethod(PaymentMethod.PIX);

        List<Transaction> transactions = List.of(transactionInPeriod);

        when(repo.findByUserIdAndDateBetween(userId, startDate, endDate))
                .thenReturn(transactions);

        // Act
        List<TransactionDto> result = transactionService.getTransactions(userId, startDate, endDate);

        // Assert
        assertNotNull(result, "O resultado não deve ser nulo");
        assertEquals(1, result.size(), "Deve retornar apenas 1 transação");
        assertEquals("Dentro do período", result.get(0).getDescription(),
                "Deve retornar apenas a transação dentro do período");

        // Verifica que a data está dentro do período
        LocalDate resultDate = result.get(0).getDate();
        assertTrue(
                (resultDate.isEqual(startDate) || resultDate.isAfter(startDate)) &&
                        (resultDate.isEqual(endDate) || resultDate.isBefore(endDate)),
                "A data da transação deve estar dentro do período"
        );

        verify(repo, times(1)).findByUserIdAndDateBetween(userId, startDate, endDate);
    }
}
