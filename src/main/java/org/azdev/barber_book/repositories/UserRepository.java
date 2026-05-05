package org.azdev.barber_book.repositories;

import org.azdev.barber_book.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = "tenant")
    Optional<User> findByEmail(String email);

    List<User> findAllByTenantId(UUID tenantId);

}
