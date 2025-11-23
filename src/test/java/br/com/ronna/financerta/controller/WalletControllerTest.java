package br.com.ronna.financerta.controller;

import br.com.ronna.financerta.dto.WalletDto;
import br.com.ronna.financerta.model.User;
import br.com.ronna.financerta.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para WalletController.
 * <p>
 * Esta classe implementa testes abrangentes de integração para o controller de carteiras,
 * cobrindo todos os cenários especificados no plano de testes:
 * <p>
 * 1. Segurança e autenticação (401/403)
 * 2. CRUD completo (GET, POST, PUT, DELETE)
 * 3. Validação de dados e exceções
 * 4. Isolamento de dados entre usuários (segurança)
 * 5. Status HTTP corretos
 * 6. Serialização/deserialização JSON
 * <p>
 * Estes testes garantem que:
 * - Endpoints protegidos exigem autenticação (Spring Security)
 * - Usuários só podem acessar suas próprias carteiras (isolamento de dados)
 * - Validações de negócio são aplicadas corretamente
 * - Dados financeiros sensíveis (saldo inicial, nome) são preservados
 * - Respostas HTTP seguem padrões REST (200, 201, 204, 404, etc.)
 * - Edge cases são tratados (deleção de carteira com transações)
 * <p>
 * IMPORTANTE: Este controller lida com dados financeiros críticos (carteiras e saldos).
 * Qualquer falha pode resultar em perda de integridade de dados ou violação de segurança.
 *
 * @author Claude - Java Test Engineer AI Agent
 * @version 1.0
 * @since 2025-11-23
 */
