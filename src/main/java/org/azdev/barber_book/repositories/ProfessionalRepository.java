package org.azdev.barber_book.repositories;

import org.azdev.barber_book.models.Professional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfessionalRepository extends JpaRepository<Professional, UUID> {

    @EntityGraph(attributePaths = "services")
    List<Professional> findAllByTenantIdAndActiveTrue(UUID tenantId);
    Optional<Professional> findByTenantIdAndNameIgnoreCase(UUID tenantId, String name);
    Optional<Professional> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("""
        SELECT p FROM Professional p
        JOIN FETCH p.tenant t
        WHERE p.id = :id
    """)
    Optional<Professional> findByIdWithTenant(@Param("id") UUID id);
}
