package br.com.ronna.financerta.service.impl;

import br.com.ronna.financerta.dto.CreditCardDto;
import br.com.ronna.financerta.model.CreditCard;
import br.com.ronna.financerta.model.User;
import br.com.ronna.financerta.repository.CreditCardRepository;
import br.com.ronna.financerta.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para CreditCardServiceImpl (CreditCardImpl).
 * <p>
 * Esta classe implementa os testes de CRUD especificados no plano de testes:
 * 1. Busca de cartão de crédito por ID com sucesso
 * 2. Falha ao buscar cartão não encontrado ou que não pertence ao usuário
 * 3. Criação de cartão com userId correto
 * 4. Deleção de cartão com sucesso (soft delete)
 * <p>
 * Estes testes garantem que:
 * - Usuários só podem acessar seus próprios cartões (segurança)
 * - O userId do contexto autenticado é usado (não o do DTO)
 * - Soft delete é implementado corretamente
 * - Dados críticos como limite e dias de fechamento/vencimento são preservados
 */
@ExtendWith(MockitoExtension.class)
class CreditCardServiceImplTest {

    @Mock
    private CreditCardRepository creditCardRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CreditCardImpl creditCardService;

    /**
     * Teste 1: Busca de Cartão de Crédito por ID com Sucesso
     * <p>
     * Objetivo: Garantir que um cartão de crédito pode ser buscado com sucesso quando
     * o ID e o userId correspondem a um cartão existente.
     * <p>
     * Cenário:
     * - Cartão existe no sistema
     * - Cartão pertence ao usuário autenticado
     * - Busca por ID e userId
     * <p>
     * Resultado esperado:
     * - creditCardRepository.findByIdAndUserId é chamado
     * - CreditCardDto é retornado com os dados corretos
     * - Todos os campos críticos (limite, dias, nome) correspondem
     */
    @Test
    void shouldFindCreditCardByIdSuccessfully() {
        // Arrange (Arrumar)
        UUID creditCardId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CreditCard creditCard = new CreditCard();
        creditCard.setId(creditCardId);
        creditCard.setName("Cartão Platinum");
        creditCard.setClosingDay(15);
        creditCard.setDueDay(25);
        creditCard.setLimitValue(new BigDecimal("5000.00"));
        creditCard.setActive(true);
        creditCard.setCreatedAt(LocalDateTime.now());
        creditCard.setUpdatedAt(LocalDateTime.now());

        User user = new User();
        user.setId(userId);
        creditCard.setUser(user);

        when(creditCardRepository.findByIdAndUserId(creditCardId, userId))
                .thenReturn(Optional.of(creditCard));

        // Act (Agir)
        CreditCardDto result = creditCardService.findById(creditCardId, userId);

        // Assert (Afirmar)
        assertNotNull(result, "O resultado não deve ser nulo");
        assertEquals(creditCardId, result.getId(), "O ID deve corresponder");
        assertEquals("Cartão Platinum", result.getName(), "O nome deve corresponder");
        assertEquals(15, result.getClosingDay(), "O dia de fechamento deve corresponder");
        assertEquals(25, result.getDueDay(), "O dia de vencimento deve corresponder");
        assertEquals(0, new BigDecimal("5000.00").compareTo(result.getLimitValue()),
                "O limite deve corresponder");
        assertTrue(result.isActive(), "O cartão deve estar ativo");

        // Verifica que o repositório foi chamado corretamente
        verify(creditCardRepository, times(1)).findByIdAndUserId(creditCardId, userId);
    }

