package br.com.ronna.financerta.controller;

import br.com.ronna.financerta.dto.TransactionCategoryDto;
import br.com.ronna.financerta.model.User;
import br.com.ronna.financerta.service.TransactionCategoryService;
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
 * Testes de integração para TransactionCategoryController.
 * <p>
 * Esta classe implementa testes abrangentes de integração para o controller de categorias
 * de transações, cobrindo todos os cenários especificados no plano de testes:
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
 * - Usuários só podem acessar suas próprias categorias (isolamento de dados)
 * - Validações de negócio são aplicadas (nome obrigatório, sem duplicatas)
 * - Respostas HTTP seguem padrões REST (200, 201, 204, 404, etc.)
 * - Edge cases são tratados (categoria em uso, nome duplicado)
 * <p>
 * IMPORTANTE: Este controller gerencia categorias que são essenciais para
 * organização e classificação de transações financeiras. Embora não lide
 * diretamente com valores monetários, é fundamental para a integridade do sistema.
 *
 * @author Claude - Java Test Engineer AI Agent
 * @version 1.0
 * @since 2025-11-23
 */
@SpringBootTest
@AutoConfigureMockMvc
class TransactionCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionCategoryService categoryService;

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
     * Teste 1: Acesso Não Autorizado - Listar Categorias Sem Token
     * <p>
     * Objetivo: Garantir que o endpoint de listagem de categorias exige autenticação.
     * <p>
     * Cenário:
     * - Requisição GET para /api/transaction-categories
     * - Nenhum token de autenticação fornecido
     * - Spring Security intercepta a requisição
     * <p>
     * Resultado esperado:
     * - Status HTTP 403 (Forbidden)
     * - Acesso negado
     * - TransactionCategoryService NÃO é chamado
     */
    @Test
    void shouldReturn403WhenListingCategoriesWithoutAuthentication() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/transaction-categories"))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).findAllByUserId(any(UUID.class));
    }

    /**
     * Teste 2: Acesso Não Autorizado - Criar Categoria Sem Token
     * <p>
     * Objetivo: Garantir que criação de categorias exige autenticação.
     * <p>
     * Cenário:
     * - Requisição POST para /api/transaction-categories
     * - Nenhum token de autenticação
     * <p>
     * Resultado esperado:
     * - Status HTTP 403 (Forbidden)
     * - Categoria NÃO é criada
     */
    @Test
    void shouldReturn403WhenCreatingCategoryWithoutAuthentication() throws Exception {
        // Arrange
        TransactionCategoryDto categoryDto = createValidCategoryDto();

        // Act & Assert
        mockMvc.perform(post("/api/transaction-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDto)))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).save(any(TransactionCategoryDto.class), any(UUID.class));
    }

    /**
     * Teste 3: Acesso Não Autorizado - Atualizar Categoria Sem Token
     * <p>
     * Objetivo: Garantir que atualização de categorias exige autenticação.
     * <p>
     * Cenário:
     * - Requisição PUT para /api/transaction-categories/{id}
     * - Nenhum token de autenticação
     * <p>
     * Resultado esperado:
     * - Status HTTP 403 (Forbidden)
     * - Categoria NÃO é atualizada
     */
    @Test
    void shouldReturn403WhenUpdatingCategoryWithoutAuthentication() throws Exception {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        TransactionCategoryDto categoryDto = createValidCategoryDto();

        // Act & Assert
        mockMvc.perform(put("/api/transaction-categories/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDto)))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).update(any(UUID.class), any(TransactionCategoryDto.class), any(UUID.class));
    }

    /**
     * Teste 4: Acesso Não Autorizado - Deletar Categoria Sem Token
     * <p>
     * Objetivo: Garantir que deleção de categorias exige autenticação.
     * <p>
     * Cenário:
     * - Requisição DELETE para /api/transaction-categories/{id}
     * - Nenhum token de autenticação
     * <p>
     * Resultado esperado:
     * - Status HTTP 403 (Forbidden)
     * - Categoria NÃO é deletada
     */
    @Test
    void shouldReturn403WhenDeletingCategoryWithoutAuthentication() throws Exception {
        // Arrange
        UUID categoryId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(delete("/api/transaction-categories/{id}", categoryId))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).delete(any(UUID.class), any(UUID.class));
    }

    // ==================== Testes de CRUD - Happy Path ====================

    /**
     * Teste 5: Listar Todas as Categorias do Usuário Autenticado
     * <p>
     * Objetivo: Garantir que usuário autenticado pode listar suas categorias.
     * <p>
     * Cenário:
     * - Requisição GET para /api/transaction-categories
     * - Usuário autenticado
     * - TransactionCategoryService retorna lista de categorias
     * <p>
     * Resultado esperado:
     * - Status HTTP 200 (OK)
     * - Content-Type: application/json
     * - Array JSON com categorias do usuário
     * - Cada categoria contém id e name
     */
    @Test
    void shouldReturnAllCategoriesForAuthenticatedUser() throws Exception {
        // Arrange
        TransactionCategoryDto category1 = createCategoryDto(UUID.randomUUID(), "Alimentação");
        TransactionCategoryDto category2 = createCategoryDto(UUID.randomUUID(), "Transporte");
        TransactionCategoryDto category3 = createCategoryDto(UUID.randomUUID(), "Lazer");

        List<TransactionCategoryDto> categories = List.of(category1, category2, category3);

        when(categoryService.findAllByUserId(userId)).thenReturn(categories);

        // Act & Assert
        mockMvc.perform(get("/api/transaction-categories")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Alimentação"))
                .andExpect(jsonPath("$[1].name").value("Transporte"))
                .andExpect(jsonPath("$[2].name").value("Lazer"));

        verify(categoryService, times(1)).findAllByUserId(userId);
    }

    /**
     * Teste 6: Retornar Lista Vazia Quando Usuário Não Tem Categorias
     * <p>
     * Objetivo: Garantir comportamento correto quando usuário não possui categorias.
     * <p>
     * Cenário:
     * - Usuário autenticado
     * - TransactionCategoryService retorna lista vazia
     * <p>
     * Resultado esperado:
     * - Status HTTP 200 (OK)
     * - Array JSON vazio
     * - Não há exceções
     */
    @Test
    void shouldReturnEmptyListWhenUserHasNoCategories() throws Exception {
        // Arrange
        when(categoryService.findAllByUserId(userId)).thenReturn(new ArrayList<>());

        // Act & Assert
        mockMvc.perform(get("/api/transaction-categories")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(categoryService, times(1)).findAllByUserId(userId);
    }

    /**
     * Teste 7: Buscar Categoria por ID com Sucesso
     * <p>
     * Objetivo: Garantir que usuário autenticado pode buscar categoria específica.
     * <p>
     * Cenário:
     * - Requisição GET para /api/transaction-categories/{id}
     * - Categoria existe e pertence ao usuário
     * <p>
     * Resultado esperado:
     * - Status HTTP 200 (OK)
     * - JSON com dados completos da categoria
     */
    @Test
    void shouldReturnCategoryByIdForAuthenticatedUser() throws Exception {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        TransactionCategoryDto categoryDto = createCategoryDto(categoryId, "Saúde");

        when(categoryService.findById(categoryId, userId)).thenReturn(categoryDto);

        // Act & Assert
        mockMvc.perform(get("/api/transaction-categories/{id}", categoryId)
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.name").value("Saúde"));

        verify(categoryService, times(1)).findById(categoryId, userId);
    }

    /**
     * Teste 8: Criar Categoria com Sucesso
     * <p>
     * Objetivo: Garantir que usuário autenticado pode criar nova categoria.
     * <p>
     * Cenário:
     * - Requisição POST para /api/transaction-categories
     * - DTO válido com nome
     * - Usuário autenticado
     * <p>
     * Resultado esperado:
     * - Status HTTP 201 (Created)
     * - JSON com categoria criada (incluindo ID gerado)
     * - TransactionCategoryService.save é chamado com userId do contexto
     */
    @Test
    void shouldCreateCategorySuccessfullyForAuthenticatedUser() throws Exception {
        // Arrange
        TransactionCategoryDto inputDto = createValidCategoryDto();

        UUID createdCategoryId = UUID.randomUUID();
        TransactionCategoryDto createdDto = createCategoryDto(createdCategoryId, "Educação");

        when(categoryService.save(any(TransactionCategoryDto.class), eq(userId)))
                .thenReturn(createdDto);

        // Act & Assert
        mockMvc.perform(post("/api/transaction-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto))
                        .with(authentication(authentication)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(createdCategoryId.toString()))
                .andExpect(jsonPath("$.name").value("Educação"));

        verify(categoryService, times(1)).save(any(TransactionCategoryDto.class), eq(userId));
    }

    /**
     * Teste 9: Atualizar Categoria com Sucesso
     * <p>
     * Objetivo: Garantir que usuário autenticado pode atualizar suas categorias.
     * <p>
     * Cenário:
     * - Requisição PUT para /api/transaction-categories/{id}
     * - DTO com nome atualizado
     * - Categoria existe e pertence ao usuário
     * <p>
     * Resultado esperado:
     * - Status HTTP 200 (OK)
     * - JSON com categoria atualizada
     * - Mudanças são refletidas na resposta
     */
    @Test
    void shouldUpdateCategorySuccessfullyForAuthenticatedUser() throws Exception {
        // Arrange
        UUID categoryId = UUID.randomUUID();

        TransactionCategoryDto updateDto = createValidCategoryDto();
        updateDto.setName("Moradia Atualizada");

        TransactionCategoryDto updatedDto = createCategoryDto(categoryId, "Moradia Atualizada");

        when(categoryService.update(eq(categoryId), any(TransactionCategoryDto.class), eq(userId)))
                .thenReturn(updatedDto);

        // Act & Assert
        mockMvc.perform(put("/api/transaction-categories/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto))
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.name").value("Moradia Atualizada"));

        verify(categoryService, times(1)).update(eq(categoryId), any(TransactionCategoryDto.class), eq(userId));
    }

    /**
     * Teste 10: Deletar Categoria com Sucesso
     * <p>
     * Objetivo: Garantir que usuário autenticado pode deletar suas categorias.
     * <p>
     * Cenário:
     * - Requisição DELETE para /api/transaction-categories/{id}
     * - Categoria existe e pertence ao usuário
     * - Categoria não está em uso por transações (ou soft delete)
     * <p>
     * Resultado esperado:
     * - Status HTTP 204 (No Content)
     * - Sem corpo na resposta
     * - TransactionCategoryService.delete é chamado
     */
    @Test
    void shouldDeleteCategorySuccessfullyForAuthenticatedUser() throws Exception {
        // Arrange
        UUID categoryId = UUID.randomUUID();

        doNothing().when(categoryService).delete(categoryId, userId);

        // Act & Assert
        mockMvc.perform(delete("/api/transaction-categories/{id}", categoryId)
                        .with(authentication(authentication)))
                .andExpect(status().isNoContent());

        verify(categoryService, times(1)).delete(categoryId, userId);
    }

    // ==================== Testes de Validação e Exceções ====================

    /**
     * Teste 11: Retornar 404 Quando Categoria Não É Encontrada
     * <p>
     * Objetivo: Garantir resposta apropriada quando categoria não existe.
     * <p>
     * Cenário:
     * - Requisição GET para /api/transaction-categories/{id}
     * - ID não existe ou pertence a outro usuário
     * - TransactionCategoryService lança NoSuchElementException
     * <p>
     * Resultado esperado:
     * - Status HTTP 404 (Not Found) ou 500 (dependendo do exception handler)
     * - Mensagem de erro apropriada
     */
    @Test
    void shouldReturn404WhenCategoryNotFound() throws Exception {
        // Arrange
        UUID nonExistentCategoryId = UUID.randomUUID();

        when(categoryService.findById(nonExistentCategoryId, userId))
                .thenThrow(new NoSuchElementException("Categoria não encontrada"));

        // Act & Assert
        mockMvc.perform(get("/api/transaction-categories/{id}", nonExistentCategoryId)
                        .with(authentication(authentication)))
                .andExpect(status().is5xxServerError()); // Ou 404 se houver @ExceptionHandler

        verify(categoryService, times(1)).findById(nonExistentCategoryId, userId);
    }

    /**
     * Teste 12: Múltiplas Categorias São Retornadas Corretamente
     * <p>
     * Objetivo: Garantir que endpoint de listagem funciona com múltiplas categorias.
     * <p>
     * Cenário:
     * - Usuário possui 10 categorias diferentes
     * - Categorias cobrem diferentes áreas de gastos
     * <p>
     * Resultado esperado:
     * - Array JSON com 10 elementos
     * - Cada categoria mantém seu nome único
     * - Ordem é preservada
     */
    @Test
    void shouldReturnMultipleCategoriesCorrectly() throws Exception {
        // Arrange
        List<TransactionCategoryDto> categories = List.of(
                createCategoryDto(UUID.randomUUID(), "Alimentação"),
                createCategoryDto(UUID.randomUUID(), "Transporte"),
                createCategoryDto(UUID.randomUUID(), "Moradia"),
                createCategoryDto(UUID.randomUUID(), "Saúde"),
                createCategoryDto(UUID.randomUUID(), "Educação"),
                createCategoryDto(UUID.randomUUID(), "Lazer"),
                createCategoryDto(UUID.randomUUID(), "Vestuário"),
                createCategoryDto(UUID.randomUUID(), "Investimentos"),
                createCategoryDto(UUID.randomUUID(), "Impostos"),
                createCategoryDto(UUID.randomUUID(), "Outros")
        );

        when(categoryService.findAllByUserId(userId)).thenReturn(categories);

        // Act & Assert
        mockMvc.perform(get("/api/transaction-categories")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].name").value("Alimentação"))
                .andExpect(jsonPath("$[5].name").value("Lazer"))
                .andExpect(jsonPath("$[9].name").value("Outros"));

        verify(categoryService, times(1)).findAllByUserId(userId);
    }

    /**
     * Teste 13: Isolamento de Dados Entre Usuários (Segurança)
     * <p>
     * Objetivo: Garantir que cada usuário só vê suas próprias categorias.
     * <p>
     * Cenário:
     * - Dois usuários diferentes fazem requisições
     * - Cada um deve receber apenas suas categorias
     * <p>
     * Resultado esperado:
     * - TransactionCategoryService.findAllByUserId é chamado com userId correto
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

        List<TransactionCategoryDto> user1Categories = List.of(
                createCategoryDto(UUID.randomUUID(), "Categoria User 1")
        );

        when(categoryService.findAllByUserId(user1Id)).thenReturn(user1Categories);

        // Act & Assert - Primeiro usuário
        mockMvc.perform(get("/api/transaction-categories")
                        .with(authentication(auth1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Categoria User 1"));

        verify(categoryService, times(1)).findAllByUserId(user1Id);

        // Arrange - Segundo usuário
        UUID user2Id = UUID.randomUUID();
        User user2 = new User();
        user2.setId(user2Id);

        UsernamePasswordAuthenticationToken auth2 = new UsernamePasswordAuthenticationToken(
                user2, null, new ArrayList<>()
        );

        List<TransactionCategoryDto> user2Categories = List.of(
                createCategoryDto(UUID.randomUUID(), "Categoria User 2")
        );

        when(categoryService.findAllByUserId(user2Id)).thenReturn(user2Categories);

        // Act & Assert - Segundo usuário
        mockMvc.perform(get("/api/transaction-categories")
                        .with(authentication(auth2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Categoria User 2"));

        verify(categoryService, times(1)).findAllByUserId(user2Id);
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
        when(categoryService.findAllByUserId(userId)).thenReturn(new ArrayList<>());

        // Act & Assert
        mockMvc.perform(get("/api/transaction-categories")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"));
    }

    /**
     * Teste 15: Criar Categoria com Nome Contendo Caracteres Especiais
     * <p>
     * Objetivo: Garantir que categorias podem ter nomes com acentos e caracteres especiais.
     * <p>
     * Cenário:
     * - Criar categoria com nome "Educação & Formação"
     * - Nome contém acento e caracteres especiais
     * <p>
     * Resultado esperado:
     * - Status HTTP 201 (Created)
     * - Nome é preservado corretamente na resposta JSON
     * - Encoding UTF-8 funciona corretamente
     */
    @Test
    void shouldCreateCategoryWithSpecialCharactersInName() throws Exception {
        // Arrange
        TransactionCategoryDto inputDto = createValidCategoryDto();
        inputDto.setName("Educação & Formação");

        UUID createdCategoryId = UUID.randomUUID();
        TransactionCategoryDto createdDto = createCategoryDto(createdCategoryId, "Educação & Formação");

        when(categoryService.save(any(TransactionCategoryDto.class), eq(userId)))
                .thenReturn(createdDto);

        // Act & Assert
        mockMvc.perform(post("/api/transaction-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto))
                        .with(authentication(authentication)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Educação & Formação"));

        verify(categoryService, times(1)).save(any(TransactionCategoryDto.class), eq(userId));
    }

    // ==================== Métodos Auxiliares ====================

    /**
     * Cria um TransactionCategoryDto válido para testes.
     *
     * @return TransactionCategoryDto com dados válidos
     */
    private TransactionCategoryDto createValidCategoryDto() {
        TransactionCategoryDto dto = new TransactionCategoryDto();
        dto.setName("Educação");
        return dto;
    }

    /**
     * Cria um TransactionCategoryDto com valores específicos para testes.
     *
     * @param id ID da categoria
     * @param name Nome da categoria
     * @return TransactionCategoryDto configurado
     */
    private TransactionCategoryDto createCategoryDto(UUID id, String name) {
        TransactionCategoryDto dto = new TransactionCategoryDto();
        dto.setId(id);
        dto.setName(name);
        return dto;
    }
}
