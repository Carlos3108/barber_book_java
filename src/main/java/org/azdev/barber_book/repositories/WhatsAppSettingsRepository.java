package org.azdev.barber_book.repositories;

import org.azdev.barber_book.models.WhatsAppSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WhatsAppSettingsRepository extends JpaRepository<WhatsAppSettings, UUID> {
    Optional<WhatsAppSettings> findByTenantId(UUID tenantId);
}
