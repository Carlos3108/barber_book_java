package org.azdev.barber_book.services;

import org.azdev.barber_book.dtos.TenantRequest;
import org.azdev.barber_book.dtos.TenantResponse;
import org.azdev.barber_book.exception.BadRequestException;
import org.azdev.barber_book.exception.NotFoundException;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.repositories.TenantRepository;
import org.azdev.barber_book.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private TenantService tenantService;

    @Test
    void getPublicInfoBySlugReturnsNameWhenTenantExists() {
        Tenant tenant = new Tenant();
        tenant.setName("Barbearia Central");

        when(tenantRepository.findBySlug("central")).thenReturn(Optional.of(tenant));

        Map<String, String> result = tenantService.getPublicInfoBySlug("central");

        assertThat(result).containsEntry("name", "Barbearia Central");
    }

    @Test
    void getPublicInfoBySlugThrowsWhenTenantMissing() {
        when(tenantRepository.findBySlug("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.getPublicInfoBySlug("unknown"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateMyBarbershopRejectsClosingBeforeOpening() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        TenantRequest request = new TenantRequest("Shop", LocalTime.of(18, 0), LocalTime.of(9, 0), "America/Sao_Paulo", 30);

        assertThatThrownBy(() -> tenantService.updateMyBarbershop(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateMyBarbershopRejectsInvalidTimezone() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        TenantRequest request = new TenantRequest("Shop", LocalTime.of(9, 0), LocalTime.of(18, 0), "Not/AZone", 30);

        assertThatThrownBy(() -> tenantService.updateMyBarbershop(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateMyBarbershopThrowsWhenTenantMissing() {
        UUID tenantId = UUID.randomUUID();
        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        TenantRequest request = new TenantRequest("Shop", LocalTime.of(9, 0), LocalTime.of(18, 0), "America/Sao_Paulo", 30);

        assertThatThrownBy(() -> tenantService.updateMyBarbershop(request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateMyBarbershopUpdatesTenantWhenValid() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TenantRequest request = new TenantRequest("Novo Nome", LocalTime.of(9, 0), LocalTime.of(18, 0), "America/Sao_Paulo", 45);

        tenantService.updateMyBarbershop(request);

        assertThat(tenant.getName()).isEqualTo("Novo Nome");
        assertThat(tenant.getSlotInterval()).isEqualTo(45);
    }

    @Test
    void getMyBarbershopSettingsReturnsCurrentTenantConfig() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Shop");
        tenant.setOpeningTime(LocalTime.of(9, 0));
        tenant.setClosingTime(LocalTime.of(18, 0));
        tenant.setTimezone("America/Sao_Paulo");
        tenant.setSlotInterval(30);

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        TenantResponse response = tenantService.getMyBarbershopSettings();

        assertThat(response.name()).isEqualTo("Shop");
        assertThat(response.slotInterval()).isEqualTo(30);
    }

    @Test
    void getMyBarbershopSettingsThrowsWhenTenantMissing() {
        UUID tenantId = UUID.randomUUID();
        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.getMyBarbershopSettings())
                .isInstanceOf(NotFoundException.class);
    }
}
