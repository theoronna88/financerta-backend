package br.com.ronna.financerta.service.impl;

import br.com.ronna.financerta.dto.TransactionCategoryDto;
import br.com.ronna.financerta.model.TransactionCategory;
import br.com.ronna.financerta.model.User;
import br.com.ronna.financerta.repository.TransactionCategoryRepository;
import br.com.ronna.financerta.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para TransactionCategoryServiceImpl.
 * <p>
 * Esta classe implementa os testes de CRUD especificados no plano de testes:
 * 1. Busca de categoria por ID com sucesso
 * 2. Falha ao buscar categoria não encontrada ou que não pertence ao usuário
 * 3. Criação de categoria com userId correto
 * 4. Deleção de categoria com sucesso (hard delete)
 * <p>
 * Estes testes garantem que:
 * - Usuários só podem acessar suas próprias categorias (segurança)
 * - O userId do contexto autenticado é usado (não o do DTO)
 * - Hard delete é implementado corretamente
 * - Categorias são essenciais para organização de transações
 */
@ExtendWith(MockitoExtension.class)
class TransactionCategoryServiceImplTest {

    @Mock
    private TransactionCategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionCategoryServiceImpl categoryService;

    /**
     * Teste 1: Busca de Categoria por ID com Sucesso
     * <p>
     * Objetivo: Garantir que uma categoria pode ser buscada com sucesso quando
     * o ID e o userId correspondem a uma categoria existente.
     * <p>
     * Cenário:
     * - Categoria existe no sistema
     * - Categoria pertence ao usuário autenticado
     * - Busca por ID e userId
     * <p>
     * Resultado esperado:
     * - categoryRepository.findByIdAndUserId é chamado
     * - TransactionCategoryDto é retornado com os dados corretos
     * - Nome e ID correspondem à entidade
     */
    @Test
    void shouldFindCategoryByIdSuccessfully() {
        // Arrange (Arrumar)
        UUID categoryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        TransactionCategory category = new TransactionCategory();
        category.setId(categoryId);
        category.setName("Alimentação");
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());