    /**
     * Teste 2: Falha ao Buscar Cartão Não Encontrado ou de Outro Usuário
     * <p>
     * Objetivo: Garantir que o sistema impede o acesso a cartões que não existem
     * ou que pertencem a outro usuário (segurança crítica).
     * <p>
     * Cenário:
     * - Busca por ID e userId
     * - Cartão não existe OU pertence a outro usuário
     * - Repository retorna Optional.empty()
     * <p>
     * Resultado esperado:
     * - NoSuchElementException é lançada
     * - Acesso negado a dados de outros usuários
     */
    @Test
    void shouldThrowExceptionWhenCreditCardNotFoundOrNotOwned() {
        // Arrange
        UUID creditCardId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // Simula que o cartão não foi encontrado para este usuário
        when(creditCardRepository.findByIdAndUserId(creditCardId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                NoSuchElementException.class,
                () -> creditCardService.findById(creditCardId, userId),
                "Deve lançar NoSuchElementException quando cartão não é encontrado"
        );

        // Verifica que o repositório foi chamado
        verify(creditCardRepository, times(1)).findByIdAndUserId(creditCardId, userId);
    }

    /**
     * Teste 3: Criação de Cartão de Crédito com UserId Correto
     * <p>
     * Objetivo: Garantir que ao criar um cartão, o userId do contexto autenticado
     * é usado, não o userId que pode vir no DTO (segurança crítica).
     * <p>
     * Cenário:
     * - Usuário autenticado com userId específico
     * - CreditCardDto contém dados do cartão
     * - Criação de novo cartão
     * <p>
     * Resultado esperado:
     * - userRepository.findById é chamado com o userId autenticado
     * - Cartão é criado com o User encontrado
     * - Todos os dados críticos são preservados (nome, dias, limite)
     * - createdAt e updatedAt são setados
     * - CreditCardDto é retornado com os dados salvos
     */
    @Test
    void shouldCreateCreditCardWithCorrectUserId() {
        // Arrange
        UUID authenticatedUserId = UUID.randomUUID();

        CreditCardDto inputDto = new CreditCardDto();
        inputDto.setName("Cartão Gold");
        inputDto.setClosingDay(10);
        inputDto.setDueDay(20);
        inputDto.setLimitValue(new BigDecimal("3000.00"));

        // Cria o usuário autenticado
        User authenticatedUser = new User();
        authenticatedUser.setId(authenticatedUserId);
        authenticatedUser.setName("Usuário Autenticado");

        when(userRepository.findById(authenticatedUserId))
                .thenReturn(Optional.of(authenticatedUser));

        // Simula o salvamento do cartão
        when(creditCardRepository.save(any(CreditCard.class))).thenAnswer(invocation -> {
            CreditCard card = invocation.getArgument(0);
            card.setId(UUID.randomUUID()); // Simula a geração do ID
            return card;
        });

        // Act
        CreditCardDto result = creditCardService.save(inputDto, authenticatedUserId);

        // Assert
        assertNotNull(result, "O resultado não deve ser nulo");
        assertEquals("Cartão Gold", result.getName(), "O nome deve corresponder");
        assertEquals(10, result.getClosingDay(), "O dia de fechamento deve corresponder");
        assertEquals(20, result.getDueDay(), "O dia de vencimento deve corresponder");
        assertEquals(0, new BigDecimal("3000.00").compareTo(result.getLimitValue()),
                "O limite deve corresponder");

        // Verifica que o usuário foi buscado com o userId autenticado
        verify(userRepository, times(1)).findById(authenticatedUserId);

        // Captura o cartão que foi salvo para verificar os dados críticos
        ArgumentCaptor<CreditCard> cardCaptor = ArgumentCaptor.forClass(CreditCard.class);
        verify(creditCardRepository, times(1)).save(cardCaptor.capture());

        CreditCard savedCard = cardCaptor.getValue();
        assertEquals("Cartão Gold", savedCard.getName(), "Nome deve corresponder");
        assertEquals(10, savedCard.getClosingDay(), "Dia de fechamento deve corresponder");
        assertEquals(20, savedCard.getDueDay(), "Dia de vencimento deve corresponder");
        assertEquals(0, new BigDecimal("3000.00").compareTo(savedCard.getLimitValue()),
                "Limite deve corresponder");
        assertNotNull(savedCard.getCreatedAt(), "CreatedAt deve ser setado");
        assertNotNull(savedCard.getUpdatedAt(), "UpdatedAt deve ser setado");
        assertEquals(authenticatedUser, savedCard.getUser(),
                "User deve ser o usuário autenticado");
    }

    /**
     * Teste 3.1: Falha ao Criar Cartão Quando Usuário Não Encontrado
     * <p>
     * Objetivo: Garantir que o sistema lança exceção quando tenta criar
     * um cartão para um usuário que não existe.
     * <p>
     * Cenário:
     * - userId não corresponde a nenhum usuário no sistema
     * - userRepository.findById retorna Optional.empty()
     * <p>
     * Resultado esperado:
     * - NoSuchElementException é lançada
     * - creditCardRepository.save NÃO é chamado
     */
    @Test
    void shouldThrowExceptionWhenCreatingCreditCardForNonExistentUser() {
        // Arrange
        UUID nonExistentUserId = UUID.randomUUID();

        CreditCardDto inputDto = new CreditCardDto();
        inputDto.setName("Cartão Teste");
        inputDto.setClosingDay(5);
        inputDto.setDueDay(15);
        inputDto.setLimitValue(new BigDecimal("1000.00"));

        // Simula que o usuário não foi encontrado
        when(userRepository.findById(nonExistentUserId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                NoSuchElementException.class,
                () -> creditCardService.save(inputDto, nonExistentUserId),
                "Deve lançar NoSuchElementException quando usuário não existe"
        );

        // Verifica que o repositório de usuário foi consultado
        verify(userRepository, times(1)).findById(nonExistentUserId);

        // Verifica que o cartão NÃO foi salvo
        verify(creditCardRepository, never()).save(any(CreditCard.class));
    }

    /**
     * Teste 4: Deleção de Cartão de Crédito com Sucesso (Soft Delete)
     * <p>
     * Objetivo: Garantir que ao deletar um cartão, ele é desativado (soft delete)
     * ao invés de ser removido do banco de dados.
     * <p>
     * Cenário:
     * - Cartão existe e pertence ao usuário
     * - Usuário solicita deleção do cartão
     * <p>
     * Resultado esperado:
     * - creditCardRepository.findByIdAndUserId é chamado
     * - Cartão tem active setado para false
     * - creditCardRepository.save é chamado com o cartão modificado
     * - creditCardRepository.delete NÃO é chamado (soft delete)
     */
    @Test
    void shouldDeleteCreditCardSuccessfully() {
        // Arrange
        UUID creditCardId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CreditCard creditCard = new CreditCard();
        creditCard.setId(creditCardId);
        creditCard.setName("Cartão para Deletar");
        creditCard.setClosingDay(12);
        creditCard.setDueDay(22);
        creditCard.setLimitValue(new BigDecimal("2000.00"));
        creditCard.setActive(true);
        creditCard.setCreatedAt(LocalDateTime.now().minusDays(30));
        creditCard.setUpdatedAt(LocalDateTime.now().minusDays(10));

        User user = new User();
        user.setId(userId);
        creditCard.setUser(user);

        when(creditCardRepository.findByIdAndUserId(creditCardId, userId))
                .thenReturn(Optional.of(creditCard));

        // Act
        creditCardService.delete(creditCardId, userId);

        // Assert
        // Verifica que o cartão foi buscado
        verify(creditCardRepository, times(1)).findByIdAndUserId(creditCardId, userId);

        // Captura o cartão que foi salvo após a "deleção"
        ArgumentCaptor<CreditCard> cardCaptor = ArgumentCaptor.forClass(CreditCard.class);
        verify(creditCardRepository, times(1)).save(cardCaptor.capture());

        CreditCard deletedCard = cardCaptor.getValue();
        assertFalse(deletedCard.isActive(), "Cartão deve estar inativo (soft delete)");

        // Verifica que delete físico NÃO foi chamado
        verify(creditCardRepository, never()).delete(any(CreditCard.class));
        verify(creditCardRepository, never()).deleteById(any(UUID.class));
    }

    /**
     * Teste 4.1: Falha ao Deletar Cartão Não Encontrado ou de Outro Usuário
     * <p>
     * Objetivo: Garantir que o sistema impede a deleção de cartões que não existem
     * ou que pertencem a outro usuário (segurança).
     * <p>
     * Cenário:
     * - Busca por ID e userId
     * - Cartão não existe OU pertence a outro usuário
     * - Repository retorna Optional.empty()
     * <p>
     * Resultado esperado:
     * - NoSuchElementException é lançada
     * - creditCardRepository.save NÃO é chamado
     */
    @Test
    void shouldThrowExceptionWhenDeletingNonExistentOrNotOwnedCreditCard() {
        // Arrange
        UUID creditCardId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // Simula que o cartão não foi encontrado para este usuário
        when(creditCardRepository.findByIdAndUserId(creditCardId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                NoSuchElementException.class,
                () -> creditCardService.delete(creditCardId, userId),
                "Deve lançar NoSuchElementException quando cartão não é encontrado"
        );

        // Verifica que o repositório foi consultado
        verify(creditCardRepository, times(1)).findByIdAndUserId(creditCardId, userId);

        // Verifica que save NÃO foi chamado
        verify(creditCardRepository, never()).save(any(CreditCard.class));
    }

    /**
     * Teste 5: Atualização de Cartão de Crédito com Sucesso
     * <p>
     * Objetivo: Garantir que um cartão pode ser atualizado corretamente,
     * preservando a segurança de userId.
     * <p>
     * Cenário:
     * - Cartão existe e pertence ao usuário
     * - Usuário atualiza dados do cartão (nome, limite, dias)
     * <p>
     * Resultado esperado:
     * - Cartão é atualizado com os novos valores
     * - userId permanece o mesmo (não pode ser alterado)
     * - creditCardRepository.save é chamado com os dados atualizados
     */
    @Test
    void shouldUpdateCreditCardSuccessfully() {
        // Arrange
        UUID creditCardId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CreditCard existingCard = new CreditCard();
        existingCard.setId(creditCardId);
        existingCard.setName("Cartão Antigo");
        existingCard.setClosingDay(5);
        existingCard.setDueDay(15);
        existingCard.setLimitValue(new BigDecimal("1000.00"));
        existingCard.setActive(true);

        User user = new User();
        user.setId(userId);
        existingCard.setUser(user);

        when(creditCardRepository.findByIdAndUserId(creditCardId, userId))
                .thenReturn(Optional.of(existingCard));

        when(creditCardRepository.save(any(CreditCard.class))).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });

        CreditCardDto updateDto = new CreditCardDto();
        updateDto.setName("Cartão Atualizado");
        updateDto.setClosingDay(20);
        updateDto.setDueDay(30);
        updateDto.setLimitValue(new BigDecimal("8000.00"));

        // Act
        CreditCardDto result = creditCardService.update(creditCardId, updateDto, userId);

        // Assert
        assertNotNull(result, "O resultado não deve ser nulo");
        assertEquals("Cartão Atualizado", result.getName(), "Nome deve ser atualizado");
        assertEquals(20, result.getClosingDay(), "Dia de fechamento deve ser atualizado");
        assertEquals(30, result.getDueDay(), "Dia de vencimento deve ser atualizado");
        assertEquals(0, new BigDecimal("8000.00").compareTo(result.getLimitValue()),
                "Limite deve ser atualizado");

        // Verifica que o cartão foi buscado
        verify(creditCardRepository, times(1)).findByIdAndUserId(creditCardId, userId);

        // Captura o cartão salvo
        ArgumentCaptor<CreditCard> cardCaptor = ArgumentCaptor.forClass(CreditCard.class);
        verify(creditCardRepository, times(1)).save(cardCaptor.capture());

        CreditCard updatedCard = cardCaptor.getValue();
        assertEquals("Cartão Atualizado", updatedCard.getName());
        assertEquals(20, updatedCard.getClosingDay());
        assertEquals(30, updatedCard.getDueDay());
        assertEquals(0, new BigDecimal("8000.00").compareTo(updatedCard.getLimitValue()));
    }
}
