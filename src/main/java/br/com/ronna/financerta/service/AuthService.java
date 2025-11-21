package br.com.ronna.financerta.service;

import br.com.ronna.financerta.dto.AuthResponse;
import br.com.ronna.financerta.dto.LoginRequest;
import br.com.ronna.financerta.dto.RegisterRequest;
import br.com.ronna.financerta.enums.UserRole;
import br.com.ronna.financerta.exception.EmailAlreadyExistsException;
import br.com.ronna.financerta.exception.InvalidCredentialsException;
import br.com.ronna.financerta.model.User;
import br.com.ronna.financerta.repository.UserRepository;
import br.com.ronna.financerta.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


public interface AuthService {


    @Transactional
    public AuthResponse register(RegisterRequest request);

    public AuthResponse login(LoginRequest request);
}

