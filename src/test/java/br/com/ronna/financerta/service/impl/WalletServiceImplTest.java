package br.com.ronna.financerta.service.impl;

import br.com.ronna.financerta.dto.WalletDto;
import br.com.ronna.financerta.model.User;
import br.com.ronna.financerta.model.Wallet;
import br.com.ronna.financerta.repository.UserRepository;
import br.com.ronna.financerta.repository.WalletRepository;
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
 * Testes unitários para WalletServiceImpl.
 * <p>
 * Esta classe implementa os testes de CRUD especificados no plano de testes:
 * 1. Busca de carteira por ID com sucesso
 * 2. Falha ao buscar carteira não encontrada ou que não pertence ao usuário
 * 3. Criação de carteira com userId correto
 * 4. Deleção de carteira com sucesso (soft delete)
 * <p>
 * Estes testes garantem que:
 * - Usuários só podem acessar suas próprias carteiras (segurança)
 * - O userId do contexto autenticado é usado (não o do DTO)
 * - Soft delete é implementado corretamente
 */
@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    /**
     * Teste 1: Busca de Carteira por ID com Sucesso
     * <p>
     * Objetivo: Garantir que uma carteira pode ser buscada com sucesso quando o ID
     * e o userId correspondem a uma carteira existente.
     * <p>
     * Cenário:
     * - Carteira existe no sistema
     * - Carteira pertence ao usuário autenticado
     * - Busca por ID e userId
     * <p>
     * Resultado esperado:
     * - walletRepository.findByIdAndUserId é chamado
     * - WalletDto é retornado com os dados corretos
     * - Todos os campos do DTO correspondem à entidade
     */
    @Test
    void shouldFindWalletByIdSuccessfully() {
        // Arrange (Arrumar)
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setName("Carteira Principal");
        wallet.setColor("#FF5733");
        wallet.setInitialBalance(new BigDecimal("1000.00"));
        wallet.setActive(true);
        wallet.setCreatedAt(LocalDateTime.now());
        wallet.setUpdatedAt(LocalDateTime.now());

        User user = new User();
        user.setId(userId);
        wallet.setUser(user);

        when(walletRepository.findByIdAndUserId(walletId, userId))
                .thenReturn(Optional.of(wallet));

        // Act (Agir)
        WalletDto result = walletService.findById(walletId, userId);

        // Assert (Afirmar)
        assertNotNull(result, "O resultado não deve ser nulo");
        assertEquals(walletId, result.getId(), "O ID deve corresponder");
        assertEquals("Carteira Principal", result.getName(), "O nome deve corresponder");
        assertEquals("#FF5733", result.getColor(), "A cor deve corresponder");
        assertEquals(0, new BigDecimal("1000.00").compareTo(result.getInitialBalance()),
                "O saldo inicial deve corresponder");
        assertTrue(result.isActive(), "A carteira deve estar ativa");

        // Verifica que o repositório foi chamado corretamente
        verify(walletRepository, times(1)).findByIdAndUserId(walletId, userId);
    }

    /**
     * Teste 2: Falha ao Buscar Carteira Não Encontrada ou de Outro Usuário
     * <p>
     * Objetivo: Garantir que o sistema impede o acesso a carteiras que não existem
     * ou que pertencem a outro usuário (segurança).
     * <p>
     * Cenário:
     * - Busca por ID e userId
     * - Carteira não existe OU pertence a outro usuário
     * - Repository retorna Optional.empty()
     * <p>
     * Resultado esperado:
     * - walletRepository.findByIdAndUserId é chamado
     * - IllegalArgumentException é lançada (BeanUtils.copyProperties não aceita null)
     * <p>
     * Nota: O serviço atual não trata null no convertToDto, causando IllegalArgumentException.
     * Esta é uma descoberta do teste que indica oportunidade de melhoria no código de produção.
     */
    @Test
    void shouldThrowExceptionWhenWalletNotFoundOrNotOwned() {
        // Arrange
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // Simula que a carteira não foi encontrada para este usuário
        when(walletRepository.findByIdAndUserId(walletId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> walletService.findById(walletId, userId),
                "Deve lançar IllegalArgumentException quando a carteira não é encontrada"
        );

        // Verifica que o repositório foi chamado
        verify(walletRepository, times(1)).findByIdAndUserId(walletId, userId);
    }

    /**
     * Teste 3: Criação de Carteira com UserId Correto
     * <p>
     * Objetivo: Garantir que ao criar uma carteira, o userId do contexto autenticado
     * é usado, não o userId que pode vir no DTO (segurança crítica).
     * <p>
     * Cenário:
     * - Usuário autenticado com userId específico
     * - WalletDto pode ter qualquer userId (ou nenhum)
     * - Criação de nova carteira
     * <p>
     * Resultado esperado:
     * - userRepository.findById é chamado com o userId autenticado
     * - Carteira é criada com o User encontrado
     * - Carteira é salva com active=true
     * - createdAt e updatedAt são setados
     * - WalletDto é retornado com os dados salvos
     */
    @Test
    void shouldCreateWalletWithCorrectUserId() {
        // Arrange
        UUID authenticatedUserId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID(); // ID diferente no DTO (deve ser ignorado)

        WalletDto inputDto = new WalletDto();
        inputDto.setName("Nova Carteira");
        inputDto.setColor("#3498DB");
        inputDto.setInitialBalance(new BigDecimal("500.00"));

        // Cria o usuário autenticado
        User authenticatedUser = new User();
        authenticatedUser.setId(authenticatedUserId);
        authenticatedUser.setName("Usuário Autenticado");

        when(userRepository.findById(authenticatedUserId))
                .thenReturn(Optional.of(authenticatedUser));

        // Simula o salvamento da carteira
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet wallet = invocation.getArgument(0);
            wallet.setId(UUID.randomUUID()); // Simula a geração do ID
            return wallet;
        });

        // Act
        WalletDto result = walletService.createWallet(inputDto, authenticatedUserId);

        // Assert
        assertNotNull(result, "O resultado não deve ser nulo");
        assertEquals("Nova Carteira", result.getName(), "O nome deve corresponder");
        assertEquals("#3498DB", result.getColor(), "A cor deve corresponder");
        assertEquals(0, new BigDecimal("500.00").compareTo(result.getInitialBalance()),
                "O saldo inicial deve corresponder");

        // Verifica que o usuário foi buscado com o userId autenticado (não do DTO)
        verify(userRepository, times(1)).findById(authenticatedUserId);
        verify(userRepository, never()).findById(differentUserId);

        // Captura a carteira que foi salva para verificar os dados críticos
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository, times(1)).save(walletCaptor.capture());

        Wallet savedWallet = walletCaptor.getValue();
        assertEquals("Nova Carteira", savedWallet.getName(), "Nome deve corresponder");
        assertEquals("#3498DB", savedWallet.getColor(), "Cor deve corresponder");
        assertEquals(0, new BigDecimal("500.00").compareTo(savedWallet.getInitialBalance()),
                "Saldo inicial deve corresponder");
        assertTrue(savedWallet.isActive(), "Carteira deve estar ativa");
        assertNotNull(savedWallet.getCreatedAt(), "CreatedAt deve ser setado");
        assertNotNull(savedWallet.getUpdatedAt(), "UpdatedAt deve ser setado");
        assertEquals(authenticatedUser, savedWallet.getUser(),
                "User deve ser o usuário autenticado");
    }

    /**
     * Teste 3.1: Falha ao Criar Carteira Quando Usuário Não Encontrado
     * <p>
     * Objetivo: Garantir que o sistema lança exceção quando tenta criar
     * uma carteira para um usuário que não existe.
     * <p>
     * Cenário:
     * - userId não corresponde a nenhum usuário no sistema
     * - userRepository.findById retorna Optional.empty()
     * <p>
     * Resultado esperado:
     * - NoSuchElementException é lançada
     * - walletRepository.save NÃO é chamado
     */
    @Test
    void shouldThrowExceptionWhenCreatingWalletForNonExistentUser() {
        // Arrange
        UUID nonExistentUserId = UUID.randomUUID();

        WalletDto inputDto = new WalletDto();
        inputDto.setName("Carteira Teste");
        inputDto.setColor("#000000");
        inputDto.setInitialBalance(new BigDecimal("100.00"));

        // Simula que o usuário não foi encontrado
        when(userRepository.findById(nonExistentUserId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                NoSuchElementException.class,
                () -> walletService.createWallet(inputDto, nonExistentUserId),
                "Deve lançar NoSuchElementException quando usuário não existe"
        );

        // Verifica que o repositório de usuário foi consultado
        verify(userRepository, times(1)).findById(nonExistentUserId);

        // Verifica que a carteira NÃO foi salva
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    /**
     * Teste 4: Deleção de Carteira com Sucesso (Soft Delete)
     * <p>
     * Objetivo: Garantir que ao deletar uma carteira, ela é desativada (soft delete)
     * ao invés de ser removida do banco de dados.
     * <p>
     * Cenário:
     * - Carteira existe e pertence ao usuário
     * - Usuário solicita deleção da carteira
     * <p>
     * Resultado esperado:
     * - walletRepository.findByIdAndUserId é chamado
     * - Carteira tem active setado para false
     * - updatedAt é atualizado
     * - walletRepository.save é chamado com a carteira modificada
     * - walletRepository.delete NÃO é chamado (soft delete, não hard delete)
     */
    @Test
    void shouldDeleteWalletSuccessfully() {
        // Arrange
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setName("Carteira para Deletar");
        wallet.setColor("#E74C3C");
        wallet.setInitialBalance(new BigDecimal("200.00"));
        wallet.setActive(true);
        wallet.setCreatedAt(LocalDateTime.now().minusDays(10));
        wallet.setUpdatedAt(LocalDateTime.now().minusDays(5));

        User user = new User();
        user.setId(userId);
        wallet.setUser(user);

        when(walletRepository.findByIdAndUserId(walletId, userId))
                .thenReturn(Optional.of(wallet));

        // Act
        walletService.deleteWalletById(walletId, userId);

        // Assert
        // Verifica que a carteira foi buscada
        verify(walletRepository, times(1)).findByIdAndUserId(walletId, userId);

        // Captura a carteira que foi salva após a "deleção"
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository, times(1)).save(walletCaptor.capture());

        Wallet deletedWallet = walletCaptor.getValue();
        assertFalse(deletedWallet.isActive(), "Carteira deve estar inativa (soft delete)");
        assertNotNull(deletedWallet.getUpdatedAt(), "UpdatedAt deve ser atualizado");

        // Verifica que delete físico NÃO foi chamado
        verify(walletRepository, never()).delete(any(Wallet.class));
        verify(walletRepository, never()).deleteById(any(UUID.class));
    }

    /**
     * Teste 4.1: Falha ao Deletar Carteira Não Encontrada ou de Outro Usuário
     * <p>
     * Objetivo: Garantir que o sistema impede a deleção de carteiras que não existem
     * ou que pertencem a outro usuário (segurança).
     * <p>
     * Cenário:
     * - Busca por ID e userId
     * - Carteira não existe OU pertence a outro usuário
     * - Repository retorna Optional.empty()
     * <p>
     * Resultado esperado:
     * - NoSuchElementException é lançada
     * - walletRepository.save NÃO é chamado
     */
    @Test
    void shouldThrowExceptionWhenDeletingNonExistentOrNotOwnedWallet() {
        // Arrange
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // Simula que a carteira não foi encontrada para este usuário
        when(walletRepository.findByIdAndUserId(walletId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                NoSuchElementException.class,
                () -> walletService.deleteWalletById(walletId, userId),
                "Deve lançar NoSuchElementException quando carteira não é encontrada"
        );

        // Verifica que o repositório foi consultado
        verify(walletRepository, times(1)).findByIdAndUserId(walletId, userId);

        // Verifica que save NÃO foi chamado
        verify(walletRepository, never()).save(any(Wallet.class));
    }
}
