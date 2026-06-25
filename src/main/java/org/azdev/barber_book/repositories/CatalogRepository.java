package org.azdev.barber_book.repositories;

import org.azdev.barber_book.dtos.CatalogResponse;
import org.azdev.barber_book.models.Catalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogRepository extends JpaRepository<Catalog, UUID> {

    List<Catalog> findAllByTenantIdAndActiveTrue(UUID tenantId);

    Optional<Catalog> findByTenantIdAndNameIgnoreCase(UUID tenantId, String name);

}
