package org.azdev.barber_book.repositories;

import org.azdev.barber_book.models.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findBySlug(String slug);

    @Query("SELECT t FROM Tenant t WHERE t.planStatus IN ('TRIAL', 'ACTIVE') AND t.trialExpiresAt < :currentDate")
    List<Tenant> findExpiredTenants(@Param("currentDate") LocalDateTime currentDate);
}
