package org.azdev.barber_book.services;

import org.azdev.barber_book.dtos.WhatsAppStatusResponse;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.models.WhatsAppSettings;
import org.azdev.barber_book.repositories.WhatsAppSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatsAppServiceTest {

    private static final String TENANT_SLUG = "my-shop";
    private static final String INSTANCE_NAME = "whatsapp_my_shop";

    @Mock
    private WhatsAppSettingsRepository settingsRepository;

    @Mock
    private TenantService tenantService;

    @Mock
    private EvolutionApiClient evolutionApiClient;

    @InjectMocks
    private WhatsAppService whatsAppService;

    @Test
    void generateQrCodeCreatesInstanceAndKeepsItDisconnectedUntilPaired() {
        Tenant tenant = tenant();
        when(tenantService.getCurrentTenant()).thenReturn(tenant);
        when(settingsRepository.findByTenantId(tenant.getId())).thenReturn(Optional.empty());
        when(settingsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(evolutionApiClient.createInstance(INSTANCE_NAME)).thenReturn("qr-code");

        String qrCode = whatsAppService.generateQrCode();

        assertThat(qrCode).isEqualTo("qr-code");
        verify(evolutionApiClient).createInstance(INSTANCE_NAME);
        verify(settingsRepository, org.mockito.Mockito.times(1)).save(org.mockito.ArgumentMatchers.argThat(
                settings -> INSTANCE_NAME.equals(settings.getInstanceName()) && !settings.isConnected()));
    }

    @Test
    void generateQrCodeReconnectsExistingInstance() {
        Tenant tenant = tenant();
        WhatsAppSettings settings = settings(tenant);
        when(tenantService.getCurrentTenant()).thenReturn(tenant);
        when(settingsRepository.findByTenantId(tenant.getId())).thenReturn(Optional.of(settings));
        when(evolutionApiClient.connectInstance(INSTANCE_NAME)).thenReturn("new-qr-code");

        String qrCode = whatsAppService.generateQrCode();

        assertThat(qrCode).isEqualTo("new-qr-code");
        assertThat(settings.isConnected()).isFalse();
        verify(evolutionApiClient).connectInstance(INSTANCE_NAME);
    }

    @Test
    void getSettingsRefreshesConnectionStateFromEvolutionApi() {
        Tenant tenant = tenant();
        WhatsAppSettings settings = settings(tenant);
        when(tenantService.getCurrentTenant()).thenReturn(tenant);
        when(settingsRepository.findByTenantId(tenant.getId())).thenReturn(Optional.of(settings));
        when(evolutionApiClient.isInstanceConnected(INSTANCE_NAME)).thenReturn(true);

        WhatsAppStatusResponse response = whatsAppService.getSettings();

        assertThat(response.connected()).isTrue();
        verify(settingsRepository).save(settings);
    }

    @Test
    void disconnectPreservesInstanceForFutureReconnect() {
        Tenant tenant = tenant();
        WhatsAppSettings settings = settings(tenant);
        when(tenantService.getCurrentTenant()).thenReturn(tenant);
        when(settingsRepository.findByTenantId(tenant.getId())).thenReturn(Optional.of(settings));

        whatsAppService.disconnect();

        assertThat(settings.isConnected()).isFalse();
        assertThat(settings.getInstanceName()).isEqualTo(INSTANCE_NAME);
        verify(evolutionApiClient).disconnectInstance(INSTANCE_NAME);
        verify(settingsRepository).save(settings);
    }

    private Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setSlug(TENANT_SLUG);
        return tenant;
    }

    private WhatsAppSettings settings(Tenant tenant) {
        WhatsAppSettings settings = new WhatsAppSettings();
        settings.setTenant(tenant);
        settings.setInstanceName(INSTANCE_NAME);
        settings.setConnected(false);
        return settings;
    }
}
