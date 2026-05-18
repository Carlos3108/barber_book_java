package org.azdev.barber_book.repositories;

import org.azdev.barber_book.models.Professional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfessionalRepository extends JpaRepository<Professional, UUID> {
    List<Professional> findAllByTenantIdAndActiveTrue(UUID tenantId);
    Optional<Professional> findByTenantIdAndNameIgnoreCase(UUID tenantId, String name);
}
