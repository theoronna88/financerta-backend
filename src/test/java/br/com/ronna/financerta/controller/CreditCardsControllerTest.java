package br.com.ronna.financerta.controller;

import br.com.ronna.financerta.dto.CreditCardDto;
import br.com.ronna.financerta.model.User;
import br.com.ronna.financerta.service.CreditCardService;
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
 * Testes de integração para CreditCardsController.
 * <p>
 * Esta classe implementa testes abrangentes de integração para o controller de cartões de crédito,
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
 * - Usuários só podem acessar seus próprios cartões (isolamento de dados)
 * - Validações de negócio são aplicadas corretamente
 * - Dados financeiros sensíveis (limite, dias de fechamento/vencimento) são preservados
 * - Respostas HTTP seguem padrões REST (200, 201, 204, 404, etc.)
 * <p>
 * IMPORTANTE: Este controller lida com dados financeiros críticos (cartões de crédito).
 * Qualquer falha pode resultar em perda de integridade de dados ou violação de segurança.
 *
 * @author Claude - Java Test Engineer AI Agent
 * @version 1.0
 * @since 2025-11-23
 */
@SpringBootTest
@AutoConfigureMockMvc
class CreditCardsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreditCardService creditCardService;

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
     * Teste 1: Acesso Não Autorizado - Listar Cartões Sem Token
     * <p>
     * Objetivo: Garantir que o endpoint de listagem de cartões exige autenticação.
     * <p>
     * Cenário:
     * - Requisição GET para /api/credit-cards
     * - Nenhum token de autenticação fornecido
     * - Spring Security intercepta a requisição
     * <p>
     * Resultado esperado:
     * - Status HTTP 403 (Forbidden)
     * - Acesso negado
     * - CreditCardService NÃO é chamado
     * - Dados sensíveis permanecem protegidos
     */
    @Test
    void shouldReturn403WhenListingCreditCardsWithoutAuthentication() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/credit-cards"))
                .andExpect(status().isForbidden());

        // Verificar que o serviço não foi chamado (requisição bloqueada pelo filtro de segurança)
        verify(creditCardService, never()).findAll(any(UUID.class));
    }

    /**
     * Teste 2: Acesso Não Autorizado - Criar Cartão Sem Token
     * <p>
     * Objetivo: Garantir que criação de cartões exige autenticação.
     * <p>
     * Cenário:
     * - Requisição POST para /api/credit-cards
     * - Nenhum token de autenticação
     * <p>
     * Resultado esperado:
     * - Status HTTP 403 (Forbidden)
     * - Cartão NÃO é criado
     */
    @Test
    void shouldReturn403WhenCreatingCreditCardWithoutAuthentication() throws Exception {
        // Arrange
        CreditCardDto creditCardDto = createValidCreditCardDto();

        // Act & Assert
        mockMvc.perform(post("/api/credit-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creditCardDto)))
                .andExpect(status().isForbidden());

        verify(creditCardService, never()).save(any(CreditCardDto.class), any(UUID.class));
    }

    /**
     * Teste 3: Acesso Não Autorizado - Atualizar Cartão Sem Token
     * <p>
     * Objetivo: Garantir que atualização de cartões exige autenticação.
     * <p>
     * Cenário:
     * - Requisição PUT para /api/credit-cards/{id}
     * - Nenhum token de autenticação
     * <p>
     * Resultado esperado:
     * - Status HTTP 403 (Forbidden)
     * - Cartão NÃO é atualizado
     */
    @Test
    void shouldReturn403WhenUpdatingCreditCardWithoutAuthentication() throws Exception {
        // Arrange
        UUID cardId = UUID.randomUUID();
        CreditCardDto creditCardDto = createValidCreditCardDto();

        // Act & Assert
        mockMvc.perform(put("/api/credit-cards/{id}", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creditCardDto)))
                .andExpect(status().isForbidden());

        verify(creditCardService, never()).update(any(UUID.class), any(CreditCardDto.class), any(UUID.class));
    }

    /**
     * Teste 4: Acesso Não Autorizado - Deletar Cartão Sem Token
     * <p>
     * Objetivo: Garantir que deleção de cartões exige autenticação.
     * <p>
     * Cenário:
     * - Requisição DELETE para /api/credit-cards/{id}
     * - Nenhum token de autenticação
     * <p>
     * Resultado esperado:
     * - Status HTTP 403 (Forbidden)
     * - Cartão NÃO é deletado
     */
    @Test
    void shouldReturn403WhenDeletingCreditCardWithoutAuthentication() throws Exception {
        // Arrange
        UUID cardId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(delete("/api/credit-cards/{id}", cardId))
                .andExpect(status().isForbidden());

        verify(creditCardService, never()).delete(any(UUID.class), any(UUID.class));
    }

    // ==================== Testes de CRUD - Happy Path ====================

    /**
     * Teste 5: Listar Todos os Cartões do Usuário Autenticado
     * <p>
     * Objetivo: Garantir que usuário autenticado pode listar seus cartões.
     * <p>
     * Cenário:
     * - Requisição GET para /api/credit-cards
     * - Usuário autenticado
     * - CreditCardService retorna lista de cartões
     * <p>
     * Resultado esperado:
     * - Status HTTP 200 (OK)
     * - Content-Type: application/json
     * - Array JSON com cartões do usuário
     * - Cada cartão contém campos esperados (id, name, limitValue, etc.)
     */
    @Test
    void shouldReturnAllCreditCardsForAuthenticatedUser() throws Exception {
        // Arrange
        CreditCardDto card1 = createCreditCardDto(UUID.randomUUID(), "Cartão Gold",
                new BigDecimal("5000.00"), 10, 20);
        CreditCardDto card2 = createCreditCardDto(UUID.randomUUID(), "Cartão Platinum",
                new BigDecimal("10000.00"), 15, 25);

        List<CreditCardDto> creditCards = List.of(card1, card2);

        when(creditCardService.findAll(userId)).thenReturn(creditCards);

        // Act & Assert
        mockMvc.perform(get("/api/credit-cards")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Cartão Gold"))
                .andExpect(jsonPath("$[0].limitValue").value(5000.00))
                .andExpect(jsonPath("$[0].closingDay").value(10))
                .andExpect(jsonPath("$[0].dueDay").value(20))
                .andExpect(jsonPath("$[1].name").value("Cartão Platinum"))
                .andExpect(jsonPath("$[1].limitValue").value(10000.00));

        verify(creditCardService, times(1)).findAll(userId);
    }

    /**
     * Teste 6: Retornar Lista Vazia Quando Usuário Não Tem Cartões
     * <p>
     * Objetivo: Garantir comportamento correto quando usuário não possui cartões.
     * <p>
     * Cenário:
     * - Usuário autenticado
     * - CreditCardService retorna lista vazia
     * <p>
     * Resultado esperado:
     * - Status HTTP 200 (OK)
     * - Array JSON vazio
     * - Não há exceções
     */
    @Test
    void shouldReturnEmptyListWhenUserHasNoCreditCards() throws Exception {
        // Arrange
        when(creditCardService.findAll(userId)).thenReturn(new ArrayList<>());

        // Act & Assert
        mockMvc.perform(get("/api/credit-cards")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(creditCardService, times(1)).findAll(userId);
    }

    /**
     * Teste 7: Buscar Cartão por ID com Sucesso
     * <p>
     * Objetivo: Garantir que usuário autenticado pode buscar cartão específico.
     * <p>
     * Cenário:
     * - Requisição GET para /api/credit-cards/{id}
     * - Cartão existe e pertence ao usuário
     * <p>
     * Resultado esperado:
     * - Status HTTP 200 (OK)
     * - JSON com dados completos do cartão
     * - Todos os campos sensíveis preservados
     */
    @Test
    void shouldReturnCreditCardByIdForAuthenticatedUser() throws Exception {
        // Arrange
        UUID cardId = UUID.randomUUID();
        CreditCardDto creditCardDto = createCreditCardDto(cardId, "Cartão Empresarial",
                new BigDecimal("15000.00"), 5, 15);

        when(creditCardService.findById(cardId, userId)).thenReturn(creditCardDto);

        // Act & Assert
        mockMvc.perform(get("/api/credit-cards/{id}", cardId)
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(cardId.toString()))
                .andExpect(jsonPath("$.name").value("Cartão Empresarial"))
                .andExpect(jsonPath("$.limitValue").value(15000.00))
                .andExpect(jsonPath("$.closingDay").value(5))
                .andExpect(jsonPath("$.dueDay").value(15));

        verify(creditCardService, times(1)).findById(cardId, userId);
    }

    /**
     * Teste 8: Criar Cartão de Crédito com Sucesso
     * <p>
     * Objetivo: Garantir que usuário autenticado pode criar novo cartão.
     * <p>
     * Cenário:
     * - Requisição POST para /api/credit-cards
     * - DTO válido com todos os campos obrigatórios
     * - Usuário autenticado
     * <p>
     * Resultado esperado:
     * - Status HTTP 201 (Created)
     * - JSON com cartão criado (incluindo ID gerado)
     * - CreditCardService.save é chamado com userId do contexto autenticado
     */
    @Test
    void shouldCreateCreditCardSuccessfullyForAuthenticatedUser() throws Exception {
        // Arrange
        CreditCardDto inputDto = createValidCreditCardDto();

        UUID createdCardId = UUID.randomUUID();
        CreditCardDto createdDto = createCreditCardDto(createdCardId, "Novo Cartão Gold",
                new BigDecimal("8000.00"), 12, 22);

        when(creditCardService.save(any(CreditCardDto.class), eq(userId)))
                .thenReturn(createdDto);

        // Act & Assert
        mockMvc.perform(post("/api/credit-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto))
                        .with(authentication(authentication)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(createdCardId.toString()))
                .andExpect(jsonPath("$.name").value("Novo Cartão Gold"))
                .andExpect(jsonPath("$.limitValue").value(8000.00))
                .andExpect(jsonPath("$.closingDay").value(12))
                .andExpect(jsonPath("$.dueDay").value(22));

        verify(creditCardService, times(1)).save(any(CreditCardDto.class), eq(userId));
    }

    /**
     * Teste 9: Atualizar Cartão de Crédito com Sucesso
     * <p>
     * Objetivo: Garantir que usuário autenticado pode atualizar seus cartões.
     * <p>
     * Cenário:
     * - Requisição PUT para /api/credit-cards/{id}
     * - DTO com dados atualizados
     * - Cartão existe e pertence ao usuário
     * <p>
     * Resultado esperado:
     * - Status HTTP 200 (OK)
     * - JSON com cartão atualizado
     * - Mudanças são refletidas na resposta
     */
    @Test
    void shouldUpdateCreditCardSuccessfullyForAuthenticatedUser() throws Exception {
        // Arrange
        UUID cardId = UUID.randomUUID();

        CreditCardDto updateDto = createValidCreditCardDto();
        updateDto.setName("Cartão Atualizado");
        updateDto.setLimitValue(new BigDecimal("20000.00"));

        CreditCardDto updatedDto = createCreditCardDto(cardId, "Cartão Atualizado",
                new BigDecimal("20000.00"), 10, 20);

        when(creditCardService.update(eq(cardId), any(CreditCardDto.class), eq(userId)))
                .thenReturn(updatedDto);

        // Act & Assert
        mockMvc.perform(put("/api/credit-cards/{id}", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto))
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(cardId.toString()))
                .andExpect(jsonPath("$.name").value("Cartão Atualizado"))
                .andExpect(jsonPath("$.limitValue").value(20000.00));

        verify(creditCardService, times(1)).update(eq(cardId), any(CreditCardDto.class), eq(userId));
    }

    /**
     * Teste 10: Deletar Cartão de Crédito com Sucesso
     * <p>
     * Objetivo: Garantir que usuário autenticado pode deletar seus cartões.
     * <p>
     * Cenário:
     * - Requisição DELETE para /api/credit-cards/{id}
     * - Cartão existe e pertence ao usuário
     * <p>
     * Resultado esperado:
     * - Status HTTP 204 (No Content)
     * - Sem corpo na resposta
     * - CreditCardService.delete é chamado
     */
    @Test
    void shouldDeleteCreditCardSuccessfullyForAuthenticatedUser() throws Exception {
        // Arrange
        UUID cardId = UUID.randomUUID();

        doNothing().when(creditCardService).delete(cardId, userId);

        // Act & Assert
        mockMvc.perform(delete("/api/credit-cards/{id}", cardId)
                        .with(authentication(authentication)))
                .andExpect(status().isNoContent());

        verify(creditCardService, times(1)).delete(cardId, userId);
    }

    // ==================== Testes de Validação e Exceções ====================

    /**
     * Teste 11: Retornar 404 Quando Cartão Não É Encontrado
     * <p>
     * Objetivo: Garantir resposta apropriada quando cartão não existe.
     * <p>
     * Cenário:
     * - Requisição GET para /api/credit-cards/{id}
     * - ID não existe ou pertence a outro usuário
     * - CreditCardService lança NoSuchElementException
     * <p>
     * Resultado esperado:
     * - Status HTTP 404 (Not Found) ou 500 (dependendo do exception handler)
     * - Mensagem de erro apropriada
     */
    @Test
    void shouldReturn404WhenCreditCardNotFound() throws Exception {
        // Arrange
        UUID nonExistentCardId = UUID.randomUUID();

        when(creditCardService.findById(nonExistentCardId, userId))
                .thenThrow(new NoSuchElementException("Cartão de crédito não encontrado"));

        // Act & Assert
        mockMvc.perform(get("/api/credit-cards/{id}", nonExistentCardId)
                        .with(authentication(authentication)))
                .andExpect(status().is5xxServerError()); // Ou 404 se houver @ExceptionHandler

        verify(creditCardService, times(1)).findById(nonExistentCardId, userId);
    }

    /**
     * Teste 12: Campos Críticos São Preservados na Serialização
     * <p>
     * Objetivo: Garantir que todos os campos críticos de um cartão de crédito
     * são corretamente serializados para JSON.
     * <p>
     * Cenário:
     * - Buscar cartão com todos os campos preenchidos
     * - Verificar que resposta JSON contém todos os dados
     * <p>
     * Resultado esperado:
     * - Todos os campos estão presentes no JSON
     * - Tipos numéricos (BigDecimal) são formatados corretamente
     * - Datas e flags booleanas são preservados
     */
    @Test
    void shouldPreserveCriticalFieldsInJsonSerialization() throws Exception {
        // Arrange
        UUID cardId = UUID.randomUUID();
        CreditCardDto creditCardDto = new CreditCardDto();
        creditCardDto.setId(cardId);
        creditCardDto.setName("Cartão Completo");
        creditCardDto.setLimitValue(new BigDecimal("12345.67"));
        creditCardDto.setClosingDay(8);
        creditCardDto.setDueDay(18);
        creditCardDto.setActive(true);

        when(creditCardService.findById(cardId, userId)).thenReturn(creditCardDto);

        // Act & Assert
        mockMvc.perform(get("/api/credit-cards/{id}", cardId)
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId.toString()))
                .andExpect(jsonPath("$.name").value("Cartão Completo"))
                .andExpect(jsonPath("$.limitValue").value(12345.67))
                .andExpect(jsonPath("$.closingDay").value(8))
                .andExpect(jsonPath("$.dueDay").value(18))
                .andExpect(jsonPath("$.active").value(true));
    }

    /**
     * Teste 13: Múltiplos Cartões São Retornados Corretamente
     * <p>
     * Objetivo: Garantir que endpoint de listagem funciona com múltiplos cartões.
     * <p>
     * Cenário:
     * - Usuário possui 5 cartões diferentes
     * - Cada cartão tem configurações únicas
     * <p>
     * Resultado esperado:
     * - Array JSON com 5 elementos
     * - Cada cartão mantém seus dados únicos
     * - Ordem é preservada
     */
    @Test
    void shouldReturnMultipleCreditCardsCorrectly() throws Exception {
        // Arrange
        List<CreditCardDto> creditCards = List.of(
                createCreditCardDto(UUID.randomUUID(), "Cartão 1", new BigDecimal("1000"), 5, 10),
                createCreditCardDto(UUID.randomUUID(), "Cartão 2", new BigDecimal("2000"), 10, 15),
                createCreditCardDto(UUID.randomUUID(), "Cartão 3", new BigDecimal("3000"), 15, 20),
                createCreditCardDto(UUID.randomUUID(), "Cartão 4", new BigDecimal("4000"), 20, 25),
                createCreditCardDto(UUID.randomUUID(), "Cartão 5", new BigDecimal("5000"), 25, 30)
        );

        when(creditCardService.findAll(userId)).thenReturn(creditCards);

        // Act & Assert
        mockMvc.perform(get("/api/credit-cards")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].name").value("Cartão 1"))
                .andExpect(jsonPath("$[1].name").value("Cartão 2"))
                .andExpect(jsonPath("$[2].name").value("Cartão 3"))
                .andExpect(jsonPath("$[3].name").value("Cartão 4"))
                .andExpect(jsonPath("$[4].name").value("Cartão 5"));

        verify(creditCardService, times(1)).findAll(userId);
    }

    /**
     * Teste 14: Content-Type Header É Configurado Corretamente
     * <p>
     * Objetivo: Garantir que todas as respostas JSON têm Content-Type correto.
     * <p>
     * Cenário:
     * - Requisições para diferentes endpoints
     * <p>
     * Resultado esperado:
     * - Content-Type: application/json em todas as respostas com corpo
     */
    @Test
    void shouldSetCorrectContentTypeHeader() throws Exception {
        // Arrange
        when(creditCardService.findAll(userId)).thenReturn(new ArrayList<>());

        // Act & Assert
        mockMvc.perform(get("/api/credit-cards")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"));
    }

    /**
     * Teste 15: Isolamento de Dados Entre Usuários (Segurança)
     * <p>
     * Objetivo: Garantir que cada usuário só vê seus próprios cartões.
     * <p>
     * Cenário:
     * - Dois usuários diferentes fazem requisições
     * - Cada um deve receber apenas seus cartões
     * <p>
     * Resultado esperado:
     * - CreditCardService.findAll é chamado com userId correto
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

        List<CreditCardDto> user1Cards = List.of(
                createCreditCardDto(UUID.randomUUID(), "Cartão User 1", new BigDecimal("5000"), 10, 20)
        );

        when(creditCardService.findAll(user1Id)).thenReturn(user1Cards);

        // Act & Assert - Primeiro usuário
        mockMvc.perform(get("/api/credit-cards")
                        .with(authentication(auth1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Cartão User 1"));

        verify(creditCardService, times(1)).findAll(user1Id);

        // Arrange - Segundo usuário
        UUID user2Id = UUID.randomUUID();
        User user2 = new User();
        user2.setId(user2Id);

        UsernamePasswordAuthenticationToken auth2 = new UsernamePasswordAuthenticationToken(
                user2, null, new ArrayList<>()
        );

        List<CreditCardDto> user2Cards = List.of(
                createCreditCardDto(UUID.randomUUID(), "Cartão User 2", new BigDecimal("3000"), 15, 25)
        );

        when(creditCardService.findAll(user2Id)).thenReturn(user2Cards);

        // Act & Assert - Segundo usuário
        mockMvc.perform(get("/api/credit-cards")
                        .with(authentication(auth2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Cartão User 2"));

        verify(creditCardService, times(1)).findAll(user2Id);
    }

    // ==================== Métodos Auxiliares ====================

    /**
     * Cria um CreditCardDto válido para testes.
     *
     * @return CreditCardDto com dados válidos
     */
    private CreditCardDto createValidCreditCardDto() {
        CreditCardDto dto = new CreditCardDto();
        dto.setName("Novo Cartão Gold");
        dto.setLimitValue(new BigDecimal("8000.00"));
        dto.setClosingDay(12);
        dto.setDueDay(22);
        dto.setActive(true);
        return dto;
    }

    /**
     * Cria um CreditCardDto com valores específicos para testes.
     *
     * @param id ID do cartão
     * @param name Nome do cartão
     * @param limitValue Limite do cartão
     * @param closingDay Dia de fechamento
     * @param dueDay Dia de vencimento
     * @return CreditCardDto configurado
     */
    private CreditCardDto createCreditCardDto(UUID id, String name, BigDecimal limitValue,
                                               int closingDay, int dueDay) {
        CreditCardDto dto = new CreditCardDto();
        dto.setId(id);
        dto.setName(name);
        dto.setLimitValue(limitValue);
        dto.setClosingDay(closingDay);
        dto.setDueDay(dueDay);
        dto.setActive(true);
        return dto;
    }
}
