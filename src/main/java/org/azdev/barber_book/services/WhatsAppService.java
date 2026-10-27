package org.azdev.barber_book.services;

import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.dtos.WhatsAppSettingsRequest;
import org.azdev.barber_book.dtos.WhatsAppStatusResponse;
import org.azdev.barber_book.exception.NotFoundException;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.models.WhatsAppSettings;
import org.azdev.barber_book.repositories.WhatsAppSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private final WhatsAppSettingsRepository settingsRepository;
    private final TenantService tenantService;
    private final EvolutionApiClient evolutionApiClient;

    @Transactional
    public WhatsAppStatusResponse getSettings() {
        Tenant tenant = tenantService.getCurrentTenant();

        var existingSettings = settingsRepository.findByTenantId(tenant.getId());
        WhatsAppSettings settings = existingSettings.orElseGet(() -> createDefaultSettings(tenant));

        if (settings.getInstanceName() != null) {
            settings.setConnected(evolutionApiClient.isInstanceConnected(settings.getInstanceName()));
            settingsRepository.save(settings);
        } else if (existingSettings.isEmpty()) {
            settingsRepository.save(settings);
        }

        return new WhatsAppStatusResponse(
                settings.isConnected(),
                settings.getInstanceName(),
                settings.isReminderEnabled(),
                settings.isDailySummaryEnabled()
        );
    }

    @Transactional
    public void updateSettings(WhatsAppSettingsRequest request) {
        Tenant tenant = tenantService.getCurrentTenant();
        WhatsAppSettings settings = settingsRepository.findByTenantId(tenant.getId())
                .orElseGet(() -> createDefaultSettings(tenant));

        settings.setReminderEnabled(request.isReminderEnabled());
        settings.setDailySummaryEnabled(request.isDailySummaryEnabled());
        settingsRepository.save(settings);
    }

    @Transactional
    public String generateQrCode() {
        Tenant tenant = tenantService.getCurrentTenant();
        WhatsAppSettings settings = settingsRepository.findByTenantId(tenant.getId())
                .orElseGet(() -> createDefaultSettings(tenant));

        String instanceName = settings.getInstanceName();
        String qrCode;
        if (instanceName == null) {
            instanceName = "whatsapp_" + tenant.getSlug().replace("-", "_");
            qrCode = evolutionApiClient.createInstance(instanceName);
        } else {
            qrCode = evolutionApiClient.connectInstance(instanceName);
        }

        settings.setInstanceName(instanceName);
        settings.setConnected(false);
        settingsRepository.save(settings);

        return qrCode;
    }

    @Transactional
    public void disconnect() {
        Tenant tenant = tenantService.getCurrentTenant();
        WhatsAppSettings settings = settingsRepository.findByTenantId(tenant.getId())
                .orElseThrow(() -> new NotFoundException("Nenhum WhatsApp conectado"));

        String instanceName = settings.getInstanceName();
        if (instanceName == null) {
            throw new NotFoundException("Nenhuma instância WhatsApp configurada");
        }

        evolutionApiClient.disconnectInstance(instanceName);
        settings.setConnected(false);
        settingsRepository.save(settings);
    }

    private WhatsAppSettings createDefaultSettings(Tenant tenant) {
        WhatsAppSettings settings = new WhatsAppSettings();
        settings.setTenant(tenant);
        settings.setConnected(false);
        return settings;
    }
}