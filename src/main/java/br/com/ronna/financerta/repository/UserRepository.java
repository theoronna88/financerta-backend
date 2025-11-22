package br.com.ronna.financerta.repository;

import br.com.ronna.financerta.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    boolean existsByPhone(@NotBlank(message = "Telefone é obrigatório") @Size(min = 10, max = 15, message = "Telefone deve ter entre 10 e 15 caracteres") String phone);
}
