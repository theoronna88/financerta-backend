package br.com.ronna.financerta.service.impl;

import br.com.ronna.financerta.dto.CreditCardStatementDto;
import br.com.ronna.financerta.dto.TransactionDto;
import br.com.ronna.financerta.enums.PaymentMethod;
import br.com.ronna.financerta.enums.TransactionType;
import br.com.ronna.financerta.exception.TransactionException;
import br.com.ronna.financerta.model.CreditCard;
import br.com.ronna.financerta.model.CreditCardStatement;
import br.com.ronna.financerta.model.Transaction;
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

        // Simula o retorno 'true' para as validações dentro de isValidTransactionDto
        when(userRepo.existsById(userId)).thenReturn(true);
        when(walletRepo.existsByIdAndUserId(walletId, userId)).thenReturn(true);
        when(categoryRepo.existsByIdAndUserId(categoryId, userId)).thenReturn(true);

        // Quando o repo.save for chamado, retorne uma entidade Transaction mockada
        Transaction savedTransaction = new Transaction();
        savedTransaction.setId(UUID.randomUUID());
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

        when(userRepo.existsById(userId)).thenReturn(true);
        when(walletRepo.existsByIdAndUserId(walletId, userId)).thenReturn(false); // Simula que a carteira não existe

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

        CreditCard mockCreditCard = new CreditCard();
        mockCreditCard.setId(creditCardId);
        mockCreditCard.setUserId(userId);
        mockCreditCard.setClosingDay(15); // Dia de fechamento da fatura
        mockCreditCard.setDueDay(25);
        mockCreditCard.setName("Cartão Teste");
        mockCreditCard.setLimitValue(new BigDecimal("5000.00"));

        // Mockar todas as validações para retornarem true
        when(userRepo.existsById(userId)).thenReturn(true);
        when(walletRepo.existsByIdAndUserId(walletId, userId)).thenReturn(true);
        when(categoryRepo.existsByIdAndUserId(categoryId, userId)).thenReturn(true);
        when(creditCardRepo.findByIdAndUserId(creditCardId, userId)).thenReturn(Optional.of(mockCreditCard));

        // Simula que não há faturas existentes, forçando a criação de novas
        when(statementRepo.findByCreditCardIdAndMonthAndYear(eq(creditCardId), anyInt(), anyInt()))
                .thenReturn(Optional.empty());

        // Simula a criação de uma nova fatura pelo statementService
        CreditCardStatementDto newStatementDto = new CreditCardStatementDto();
        newStatementDto.setId(UUID.randomUUID());
        newStatementDto.setCreditCardId(creditCardId);
        when(statementService.save(any(CreditCardStatementDto.class), eq(userId))).thenReturn(newStatementDto);

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
        assertNotNull(firstInstallment.getCreditCardStatementId(),
                "creditCardStatementId não deve ser nulo");

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

        Transaction transactionToDelete = new Transaction();
        transactionToDelete.setId(transactionId);
        transactionToDelete.setUserId(userId);
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

        // Criar as 3 parcelas que pertencem ao mesmo grupo de compra
        Transaction installment1 = new Transaction();
        installment1.setId(UUID.randomUUID());
        installment1.setUserId(userId);
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
        installment2.setUserId(userId);
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
        installment3.setUserId(userId);
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
}