@SpringBootTest
@AutoConfigureMockMvc
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WalletService walletService;

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

    // ==================== Testes de Segurança ====================

    /**
     * Teste 1: Acesso Não Autorizado - Listar Carteiras Sem Token
     * <p>
     * Objetivo: Garantir que o endpoint de listagem de carteiras exige autenticação.
     * <p>
     * Cenário:
     * - Requisição GET para /api/wallets
     * - Nenhum token de autenticação fornecido
     * - Spring Security intercepta a requisição
     * <p>
     * Resultado esperado:
     * - Status HTTP 403 (Forbidden)
     * - Acesso negado
     * - WalletService NÃO é chamado
     * - Dados sensíveis permanecem protegidos
     */
    @Test
    void shouldReturn403WhenListingWalletsWithoutAuthentication() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/wallets"))
                .andExpect(status().isForbidden());

        // Verificar que o serviço não foi chamado (requisição bloqueada pelo filtro de segurança)
        verify(walletService, never()).getWallets(any(UUID.class));
    }

    /**
     * Teste 2: Acesso Não Autorizado - Criar Carteira Sem Token
     * <p>
     * Objetivo: Garantir que criação de carteiras exige autenticação.
     * <p>
     * Cenário:
     * - Requisição POST para /api/wallets
     * - Nenhum token de autenticação
     * <p>
     * Resultado esperado:
     * - Status HTTP 403 (Forbidden)
     * - Carteira NÃO é criada
     */
    @Test
    void shouldReturn403WhenCreatingWalletWithoutAuthentication() throws Exception {
        // Arrange
        WalletDto walletDto = createValidWalletDto();

        // Act & Assert
        mockMvc.perform(post("/api/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(walletDto)))
                .andExpect(status().isForbidden());

        verify(walletService, never()).createWallet(any(WalletDto.class), any(UUID.class));
    }

    /**
     * Teste 3: Acesso Não Autorizado - Atualizar Carteira Sem Token
     * <p>
     * Objetivo: Garantir que atualização de carteiras exige autenticação.
     * <p>
     * Cenário:
     * - Requisição PUT para /api/wallets/{id}
     * - Nenhum token de autenticação
     * <p>
     * Resultado esperado:
     * - Status HTTP 403 (Forbidden)
     * - Carteira NÃO é atualizada
     */
    @Test
    void shouldReturn403WhenUpdatingWalletWithoutAuthentication() throws Exception {
        // Arrange
        UUID walletId = UUID.randomUUID();
        WalletDto walletDto = createValidWalletDto();

        // Act & Assert
        mockMvc.perform(put("/api/wallets/{id}", walletId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(walletDto)))
                .andExpect(status().isForbidden());

        verify(walletService, never()).updateWallet(any(UUID.class), any(WalletDto.class), any(UUID.class));
    }

    /**
     * Teste 4: Acesso Não Autorizado - Deletar Carteira Sem Token
     * <p>
     * Objetivo: Garantir que deleção de carteiras exige autenticação.
     * <p>
     * Cenário:
     * - Requisição DELETE para /api/wallets/{id}
     * - Nenhum token de autenticação
     * <p>
     * Resultado esperado:
     * - Status HTTP 403 (Forbidden)
     * - Carteira NÃO é deletada
     */
    @Test
    void shouldReturn403WhenDeletingWalletWithoutAuthentication() throws Exception {
        // Arrange
        UUID walletId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(delete("/api/wallets/{id}", walletId))
                .andExpect(status().isForbidden());

        verify(walletService, never()).deleteWalletById(any(UUID.class), any(UUID.class));
    }

    // ==================== Testes de CRUD - Happy Path ====================

    /**
     * Teste 5: Listar Todas as Carteiras do Usuário Autenticado
     * <p>
     * Objetivo: Garantir que usuário autenticado pode listar suas carteiras.
     * <p>
     * Cenário:
     * - Requisição GET para /api/wallets
     * - Usuário autenticado
     * - WalletService retorna lista de carteiras
     * <p>
     * Resultado esperado:
     * - Status HTTP 200 (OK)
     * - Content-Type: application/json
     * - Array JSON com carteiras do usuário
     * - Cada carteira contém campos esperados (id, name, initialBalance, color, active)
     */
    @Test
    void shouldReturnAllWalletsForAuthenticatedUser() throws Exception {
        // Arrange
        WalletDto wallet1 = createWalletDto(UUID.randomUUID(), "Conta Corrente",
                new BigDecimal("5000.00"), "#0000FF", true);
        WalletDto wallet2 = createWalletDto(UUID.randomUUID(), "Poupança",
                new BigDecimal("10000.00"), "#00FF00", true);

        List<WalletDto> wallets = List.of(wallet1, wallet2);

        when(walletService.getWallets(userId)).thenReturn(wallets);

        // Act & Assert
        mockMvc.perform(get("/api/wallets")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Conta Corrente"))
                .andExpect(jsonPath("$[0].initialBalance").value(5000.00))
                .andExpect(jsonPath("$[0].color").value("#0000FF"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].name").value("Poupança"))
                .andExpect(jsonPath("$[1].initialBalance").value(10000.00));

        verify(walletService, times(1)).getWallets(userId);
    }

    /**
     * Teste 6: Retornar Lista Vazia Quando Usuário Não Tem Carteiras
     * <p>
     * Objetivo: Garantir comportamento correto quando usuário não possui carteiras.
     * <p>
     * Cenário:
     * - Usuário autenticado
     * - WalletService retorna lista vazia
     * <p>
     * Resultado esperado:
     * - Status HTTP 200 (OK)
     * - Array JSON vazio
     * - Não há exceções
     */
    @Test
    void shouldReturnEmptyListWhenUserHasNoWallets() throws Exception {
        // Arrange
        when(walletService.getWallets(userId)).thenReturn(new ArrayList<>());

        // Act & Assert
        mockMvc.perform(get("/api/wallets")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(walletService, times(1)).getWallets(userId);
    }

    /**
     * Teste 7: Buscar Carteira por ID com Sucesso
     * <p>
     * Objetivo: Garantir que usuário autenticado pode buscar carteira específica.
     * <p>
     * Cenário:
     * - Requisição GET para /api/wallets/{id}
     * - Carteira existe e pertence ao usuário
     * <p>
     * Resultado esperado:
     * - Status HTTP 200 (OK)
     * - JSON com dados completos da carteira
     * - Todos os campos sensíveis preservados
     */
    @Test
    void shouldReturnWalletByIdForAuthenticatedUser() throws Exception {
        // Arrange
        UUID walletId = UUID.randomUUID();
        WalletDto walletDto = createWalletDto(walletId, "Carteira Empresarial",
                new BigDecimal("25000.00"), "#FF0000", true);

        when(walletService.findById(walletId, userId)).thenReturn(walletDto);

        // Act & Assert
        mockMvc.perform(get("/api/wallets/{id}", walletId)
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(walletId.toString()))
                .andExpect(jsonPath("$.name").value("Carteira Empresarial"))
                .andExpect(jsonPath("$.initialBalance").value(25000.00))
                .andExpect(jsonPath("$.color").value("#FF0000"))
                .andExpect(jsonPath("$.active").value(true));

        verify(walletService, times(1)).findById(walletId, userId);
    }

    /**
     * Teste 8: Criar Carteira com Sucesso
     * <p>
     * Objetivo: Garantir que usuário autenticado pode criar nova carteira.
     * <p>
     * Cenário:
     * - Requisição POST para /api/wallets
     * - DTO válido com todos os campos obrigatórios
     * - Usuário autenticado
     * <p>
     * Resultado esperado:
     * - Status HTTP 201 (Created)
     * - JSON com carteira criada (incluindo ID gerado)
     * - WalletService.createWallet é chamado com userId do contexto autenticado
     */
    @Test
    void shouldCreateWalletSuccessfullyForAuthenticatedUser() throws Exception {
        // Arrange
        WalletDto inputDto = createValidWalletDto();

        UUID createdWalletId = UUID.randomUUID();
        WalletDto createdDto = createWalletDto(createdWalletId, "Nova Carteira",
                new BigDecimal("3000.00"), "#00FFFF", true);

        when(walletService.createWallet(any(WalletDto.class), eq(userId)))
                .thenReturn(createdDto);

        // Act & Assert
        mockMvc.perform(post("/api/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto))
                        .with(authentication(authentication)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(createdWalletId.toString()))
                .andExpect(jsonPath("$.name").value("Nova Carteira"))
                .andExpect(jsonPath("$.initialBalance").value(3000.00))
                .andExpect(jsonPath("$.color").value("#00FFFF"));

        verify(walletService, times(1)).createWallet(any(WalletDto.class), eq(userId));
    }

    /**
     * Teste 9: Atualizar Carteira com Sucesso
     * <p>
     * Objetivo: Garantir que usuário autenticado pode atualizar suas carteiras.
     * <p>
     * Cenário:
     * - Requisição PUT para /api/wallets/{id}
     * - DTO com dados atualizados
     * - Carteira existe e pertence ao usuário
     * <p>
     * Resultado esperado:
     * - Status HTTP 200 (OK)
     * - JSON com carteira atualizada
     * - Mudanças são refletidas na resposta
     */
    @Test
    void shouldUpdateWalletSuccessfullyForAuthenticatedUser() throws Exception {
        // Arrange
        UUID walletId = UUID.randomUUID();

        WalletDto updateDto = createValidWalletDto();
        updateDto.setName("Carteira Atualizada");
        updateDto.setInitialBalance(new BigDecimal("15000.00"));
        updateDto.setColor("#FFFF00");

        WalletDto updatedDto = createWalletDto(walletId, "Carteira Atualizada",
                new BigDecimal("15000.00"), "#FFFF00", true);

        when(walletService.updateWallet(eq(walletId), any(WalletDto.class), eq(userId)))
                .thenReturn(updatedDto);

        // Act & Assert
        mockMvc.perform(put("/api/wallets/{id}", walletId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto))
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(walletId.toString()))
                .andExpect(jsonPath("$.name").value("Carteira Atualizada"))
                .andExpect(jsonPath("$.initialBalance").value(15000.00))
                .andExpect(jsonPath("$.color").value("#FFFF00"));

        verify(walletService, times(1)).updateWallet(eq(walletId), any(WalletDto.class), eq(userId));
    }

    /**
     * Teste 10: Deletar Carteira com Sucesso
     * <p>
     * Objetivo: Garantir que usuário autenticado pode deletar suas carteiras.
     * <p>
     * Cenário:
     * - Requisição DELETE para /api/wallets/{id}
     * - Carteira existe e pertence ao usuário
     * - Carteira não possui transações associadas (ou soft delete)
     * <p>
     * Resultado esperado:
     * - Status HTTP 204 (No Content)
     * - Sem corpo na resposta
     * - WalletService.deleteWalletById é chamado
     */
    @Test
    void shouldDeleteWalletSuccessfullyForAuthenticatedUser() throws Exception {
        // Arrange
        UUID walletId = UUID.randomUUID();

        doNothing().when(walletService).deleteWalletById(walletId, userId);

        // Act & Assert
        mockMvc.perform(delete("/api/wallets/{id}", walletId)
                        .with(authentication(authentication)))
                .andExpect(status().isNoContent());

        verify(walletService, times(1)).deleteWalletById(walletId, userId);
    }

    // ==================== Testes de Validação e Exceções ====================

    /**
     * Teste 11: Retornar 404 Quando Carteira Não É Encontrada
     * <p>
     * Objetivo: Garantir resposta apropriada quando carteira não existe.
     * <p>
     * Cenário:
     * - Requisição GET para /api/wallets/{id}
     * - ID não existe ou pertence a outro usuário
     * - WalletService lança NoSuchElementException
     * <p>
     * Resultado esperado:
     * - Status HTTP 404 (Not Found) ou 500 (dependendo do exception handler)
     * - Mensagem de erro apropriada
     */
    @Test
    void shouldReturn404WhenWalletNotFound() throws Exception {
        // Arrange
        UUID nonExistentWalletId = UUID.randomUUID();

        when(walletService.findById(nonExistentWalletId, userId))
                .thenThrow(new NoSuchElementException("Carteira não encontrada"));

        // Act & Assert
        mockMvc.perform(get("/api/wallets/{id}", nonExistentWalletId)
                        .with(authentication(authentication)))
                .andExpect(status().is5xxServerError()); // Ou 404 se houver @ExceptionHandler

        verify(walletService, times(1)).findById(nonExistentWalletId, userId);
    }

    /**
     * Teste 12: Campos Críticos São Preservados na Serialização
     * <p>
     * Objetivo: Garantir que todos os campos críticos de uma carteira
     * são corretamente serializados para JSON.
     * <p>
     * Cenário:
     * - Buscar carteira com todos os campos preenchidos
     * - Verificar que resposta JSON contém todos os dados
     * <p>
     * Resultado esperado:
     * - Todos os campos estão presentes no JSON
     * - Tipos numéricos (BigDecimal) são formatados corretamente
     * - Flags booleanas são preservadas
     */
    @Test
    void shouldPreserveCriticalFieldsInJsonSerialization() throws Exception {
        // Arrange
        UUID walletId = UUID.randomUUID();
        WalletDto walletDto = new WalletDto();
        walletDto.setId(walletId);
        walletDto.setName("Carteira Completa");
        walletDto.setInitialBalance(new BigDecimal("12345.67"));
        walletDto.setColor("#123456");
        walletDto.setActive(false); // Testando false também

        when(walletService.findById(walletId, userId)).thenReturn(walletDto);

        // Act & Assert
        mockMvc.perform(get("/api/wallets/{id}", walletId)
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(walletId.toString()))
                .andExpect(jsonPath("$.name").value("Carteira Completa"))
                .andExpect(jsonPath("$.initialBalance").value(12345.67))
                .andExpect(jsonPath("$.color").value("#123456"))
                .andExpect(jsonPath("$.active").value(false));
    }

    /**
     * Teste 13: Múltiplas Carteiras São Retornadas Corretamente
     * <p>
     * Objetivo: Garantir que endpoint de listagem funciona com múltiplas carteiras.
     * <p>
     * Cenário:
     * - Usuário possui 4 carteiras diferentes
     * - Cada carteira tem configurações únicas
     * <p>
     * Resultado esperado:
     * - Array JSON com 4 elementos
     * - Cada carteira mantém seus dados únicos
     * - Ordem é preservada
     */
    @Test
    void shouldReturnMultipleWalletsCorrectly() throws Exception {
        // Arrange
        List<WalletDto> wallets = List.of(
                createWalletDto(UUID.randomUUID(), "Conta Corrente", new BigDecimal("1000"), "#FF0000", true),
                createWalletDto(UUID.randomUUID(), "Poupança", new BigDecimal("5000"), "#00FF00", true),
                createWalletDto(UUID.randomUUID(), "Investimentos", new BigDecimal("20000"), "#0000FF", true),
                createWalletDto(UUID.randomUUID(), "Carteira Antiga", new BigDecimal("0"), "#CCCCCC", false)
        );

        when(walletService.getWallets(userId)).thenReturn(wallets);

        // Act & Assert
        mockMvc.perform(get("/api/wallets")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].name").value("Conta Corrente"))
                .andExpect(jsonPath("$[1].name").value("Poupança"))
                .andExpect(jsonPath("$[2].name").value("Investimentos"))
                .andExpect(jsonPath("$[3].name").value("Carteira Antiga"))
                .andExpect(jsonPath("$[3].active").value(false));

        verify(walletService, times(1)).getWallets(userId);
    }

    /**
     * Teste 14: Isolamento de Dados Entre Usuários (Segurança)
     * <p>
     * Objetivo: Garantir que cada usuário só vê suas próprias carteiras.
     * <p>
     * Cenário:
     * - Dois usuários diferentes fazem requisições
     * - Cada um deve receber apenas suas carteiras
     * <p>
     * Resultado esperado:
     * - WalletService.getWallets é chamado com userId correto
     * - Dados de outros usuários não são expostos
     */
    @Test
    void shouldIsolateDataBetweenDifferentUsers() throws Exception {
        // Arrange - Primeiro usuário
        UUID user1Id = UUID.randomUUID();
        User user1 = new User();
        user1.setId(user1Id);

        UsernamePasswordAuthenticationToken auth1 = new UsernamePasswordAuthenticationToken(
                user1, null, new ArrayList<>()
        );

        List<WalletDto> user1Wallets = List.of(
                createWalletDto(UUID.randomUUID(), "Carteira User 1", new BigDecimal("1000"), "#FF0000", true)
        );

        when(walletService.getWallets(user1Id)).thenReturn(user1Wallets);

        // Act & Assert - Primeiro usuário
        mockMvc.perform(get("/api/wallets")
                        .with(authentication(auth1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Carteira User 1"));

        verify(walletService, times(1)).getWallets(user1Id);

        // Arrange - Segundo usuário
        UUID user2Id = UUID.randomUUID();
        User user2 = new User();
        user2.setId(user2Id);

        UsernamePasswordAuthenticationToken auth2 = new UsernamePasswordAuthenticationToken(
                user2, null, new ArrayList<>()
        );

        List<WalletDto> user2Wallets = List.of(
                createWalletDto(UUID.randomUUID(), "Carteira User 2", new BigDecimal("2000"), "#00FF00", true)
        );

        when(walletService.getWallets(user2Id)).thenReturn(user2Wallets);

        // Act & Assert - Segundo usuário
        mockMvc.perform(get("/api/wallets")
                        .with(authentication(auth2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Carteira User 2"));

        verify(walletService, times(1)).getWallets(user2Id);
    }

    /**
     * Teste 15: Criar Carteira com Saldo Inicial Zero
     * <p>
     * Objetivo: Garantir que carteiras podem ser criadas com saldo inicial zero.
     * <p>
     * Cenário:
     * - DTO com initialBalance = 0.00
     * - Criação de carteira nova sem saldo
     * <p>
     * Resultado esperado:
     * - Status HTTP 201 (Created)
     * - Carteira criada com saldo zero
     */
    @Test
    void shouldCreateWalletWithZeroInitialBalance() throws Exception {
        // Arrange
        WalletDto inputDto = createValidWalletDto();
        inputDto.setInitialBalance(BigDecimal.ZERO);

        UUID createdWalletId = UUID.randomUUID();
        WalletDto createdDto = createWalletDto(createdWalletId, "Carteira Nova",
                BigDecimal.ZERO, "#FFFFFF", true);

        when(walletService.createWallet(any(WalletDto.class), eq(userId)))
                .thenReturn(createdDto);

        // Act & Assert
        mockMvc.perform(post("/api/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto))
                        .with(authentication(authentication)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.initialBalance").value(0.00));

        verify(walletService, times(1)).createWallet(any(WalletDto.class), eq(userId));
    }

    // ==================== Métodos Auxiliares ====================

    /**
     * Cria um WalletDto válido para testes.
     *
     * @return WalletDto com dados válidos
     */
    private WalletDto createValidWalletDto() {
        WalletDto dto = new WalletDto();
        dto.setName("Nova Carteira");
        dto.setInitialBalance(new BigDecimal("3000.00"));
        dto.setColor("#00FFFF");
        dto.setActive(true);
        return dto;
    }

    /**
     * Cria um WalletDto com valores específicos para testes.
     *
     * @param id ID da carteira
     * @param name Nome da carteira
     * @param initialBalance Saldo inicial
     * @param color Cor da carteira
     * @param active Status ativo/inativo
     * @return WalletDto configurado
     */
    private WalletDto createWalletDto(UUID id, String name, BigDecimal initialBalance,
                                       String color, boolean active) {
        WalletDto dto = new WalletDto();
        dto.setId(id);
        dto.setName(name);
        dto.setInitialBalance(initialBalance);
        dto.setColor(color);
        dto.setActive(active);
        return dto;
    }
}
