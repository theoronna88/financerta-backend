package br.com.ronna.financerta.service.impl;

import br.com.ronna.financerta.dto.AuthResponse;
import br.com.ronna.financerta.dto.LoginRequest;
import br.com.ronna.financerta.dto.RegisterRequest;
import br.com.ronna.financerta.enums.UserRole;
import br.com.ronna.financerta.exception.EmailAlreadyExistsException;
import br.com.ronna.financerta.exception.InvalidCredentialsException;
import br.com.ronna.financerta.exception.PhoneAlreadyExistsException;
import br.com.ronna.financerta.model.User;
import br.com.ronna.financerta.repository.UserRepository;
import br.com.ronna.financerta.security.JwtProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para AuthServiceImpl.
 * <p>
 * Esta classe implementa os 4 testes críticos especificados no plano de testes:
 * 1. Registro de usuário com sucesso (Happy Path)
 * 2. Falha ao registrar com e-mail duplicado (Validação)
 * 3. Login com sucesso (Happy Path)
 * 4. Falha de login com credenciais inválidas (Segurança)
 * <p>
 * A autenticação é a porta de entrada do sistema e precisa ser à prova de falhas.
 * Estes testes garantem que:
 * - Senhas são criptografadas adequadamente
 * - Validações de e-mail e telefone duplicados funcionam
 * - Tokens JWT são gerados corretamente
 * - Exceções de segurança são tratadas apropriadamente
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    /**
     * Teste 1: Registro de Usuário com Sucesso (Happy Path)
     * <p>
     * Objetivo: Garantir que um novo usuário pode ser registrado com sucesso no sistema.
     * <p>
     * Cenário:
     * - E-mail não existe no sistema
     * - Telefone não existe no sistema
     * - Todos os dados são válidos
     * <p>
     * Resultado esperado:
     * - passwordEncoder.encode é chamado para criptografar a senha
     * - userRepository.save é chamado com os dados corretos
     * - jwtProvider.generateToken é chamado para gerar o token
     * - AuthResponse é retornado com todos os dados do usuário e token
     * - Usuário é criado com role USER e enabled true
     */
    @Test
    void shouldRegisterUserSuccessfully() {
        // Arrange (Arrumar)
        RegisterRequest request = new RegisterRequest(
                "João Silva",
                "joao@teste.com",
                "11999887766",
                "senha123"
        );

        // Simula que e-mail e telefone não existem
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(request.getPhone())).thenReturn(false);

        // Simula a criptografia da senha
        String encodedPassword = "senha_criptografada_hash";
        when(passwordEncoder.encode(request.getPassword())).thenReturn(encodedPassword);

        // Simula o salvamento do usuário - retorna o usuário com ID gerado
        UUID userId = UUID.randomUUID();
        User savedUser = new User();
        savedUser.setId(userId);
        savedUser.setName(request.getName());
        savedUser.setEmail(request.getEmail());
        savedUser.setPhone(request.getPhone());
        savedUser.setPassword(encodedPassword);
        savedUser.setRole(UserRole.USER);
        savedUser.setEnabled(true);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Simula a geração do token JWT
        String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
        when(jwtProvider.generateToken(savedUser)).thenReturn(jwtToken);

        // Act (Agir)
        AuthResponse response = authService.register(request);

        // Assert (Afirmar)
        assertNotNull(response, "A resposta não deve ser nula");
        assertEquals(jwtToken, response.getToken(), "O token deve ser o gerado pelo JwtProvider");
        assertEquals(userId, response.getUserId(), "O userId deve corresponder");
        assertEquals(request.getName(), response.getName(), "O nome deve corresponder");
        assertEquals(request.getEmail(), response.getEmail(), "O e-mail deve corresponder");
        assertEquals(request.getPhone(), response.getPhone(), "O telefone deve corresponder");
        assertEquals(UserRole.USER.name(), response.getRole(), "A role deve ser USER");

        // Verifica que as validações foram chamadas
        verify(userRepository, times(1)).existsByEmail(request.getEmail());
        verify(userRepository, times(1)).existsByPhone(request.getPhone());

        // Verifica que a senha foi criptografada
        verify(passwordEncoder, times(1)).encode(request.getPassword());

        // Captura o usuário que foi salvo para verificar os dados
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals(request.getName(), capturedUser.getName(), "Nome do usuário salvo deve corresponder");
        assertEquals(request.getEmail(), capturedUser.getEmail(), "E-mail do usuário salvo deve corresponder");
        assertEquals(request.getPhone(), capturedUser.getPhone(), "Telefone do usuário salvo deve corresponder");
        assertEquals(encodedPassword, capturedUser.getPassword(), "Senha deve estar criptografada");
        assertEquals(UserRole.USER, capturedUser.getRole(), "Role deve ser USER");
        assertTrue(capturedUser.isEnabled(), "Usuário deve estar habilitado");

        // Verifica que o token foi gerado
        verify(jwtProvider, times(1)).generateToken(savedUser);
    }

    /**
     * Teste 2: Falha ao Registrar com E-mail Duplicado (Validação)
     * <p>
     * Objetivo: Garantir que o sistema impede o registro de usuários com e-mail já existente.
     * <p>
     * Cenário:
     * - E-mail já existe no sistema
     * - Tentativa de registro com e-mail duplicado
     * <p>
     * Resultado esperado:
     * - EmailAlreadyExistsException é lançada
     * - Mensagem da exceção indica que o e-mail já está em uso
     * - userRepository.save NÃO é chamado
     * - passwordEncoder.encode NÃO é chamado
     */
    @Test
    void shouldThrowExceptionWhenRegisteringWithExistingEmail() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "Maria Santos",
                "maria@teste.com",
                "11988776655",
                "senha456"
        );

        // Simula que o e-mail já existe
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // Act & Assert
        EmailAlreadyExistsException exception = assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(request),
                "Deve lançar EmailAlreadyExistsException"
        );

        assertEquals("Email já está em uso", exception.getMessage(),
                "Mensagem da exceção deve ser específica");

        // Verifica que existsByEmail foi chamado
        verify(userRepository, times(1)).existsByEmail(request.getEmail());

        // Verifica que existsByPhone NÃO foi chamado (falha na primeira validação)
        verify(userRepository, never()).existsByPhone(anyString());

        // Verifica que a senha NÃO foi criptografada
        verify(passwordEncoder, never()).encode(anyString());

        // Verifica que o usuário NÃO foi salvo
        verify(userRepository, never()).save(any(User.class));

        // Verifica que o token NÃO foi gerado
        verify(jwtProvider, never()).generateToken(any(User.class));
    }

    /**
     * Teste 2.1: Falha ao Registrar com Telefone Duplicado (Validação)
     * <p>
     * Objetivo: Garantir que o sistema impede o registro de usuários com telefone já existente.
     * <p>
     * Cenário:
     * - E-mail não existe (validação passa)
     * - Telefone já existe no sistema
     * - Tentativa de registro com telefone duplicado
     * <p>
     * Resultado esperado:
     * - PhoneAlreadyExistsException é lançada
     * - Mensagem da exceção indica que o telefone já está em uso
     * - userRepository.save NÃO é chamado
     * - passwordEncoder.encode NÃO é chamado
     */
    @Test
    void shouldThrowExceptionWhenRegisteringWithExistingPhone() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "Carlos Oliveira",
                "carlos@teste.com",
                "11977665544",
                "senha789"
        );

        // Simula que o e-mail não existe, mas o telefone já existe
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(request.getPhone())).thenReturn(true);

        // Act & Assert
        PhoneAlreadyExistsException exception = assertThrows(
                PhoneAlreadyExistsException.class,
                () -> authService.register(request),
                "Deve lançar PhoneAlreadyExistsException"
        );

        assertEquals("Telefone já está em uso", exception.getMessage(),
                "Mensagem da exceção deve ser específica");

        // Verifica que as validações foram chamadas na ordem correta
        verify(userRepository, times(1)).existsByEmail(request.getEmail());
        verify(userRepository, times(1)).existsByPhone(request.getPhone());

        // Verifica que a senha NÃO foi criptografada
        verify(passwordEncoder, never()).encode(anyString());

        // Verifica que o usuário NÃO foi salvo
        verify(userRepository, never()).save(any(User.class));

        // Verifica que o token NÃO foi gerado
        verify(jwtProvider, never()).generateToken(any(User.class));
    }

    /**
     * Teste 3: Login com Sucesso (Happy Path)
     * <p>
     * Objetivo: Garantir que um usuário pode fazer login com credenciais válidas.
     * <p>
     * Cenário:
     * - Usuário existe no sistema
     * - Credenciais (e-mail e senha) são corretas
     * - authenticationManager autentica com sucesso
     * <p>
     * Resultado esperado:
     * - authenticationManager.authenticate é chamado com as credenciais
     * - jwtProvider.generateToken é chamado para gerar o token
     * - AuthResponse é retornado com os dados do usuário e token
     */
    @Test
    void shouldLoginSuccessfully() {
        // Arrange
        LoginRequest request = new LoginRequest("joao@teste.com", "senha123");

        // Cria um usuário mockado para simular o retorno da autenticação
        UUID userId = UUID.randomUUID();
        User authenticatedUser = new User();
        authenticatedUser.setId(userId);
        authenticatedUser.setName("João Silva");
        authenticatedUser.setEmail(request.getEmail());
        authenticatedUser.setPhone("11999887766");
        authenticatedUser.setPassword("senha_criptografada_hash");
        authenticatedUser.setRole(UserRole.USER);
        authenticatedUser.setEnabled(true);

        // Mocka o objeto Authentication retornado pelo authenticationManager
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(authenticatedUser);

        // Simula o comportamento do authenticationManager
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        // Simula a geração do token JWT
        String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.login.token";
        when(jwtProvider.generateToken(authenticatedUser)).thenReturn(jwtToken);

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertNotNull(response, "A resposta não deve ser nula");
        assertEquals(jwtToken, response.getToken(), "O token deve ser o gerado pelo JwtProvider");
        assertEquals(userId, response.getUserId(), "O userId deve corresponder");
        assertEquals(authenticatedUser.getName(), response.getName(), "O nome deve corresponder");
        assertEquals(authenticatedUser.getEmail(), response.getEmail(), "O e-mail deve corresponder");
        assertEquals(authenticatedUser.getPhone(), response.getPhone(), "O telefone deve corresponder");
        assertEquals(UserRole.USER.name(), response.getRole(), "A role deve ser USER");

        // Verifica que a autenticação foi tentada com as credenciais corretas
        ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager, times(1)).authenticate(authCaptor.capture());

        UsernamePasswordAuthenticationToken capturedAuth = authCaptor.getValue();
        assertEquals(request.getEmail(), capturedAuth.getPrincipal(),
                "O e-mail deve ser usado como principal");
        assertEquals(request.getPassword(), capturedAuth.getCredentials(),
                "A senha deve ser usada como credencial");

        // Verifica que o token foi gerado
        verify(jwtProvider, times(1)).generateToken(authenticatedUser);
    }

    /**
     * Teste 4: Falha de Login com Credenciais Inválidas (Segurança)
     * <p>
     * Objetivo: Garantir que o sistema impede login com credenciais incorretas.
     * <p>
     * Cenário:
     * - Usuário tenta fazer login com credenciais inválidas
     * - authenticationManager lança BadCredentialsException
     * <p>
     * Resultado esperado:
     * - authenticationManager.authenticate é chamado
     * - InvalidCredentialsException é lançada (conversão da BadCredentialsException)
     * - Mensagem da exceção indica credenciais inválidas
     * - jwtProvider.generateToken NÃO é chamado
     */
    @Test
    void shouldThrowExceptionWhenLoginWithInvalidCredentials() {
        // Arrange
        LoginRequest request = new LoginRequest("joao@teste.com", "senha_errada");

        // Simula que o authenticationManager lança BadCredentialsException
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request),
                "Deve lançar InvalidCredentialsException"
        );

        assertEquals("Email ou senha inválidos", exception.getMessage(),
                "Mensagem da exceção deve ser amigável ao usuário");

        // Verifica que a autenticação foi tentada
        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        // Verifica que o token NÃO foi gerado
        verify(jwtProvider, never()).generateToken(any(User.class));
    }
}
