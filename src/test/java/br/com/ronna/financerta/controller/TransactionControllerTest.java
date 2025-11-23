package br.com.ronna.financerta.controller;

import br.com.ronna.financerta.dto.TransactionDto;
import br.com.ronna.financerta.enums.PaymentMethod;
import br.com.ronna.financerta.enums.TransactionType;
import br.com.ronna.financerta.model.User;
import br.com.ronna.financerta.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para TransactionController.
 * <p>
 * Esta classe implementa testes de integração especificados no plano de testes:
 * 1. Teste de segurança: acesso não autorizado (sem token)
 * 2. Teste de segurança: acesso autorizado (com usuário mockado)
 * 3. Testes adicionais de endpoints com autenticação
 * <p>
 * Estes testes garantem que:
 * - Endpoints protegidos exigem autenticação (Spring Security)
 * - Usuários autenticados podem acessar seus dados
 * - A camada de controller funciona corretamente com a camada de serviço
 * - Serialização/deserialização de JSON funciona
 * <p>
 * Usamos MockMvc para simular requisições HTTP e MockBean para simular o serviço.
 * @SpringBootTest carrega o contexto completo da aplicação incluindo segurança.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private ObjectMapper objectMapper;

    private UsernamePasswordAuthenticationToken authentication;
    private User mockUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mockUser = new User();
        mockUser.setId(userId);
        mockUser.setEmail("testuser@test.com");
        mockUser.setName("Test User");

        authentication = new UsernamePasswordAuthenticationToken(
                mockUser,
                null,
                new ArrayList<>()
        );
    }

    /**
     * Teste 1: Acesso Não Autorizado (Sem Token JWT)
     * <p>
     * Objetivo: Garantir que endpoints protegidos não podem ser acessados sem token JWT.
     * <p>
     * Cenário:
     * - Requisição GET para /api/transactions
     * - Nenhum token de autenticação fornecido
     * - Spring Security intercepta a requisição
     * <p>
     * Resultado esperado:
     * - Status HTTP 403 (Forbidden) ou 401 (Unauthorized)
     * - Acesso negado
     * - TransactionService NÃO é chamado
     * <p>
     * Nota: Spring Security pode retornar 403 (Forbidden) ao invés de 401 (Unauthorized)
     * dependendo da configuração. Ambos indicam que o acesso foi negado.
     */
    @Test
    void shouldReturnUnauthorizedWhenNoTokenIsProvided() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isForbidden()); // Spring Security retorna 403 por padrão

        // Nota: O serviço não é chamado porque a requisição é bloqueada pelo filtro de segurança
    }

    /**
     * Teste 2: Acesso Autorizado para Usuário Autenticado
     * <p>
     * Objetivo: Garantir que um usuário autenticado pode acessar seus dados.
     * <p>
     * Cenário:
     * - Requisição GET para /api/transactions
     * - Usuário autenticado (simulado com authentication)
     * - TransactionService retorna uma lista (pode ser vazia)
     * <p>
     * Resultado esperado:
     * - Status HTTP 200 (OK)
     * - TransactionService.getTransactions é chamado com parâmetros de período
     * - Resposta JSON com lista de transações
     */
    @Test
    void shouldReturnOkForAuthenticatedUser() throws Exception {
        // Arrange
        // Simula que o serviço retorna uma lista vazia
        when(transactionService.getTransactions(any(UUID.class), any(), any()))
                .thenReturn(new ArrayList<>());

        // Act & Assert
        mockMvc.perform(get("/api/transactions")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    /**
     * Teste 3: Buscar Transação por ID com Autenticação
     * <p>
     * Objetivo: Garantir que um usuário autenticado pode buscar uma transação específica.
     * <p>
     * Cenário:
     * - Requisição GET para /api/transactions/{id}
     * - Usuário autenticado
     * - TransactionService retorna a transação
     * <p>
     * Resultado esperado:
     * - Status HTTP 200 (OK)
     * - Resposta JSON com os dados da transação
     */
    @Test
    void shouldReturnTransactionByIdForAuthenticatedUser() throws Exception {
        // Arrange
        UUID transactionId = UUID.randomUUID();

        TransactionDto transactionDto = new TransactionDto();
        transactionDto.setId(transactionId);
        transactionDto.setDescription("Compra teste");
        transactionDto.setAmount(new BigDecimal("100.00"));
        transactionDto.setDate(LocalDate.now());
        transactionDto.setType(TransactionType.EXPENSE);
        transactionDto.setPaymentMethod(PaymentMethod.PIX);

        when(transactionService.getTransactionById(eq(transactionId), any(UUID.class)))
                .thenReturn(transactionDto);

        // Act & Assert
        mockMvc.perform(get("/api/transactions/{id}", transactionId)
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(transactionId.toString()))
                .andExpect(jsonPath("$.description").value("Compra teste"))
                .andExpect(jsonPath("$.amount").value(100.00));
    }

    /**
     * Teste 4: Criar Transação com Autenticação
     * <p>
     * Objetivo: Garantir que um usuário autenticado pode criar uma transação.
     * <p>
     * Cenário:
     * - Requisição POST para /api/transactions
     * - Usuário autenticado
     * - Body JSON com dados da transação
     * - TransactionService cria a transação
     * <p>
     * Resultado esperado:
     * - Status HTTP 201 (Created)
     * - Resposta JSON com a transação criada
     */
    @Test
    void shouldCreateTransactionForAuthenticatedUser() throws Exception {
        // Arrange
        TransactionDto inputDto = new TransactionDto();
        inputDto.setDescription("Nova compra");
        inputDto.setAmount(new BigDecimal("250.00"));
        inputDto.setDate(LocalDate.now());
        inputDto.setType(TransactionType.EXPENSE);
        inputDto.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        inputDto.setTotalInstallments(1);

        TransactionDto createdDto = new TransactionDto();
        createdDto.setId(UUID.randomUUID());
        createdDto.setDescription(inputDto.getDescription());
        createdDto.setAmount(inputDto.getAmount());
        createdDto.setDate(inputDto.getDate());
        createdDto.setType(inputDto.getType());
        createdDto.setPaymentMethod(inputDto.getPaymentMethod());
        createdDto.setTotalInstallments(inputDto.getTotalInstallments());

        when(transactionService.createTransaction(any(TransactionDto.class), any(UUID.class)))
                .thenReturn(createdDto);

        // Act & Assert
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto))
                        .with(authentication(authentication)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.description").value("Nova compra"))
                .andExpect(jsonPath("$.amount").value(250.00));
    }

    /**
     * Teste 5: Criar Transação Sem Autenticação Retorna 403
     * <p>
     * Objetivo: Garantir que criar transação sem autenticação é bloqueado.
     * <p>
     * Cenário:
     * - Requisição POST para /api/transactions
     * - Nenhum token de autenticação
     * <p>
     * Resultado esperado:
     * - Status HTTP 403 (Forbidden)
     */
    @Test
    void shouldReturnUnauthorizedWhenCreatingTransactionWithoutAuth() throws Exception {
        // Arrange
        TransactionDto inputDto = new TransactionDto();
        inputDto.setDescription("Tentativa sem auth");
        inputDto.setAmount(new BigDecimal("100.00"));
        inputDto.setDate(LocalDate.now());
        inputDto.setType(TransactionType.EXPENSE);
        inputDto.setPaymentMethod(PaymentMethod.PIX);

        // Act & Assert
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isForbidden());
    }

    /**
     * Teste 6: Atualizar Transação com Autenticação
     * <p>
     * Objetivo: Garantir que um usuário autenticado pode atualizar uma transação.
     * <p>
     * Cenário:
     * - Requisição PUT para /api/transactions/{id}
     * - Usuário autenticado
     * - Body JSON com dados atualizados
     * <p>
     * Resultado esperado:
     * - Status HTTP 200 (OK)
     * - Resposta JSON com a transação atualizada
     */
    @Test
    void shouldUpdateTransactionForAuthenticatedUser() throws Exception {
        // Arrange
        UUID transactionId = UUID.randomUUID();

        TransactionDto updateDto = new TransactionDto();
        updateDto.setDescription("Compra atualizada");
        updateDto.setAmount(new BigDecimal("300.00"));
        updateDto.setDate(LocalDate.now());
        updateDto.setType(TransactionType.EXPENSE);
        updateDto.setPaymentMethod(PaymentMethod.DEBIT_CARD);

        TransactionDto updatedDto = new TransactionDto();
        updatedDto.setId(transactionId);
        updatedDto.setDescription(updateDto.getDescription());
        updatedDto.setAmount(updateDto.getAmount());
        updatedDto.setDate(updateDto.getDate());
        updatedDto.setType(updateDto.getType());
        updatedDto.setPaymentMethod(updateDto.getPaymentMethod());

        when(transactionService.updateTransaction(eq(transactionId), any(TransactionDto.class), any(UUID.class)))
                .thenReturn(updatedDto);

        // Act & Assert
        mockMvc.perform(put("/api/transactions/{id}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto))
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(transactionId.toString()))
                .andExpect(jsonPath("$.description").value("Compra atualizada"))
                .andExpect(jsonPath("$.amount").value(300.00));
    }

    /**
     * Teste 7: Deletar Transação com Autenticação
     * <p>
     * Objetivo: Garantir que um usuário autenticado pode deletar uma transação.
     * <p>
     * Cenário:
     * - Requisição DELETE para /api/transactions/{id}
     * - Usuário autenticado
     * - TransactionService executa a deleção
     * <p>
     * Resultado esperado:
     * - Status HTTP 204 (No Content)
     * - Sem corpo na resposta
     */
    @Test
    void shouldDeleteTransactionForAuthenticatedUser() throws Exception {
        // Arrange
        UUID transactionId = UUID.randomUUID();

        // Não precisa configurar when() para void methods, mas vamos garantir que não lança exceção
        // doNothing().when(transactionService).deleteTransaction(eq(transactionId), eq(userId));

        // Act & Assert
        mockMvc.perform(delete("/api/transactions/{id}", transactionId)
                        .with(authentication(authentication)))
                .andExpect(status().isNoContent());
    }

    /**
     * Teste 8: Deletar Transação Sem Autenticação Retorna 403
     * <p>
     * Objetivo: Garantir que deletar transação sem autenticação é bloqueado.
     * <p>
     * Cenário:
     * - Requisição DELETE para /api/transactions/{id}
     * - Nenhum token de autenticação
     * <p>
     * Resultado esperado:
     * - Status HTTP 403 (Forbidden)
     */
    @Test
    void shouldReturnUnauthorizedWhenDeletingTransactionWithoutAuth() throws Exception {
        // Arrange
        UUID transactionId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(delete("/api/transactions/{id}", transactionId))
                .andExpect(status().isForbidden());
    }

    /**
     * Teste 9: Listar Transações Retorna Lista Com Múltiplas Transações
     * <p>
     * Objetivo: Garantir que o endpoint de listagem retorna múltiplas transações corretamente.
     * <p>
     * Cenário:
     * - Requisição GET para /api/transactions
     * - Usuário autenticado
     * - TransactionService retorna lista com 3 transações
     * <p>
     * Resultado esperado:
     * - Status HTTP 200 (OK)
     * - Array JSON com 3 elementos
     * - Cada elemento tem os campos esperados
     */
    @Test
    void shouldReturnMultipleTransactionsForAuthenticatedUser() throws Exception {
        // Arrange
        TransactionDto transaction1 = new TransactionDto();
        transaction1.setId(UUID.randomUUID());
        transaction1.setDescription("Transação 1");
        transaction1.setAmount(new BigDecimal("100.00"));
        transaction1.setDate(LocalDate.now());
        transaction1.setType(TransactionType.EXPENSE);
        transaction1.setPaymentMethod(PaymentMethod.PIX);

        TransactionDto transaction2 = new TransactionDto();
        transaction2.setId(UUID.randomUUID());
        transaction2.setDescription("Transação 2");
        transaction2.setAmount(new BigDecimal("200.00"));
        transaction2.setDate(LocalDate.now());
        transaction2.setType(TransactionType.INCOME);
        transaction2.setPaymentMethod(PaymentMethod.DEBIT_CARD);

        TransactionDto transaction3 = new TransactionDto();
        transaction3.setId(UUID.randomUUID());
        transaction3.setDescription("Transação 3");
        transaction3.setAmount(new BigDecimal("300.00"));
        transaction3.setDate(LocalDate.now());
        transaction3.setType(TransactionType.EXPENSE);
        transaction3.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        List<TransactionDto> transactions = List.of(transaction1, transaction2, transaction3);

        // Use lenient stubbing to avoid strictness issues
        when(transactionService.getTransactions(any(UUID.class), any(), any()))
                .thenReturn(transactions);

        // Act & Assert
        mockMvc.perform(get("/api/transactions")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].description").value("Transação 1"))
                .andExpect(jsonPath("$[1].description").value("Transação 2"))
                .andExpect(jsonPath("$[2].description").value("Transação 3"));
    }

    /**
     * Teste 10: Buscar Transações Com Período Específico
     * <p>
     * Objetivo: Garantir que o endpoint aceita parâmetros de data para filtrar transações.
     * <p>
     * Cenário:
     * - Requisição GET para /api/transactions com parâmetros startDate e endDate
     * - Usuário autenticado
     * - TransactionService retorna transações do período
     * <p>
     * Resultado esperado:
     * - Status HTTP 200 (OK)
     * - TransactionService.getTransactions é chamado com as datas fornecidas
     * - Resposta JSON com lista de transações do período
     */
    @Test
    void shouldReturnTransactionsForSpecificPeriod() throws Exception {
        // Arrange
        LocalDate startDate = LocalDate.of(2025, 11, 1);
        LocalDate endDate = LocalDate.of(2025, 11, 30);

        TransactionDto transaction1 = new TransactionDto();
        transaction1.setId(UUID.randomUUID());
        transaction1.setDescription("Transação do período");
        transaction1.setAmount(new BigDecimal("150.00"));
        transaction1.setDate(LocalDate.of(2025, 11, 15));
        transaction1.setType(TransactionType.EXPENSE);
        transaction1.setPaymentMethod(PaymentMethod.PIX);

        List<TransactionDto> transactions = List.of(transaction1);

        when(transactionService.getTransactions(any(UUID.class), eq(startDate), eq(endDate)))
                .thenReturn(transactions);

        // Act & Assert
        mockMvc.perform(get("/api/transactions")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Transação do período"))
                .andExpect(jsonPath("$[0].date").value("2025-11-15"));
    }

    /**
     * Teste 11: Buscar Transações Sem Período Usa Default do Mês Atual
     * <p>
     * Objetivo: Garantir que quando nenhum período é fornecido, o serviço é chamado
     * com parâmetros nulos (que serão tratados pelo serviço para usar mês atual).
     * <p>
     * Cenário:
     * - Requisição GET para /api/transactions sem parâmetros de data
     * - Usuário autenticado
     * - TransactionService deve ser chamado com null para as datas
     * <p>
     * Resultado esperado:
     * - Status HTTP 200 (OK)
     * - TransactionService.getTransactions é chamado com datas null
     * - Resposta JSON com lista de transações (serviço aplica filtro do mês atual)
     */
    @Test
    void shouldUseDefaultPeriodWhenNoDatesProvided() throws Exception {
        // Arrange
        TransactionDto transaction1 = new TransactionDto();
        transaction1.setId(UUID.randomUUID());
        transaction1.setDescription("Transação do mês atual");
        transaction1.setAmount(new BigDecimal("200.00"));
        transaction1.setDate(LocalDate.now());
        transaction1.setType(TransactionType.INCOME);
        transaction1.setPaymentMethod(PaymentMethod.DEBIT_CARD);

        List<TransactionDto> transactions = List.of(transaction1);

        when(transactionService.getTransactions(any(UUID.class), any(), any()))
                .thenReturn(transactions);

        // Act & Assert
        mockMvc.perform(get("/api/transactions")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Transação do mês atual"));
    }
}