        User user = new User();
        user.setId(userId);
        category.setUser(user);

        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.of(category));

        // Act (Agir)
        TransactionCategoryDto result = categoryService.findById(categoryId, userId);

        // Assert (Afirmar)
        assertNotNull(result, "O resultado não deve ser nulo");
        assertEquals(categoryId, result.getId(), "O ID deve corresponder");
        assertEquals("Alimentação", result.getName(), "O nome deve corresponder");

        // Verifica que o repositório foi chamado corretamente
        verify(categoryRepository, times(1)).findByIdAndUserId(categoryId, userId);
    }

    /**
     * Teste 2: Falha ao Buscar Categoria Não Encontrada ou de Outro Usuário
     * <p>
     * Objetivo: Garantir que o sistema impede o acesso a categorias que não existem
     * ou que pertencem a outro usuário (segurança).
     * <p>
     * Cenário:
     * - Busca por ID e userId
     * - Categoria não existe OU pertence a outro usuário
     * - Repository retorna Optional.empty()
     * <p>
     * Resultado esperado:
     * - categoryRepository.findByIdAndUserId é chamado
     * - Retorna null (comportamento atual do serviço)
     * <p>
     * Nota: O serviço atual retorna null ao invés de lançar exceção.
     * Esta é uma decisão de design consistente com WalletService.
     */
    @Test
    void shouldReturnNullWhenCategoryNotFoundOrNotOwned() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // Simula que a categoria não foi encontrada para este usuário
        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.empty());

        // Act
        TransactionCategoryDto result = categoryService.findById(categoryId, userId);

        // Assert
        assertNull(result, "O resultado deve ser nulo quando a categoria não é encontrada");

        // Verifica que o repositório foi chamado
        verify(categoryRepository, times(1)).findByIdAndUserId(categoryId, userId);
    }

    /**
     * Teste 3: Criação de Categoria com UserId Correto
     * <p>
     * Objetivo: Garantir que ao criar uma categoria, o userId do contexto autenticado
     * é usado, não o userId que pode vir no DTO (segurança crítica).
     * <p>
     * Cenário:
     * - Usuário autenticado com userId específico
     * - TransactionCategoryDto contém nome da categoria
     * - Criação de nova categoria
     * <p>
     * Resultado esperado:
     * - userRepository.findById é chamado com o userId autenticado
     * - Categoria é criada com o User encontrado
     * - createdAt e updatedAt são setados
     * - TransactionCategoryDto é retornado com os dados salvos
     */
    @Test
    void shouldCreateCategoryWithCorrectUserId() {
        // Arrange
        UUID authenticatedUserId = UUID.randomUUID();

        TransactionCategoryDto inputDto = new TransactionCategoryDto();
        inputDto.setName("Transporte");

        // Cria o usuário autenticado
        User authenticatedUser = new User();
        authenticatedUser.setId(authenticatedUserId);
        authenticatedUser.setName("Usuário Autenticado");

        when(userRepository.findById(authenticatedUserId))
                .thenReturn(Optional.of(authenticatedUser));

        // Simula o salvamento da categoria
        when(categoryRepository.save(any(TransactionCategory.class))).thenAnswer(invocation -> {
            TransactionCategory category = invocation.getArgument(0);
            category.setId(UUID.randomUUID()); // Simula a geração do ID
            return category;
        });

        // Act
        TransactionCategoryDto result = categoryService.save(inputDto, authenticatedUserId);

        // Assert
        assertNotNull(result, "O resultado não deve ser nulo");
        assertEquals("Transporte", result.getName(), "O nome deve corresponder");

        // Verifica que o usuário foi buscado com o userId autenticado
        verify(userRepository, times(1)).findById(authenticatedUserId);

        // Captura a categoria que foi salva para verificar os dados críticos
        ArgumentCaptor<TransactionCategory> categoryCaptor = ArgumentCaptor.forClass(TransactionCategory.class);
        verify(categoryRepository, times(1)).save(categoryCaptor.capture());

        TransactionCategory savedCategory = categoryCaptor.getValue();
        assertEquals("Transporte", savedCategory.getName(), "Nome deve corresponder");
        assertNotNull(savedCategory.getCreatedAt(), "CreatedAt deve ser setado");
        assertNotNull(savedCategory.getUpdatedAt(), "UpdatedAt deve ser setado");
        assertEquals(authenticatedUser, savedCategory.getUser(),
                "User deve ser o usuário autenticado");
    }

    /**
     * Teste 3.1: Falha ao Criar Categoria Quando Usuário Não Encontrado
     * <p>
     * Objetivo: Garantir que o sistema lança exceção quando tenta criar
     * uma categoria para um usuário que não existe.
     * <p>
     * Cenário:
     * - userId não corresponde a nenhum usuário no sistema
     * - userRepository.findById retorna Optional.empty()
     * <p>
     * Resultado esperado:
     * - NoSuchElementException é lançada
     * - categoryRepository.save NÃO é chamado
     */
    @Test
    void shouldThrowExceptionWhenCreatingCategoryForNonExistentUser() {
        // Arrange
        UUID nonExistentUserId = UUID.randomUUID();

        TransactionCategoryDto inputDto = new TransactionCategoryDto();
        inputDto.setName("Lazer");

        // Simula que o usuário não foi encontrado
        when(userRepository.findById(nonExistentUserId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                NoSuchElementException.class,
                () -> categoryService.save(inputDto, nonExistentUserId),
                "Deve lançar NoSuchElementException quando usuário não existe"
        );

        // Verifica que o repositório de usuário foi consultado
        verify(userRepository, times(1)).findById(nonExistentUserId);

        // Verifica que a categoria NÃO foi salva
        verify(categoryRepository, never()).save(any(TransactionCategory.class));
    }

    /**
     * Teste 4: Deleção de Categoria com Sucesso (Hard Delete)
     * <p>
     * Objetivo: Garantir que ao deletar uma categoria, ela é removida do banco.
     * <p>
     * Cenário:
     * - Categoria existe e pertence ao usuário
     * - Usuário solicita deleção da categoria
     * - Categoria não está associada a transações (verificação futura)
     * <p>
     * Resultado esperado:
     * - categoryRepository.findByIdAndUserId é chamado
     * - categoryRepository.delete é chamado com a categoria
     * - Categoria é removida permanentemente (hard delete)
     */
    @Test
    void shouldDeleteCategorySuccessfully() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        TransactionCategory category = new TransactionCategory();
        category.setId(categoryId);
        category.setName("Categoria para Deletar");
        category.setCreatedAt(LocalDateTime.now().minusDays(10));
        category.setUpdatedAt(LocalDateTime.now().minusDays(5));

        User user = new User();
        user.setId(userId);
        category.setUser(user);

        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.of(category));

        // Act
        categoryService.delete(categoryId, userId);

        // Assert
        // Verifica que a categoria foi buscada
        verify(categoryRepository, times(1)).findByIdAndUserId(categoryId, userId);

        // Verifica que delete foi chamado (hard delete)
        verify(categoryRepository, times(1)).delete(category);
    }

    /**
     * Teste 4.1: Não Lança Exceção ao Tentar Deletar Categoria Não Encontrada
     * <p>
     * Objetivo: Garantir que o sistema não lança exceção quando tenta deletar
     * uma categoria que não existe (comportamento atual do serviço).
     * <p>
     * Cenário:
     * - Busca por ID e userId
     * - Categoria não existe OU pertence a outro usuário
     * - Repository retorna Optional.empty()
     * <p>
     * Resultado esperado:
     * - categoryRepository.findByIdAndUserId é chamado
     * - categoryRepository.delete NÃO é chamado
     * - Nenhuma exceção é lançada (comportamento silencioso)
     * <p>
     * Nota: O serviço atual usa ifPresent, então não lança exceção.
     * Este comportamento pode ser considerado uma feature de segurança.
     */
    @Test
    void shouldNotThrowExceptionWhenDeletingNonExistentCategory() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // Simula que a categoria não foi encontrada para este usuário
        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert - Não deve lançar exceção
        assertDoesNotThrow(
                () -> categoryService.delete(categoryId, userId),
                "Não deve lançar exceção quando categoria não é encontrada"
        );

        // Verifica que o repositório foi consultado
        verify(categoryRepository, times(1)).findByIdAndUserId(categoryId, userId);

        // Verifica que delete NÃO foi chamado
        verify(categoryRepository, never()).delete(any(TransactionCategory.class));
    }

    /**
     * Teste 5: Atualização de Categoria com Sucesso
     * <p>
     * Objetivo: Garantir que uma categoria pode ser atualizada corretamente,
     * preservando a segurança de userId.
     * <p>
     * Cenário:
     * - Categoria existe e pertence ao usuário
     * - Usuário atualiza o nome da categoria
     * <p>
     * Resultado esperado:
     * - Categoria é atualizada com o novo nome
     * - userId permanece o mesmo (não pode ser alterado)
     * - updatedAt é atualizado
     * - categoryRepository.save é chamado com os dados atualizados
     */
    @Test
    void shouldUpdateCategorySuccessfully() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        TransactionCategory existingCategory = new TransactionCategory();
        existingCategory.setId(categoryId);
        existingCategory.setName("Nome Antigo");
        existingCategory.setCreatedAt(LocalDateTime.now().minusDays(30));
        existingCategory.setUpdatedAt(LocalDateTime.now().minusDays(10));

        User user = new User();
        user.setId(userId);
        existingCategory.setUser(user);

        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.of(existingCategory));

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(categoryRepository.save(any(TransactionCategory.class))).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });

        TransactionCategoryDto updateDto = new TransactionCategoryDto();
        updateDto.setName("Nome Atualizado");

        // Act
        TransactionCategoryDto result = categoryService.update(categoryId, updateDto, userId);

        // Assert
        assertNotNull(result, "O resultado não deve ser nulo");
        assertEquals("Nome Atualizado", result.getName(), "Nome deve ser atualizado");

        // Verifica que a categoria foi buscada
        verify(categoryRepository, times(1)).findByIdAndUserId(categoryId, userId);

        // Verifica que o usuário foi buscado
        verify(userRepository, times(1)).findById(userId);

        // Captura a categoria salva
        ArgumentCaptor<TransactionCategory> categoryCaptor = ArgumentCaptor.forClass(TransactionCategory.class);
        verify(categoryRepository, times(1)).save(categoryCaptor.capture());

        TransactionCategory updatedCategory = categoryCaptor.getValue();
        assertEquals("Nome Atualizado", updatedCategory.getName(), "Nome deve estar atualizado");
        assertNotNull(updatedCategory.getUpdatedAt(), "UpdatedAt deve ser setado");
    }

    /**
     * Teste 6: Busca de Todas as Categorias do Usuário
     * <p>
     * Objetivo: Garantir que todas as categorias de um usuário podem ser listadas.
     * <p>
     * Cenário:
     * - Usuário possui múltiplas categorias
     * - Busca todas as categorias do usuário
     * <p>
     * Resultado esperado:
     * - categoryRepository.findByUserId é chamado
     * - Lista de DTOs é retornada com todas as categorias
     * - Cada DTO contém os dados corretos
     */
    @Test
    void shouldFindAllCategoriesByUserId() {
        // Arrange
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        TransactionCategory category1 = new TransactionCategory();
        category1.setId(UUID.randomUUID());
        category1.setName("Alimentação");
        category1.setUser(user);

        TransactionCategory category2 = new TransactionCategory();
        category2.setId(UUID.randomUUID());
        category2.setName("Transporte");
        category2.setUser(user);

        TransactionCategory category3 = new TransactionCategory();
        category3.setId(UUID.randomUUID());
        category3.setName("Lazer");
        category3.setUser(user);

        List<TransactionCategory> categories = List.of(category1, category2, category3);

        when(categoryRepository.findByUserId(userId))
                .thenReturn(categories);

        // Act
        List<TransactionCategoryDto> result = categoryService.findAllByUserId(userId);

        // Assert
        assertNotNull(result, "O resultado não deve ser nulo");
        assertEquals(3, result.size(), "Deve retornar 3 categorias");

        assertEquals("Alimentação", result.get(0).getName());
        assertEquals("Transporte", result.get(1).getName());
        assertEquals("Lazer", result.get(2).getName());

        // Verifica que o repositório foi chamado
        verify(categoryRepository, times(1)).findByUserId(userId);
    }

    /**
     * Teste 7: Busca de Todas as Categorias Retorna Lista Vazia
     * <p>
     * Objetivo: Garantir que quando um usuário não tem categorias,
     * uma lista vazia é retornada (não null).
     * <p>
     * Cenário:
     * - Usuário não possui categorias cadastradas
     * - Busca todas as categorias do usuário
     * <p>
     * Resultado esperado:
     * - categoryRepository.findByUserId é chamado
     * - Lista vazia é retornada
     */
    @Test
    void shouldReturnEmptyListWhenUserHasNoCategories() {
        // Arrange
        UUID userId = UUID.randomUUID();

        when(categoryRepository.findByUserId(userId))
                .thenReturn(List.of());

        // Act
        List<TransactionCategoryDto> result = categoryService.findAllByUserId(userId);

        // Assert
        assertNotNull(result, "O resultado não deve ser nulo");
        assertTrue(result.isEmpty(), "A lista deve estar vazia");

        // Verifica que o repositório foi chamado
        verify(categoryRepository, times(1)).findByUserId(userId);
    }
}
