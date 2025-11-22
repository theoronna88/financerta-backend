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
import br.com.ronna.financerta.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    @Override
    public AuthResponse register(RegisterRequest request) {
        // Verificar se o email já existe
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email já está em uso");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new PhoneAlreadyExistsException("Telefone já está em uso");
        }

        // Criar novo usuário
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.USER);
        user.setPhone(request.getPhone());
        user.setEnabled(true);

        // Salvar usuário
        user = userRepository.save(user);

        // Gerar token JWT
        String token = jwtProvider.generateToken(user);

        // Retornar resposta
        return createAuthResponse(user, token);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            // Autenticar usuário
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            User user = (User) authentication.getPrincipal();

            // Gerar token JWT
            String token = jwtProvider.generateToken(user);

            // Retornar resposta
            return createAuthResponse(user, token);
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Email ou senha inválidos");
        }
    }


    private AuthResponse createAuthResponse(User user, String token) {
        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name()
        );
    }
}



