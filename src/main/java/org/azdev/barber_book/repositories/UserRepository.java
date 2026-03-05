package org.azdev.barber_book.repositories;

import org.azdev.barber_book.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {
    // Usado pelo UserDetailsService do Spring Security
    Optional<User> findByEmail(String email);

    // Usado no painel de admin para listar os barbeiros da loja
    List<User> findAllByTenantId(UUID tenantId);
}
