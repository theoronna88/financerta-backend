package br.com.ronna.financerta.utils;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;
import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UUID getUserId (Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Principal não pode ser nulo para obter o ID do usuário.");
        }
        if (principal instanceof UsernamePasswordAuthenticationToken token) {
            Object principalObj = token.getPrincipal();

            if (principalObj instanceof br.com.ronna.financerta.model.User user) {
                return user.getId();
            }
        }
        throw new IllegalStateException("Não foi possível extrair o ID do usuário do Principal.");
    }
}
