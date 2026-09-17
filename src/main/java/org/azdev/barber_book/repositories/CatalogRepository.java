package org.azdev.barber_book.repositories;

import org.azdev.barber_book.models.Catalog;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogRepository extends JpaRepository<Catalog, UUID> {

    List<Catalog> findAllByTenantIdAndActiveTrue(UUID tenantId);

    @Query("""
        SELECT c FROM Catalog c
        WHERE c.active = true
          AND c.tenant.slug = :slug
    """)
    List<Catalog> findAllActiveByTenantSlug(@Param("slug") String slug);

    Optional<Catalog> findByTenantIdAndNameIgnoreCase(UUID tenantId, String name);

    Optional<Catalog> findByIdAndTenantId(UUID id, UUID tenantId);
}