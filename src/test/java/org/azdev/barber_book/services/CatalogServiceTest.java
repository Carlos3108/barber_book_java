package org.azdev.barber_book.services;

import org.azdev.barber_book.dtos.CatalogRequest;
import org.azdev.barber_book.dtos.CatalogResponse;
import org.azdev.barber_book.exception.BadRequestException;
import org.azdev.barber_book.exception.NotFoundException;
import org.azdev.barber_book.models.Catalog;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.repositories.CatalogRepository;
import org.azdev.barber_book.repositories.TenantRepository;
import org.azdev.barber_book.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private CatalogRepository catalogRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private CatalogService catalogService;

    private CatalogRequest request(String name) {
        return new CatalogRequest(name, new BigDecimal("50.00"), 30);
    }

    @Test
    void createServiceCreatesNewWhenNoneExists() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(catalogRepository.findByTenantIdAndNameIgnoreCase(tenantId, "Corte")).thenReturn(Optional.empty());
        when(tenantRepository.getReferenceById(tenantId)).thenReturn(tenant);
        when(catalogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CatalogResponse response = catalogService.createService(request("Corte"));

        assertThat(response.name()).isEqualTo("Corte");
        assertThat(response.active()).isTrue();
    }

    @Test
    void createServiceRejectsWhenActiveDuplicateExists() {
        UUID tenantId = UUID.randomUUID();
        Catalog existing = new Catalog();
        existing.setActive(true);

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(catalogRepository.findByTenantIdAndNameIgnoreCase(tenantId, "Corte")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> catalogService.createService(request("Corte")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createServiceReactivatesInactiveDuplicate() {
        UUID tenantId = UUID.randomUUID();
        Catalog existing = new Catalog();
        existing.setId(UUID.randomUUID());
        existing.setActive(false);
        existing.setName("Corte");

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(catalogRepository.findByTenantIdAndNameIgnoreCase(tenantId, "Corte")).thenReturn(Optional.of(existing));
        when(catalogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CatalogResponse response = catalogService.createService(request("Corte"));

        assertThat(response.active()).isTrue();
    }

    @Test
    void listMyServicesMapsRepositoryResults() {
        UUID tenantId = UUID.randomUUID();
        Catalog catalog = new Catalog();
        catalog.setId(UUID.randomUUID());
        catalog.setName("Barba");
        catalog.setPrice(new BigDecimal("20.00"));
        catalog.setDurationMinutes(20);

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(catalogRepository.findAllByTenantIdAndActiveTrue(tenantId)).thenReturn(List.of(catalog));

        List<CatalogResponse> result = catalogService.listMyServices();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Barba");
    }

    @Test
    void getMyServiceByIdThrowsWhenNotOwned() {
        UUID tenantId = UUID.randomUUID();
        UUID catalogId = UUID.randomUUID();

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(catalogRepository.findByIdAndTenantId(catalogId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getMyServiceById(catalogId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getPublicServiceByIdReturnsActiveService() {
        UUID catalogId = UUID.randomUUID();
        Catalog catalog = new Catalog();
        catalog.setId(catalogId);
        catalog.setActive(true);
        catalog.setName("Corte");
        catalog.setPrice(new BigDecimal("30.00"));
        catalog.setDurationMinutes(30);

        when(catalogRepository.findById(catalogId)).thenReturn(Optional.of(catalog));

        CatalogResponse response = catalogService.getPublicServiceById(catalogId);

        assertThat(response.id()).isEqualTo(catalogId);
    }

    @Test
    void getPublicServiceByIdThrowsWhenInactiveOrMissing() {
        UUID catalogId = UUID.randomUUID();
        when(catalogRepository.findById(catalogId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getPublicServiceById(catalogId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteServiceDeactivates() {
        UUID tenantId = UUID.randomUUID();
        UUID catalogId = UUID.randomUUID();
        Catalog catalog = new Catalog();
        catalog.setId(catalogId);
        catalog.setActive(true);

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(catalogRepository.findByIdAndTenantId(catalogId, tenantId)).thenReturn(Optional.of(catalog));
        when(catalogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        catalogService.deleteService(catalogId);

        assertThat(catalog.isActive()).isFalse();
    }

    @Test
    void updateServiceAllowsSameNameWithoutDuplicateCheck() {
        UUID tenantId = UUID.randomUUID();
        UUID catalogId = UUID.randomUUID();
        Catalog catalog = new Catalog();
        catalog.setId(catalogId);
        catalog.setName("Corte");
        catalog.setPrice(new BigDecimal("10.00"));
        catalog.setDurationMinutes(15);

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(catalogRepository.findByIdAndTenantId(catalogId, tenantId)).thenReturn(Optional.of(catalog));
        when(catalogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CatalogResponse response = catalogService.updateService(catalogId, request("Corte"));

        assertThat(response.price()).isEqualByComparingTo("50.00");
    }

    @Test
    void updateServiceRejectsRenamingToActiveDuplicateName() {
        UUID tenantId = UUID.randomUUID();
        UUID catalogId = UUID.randomUUID();
        Catalog catalog = new Catalog();
        catalog.setId(catalogId);
        catalog.setName("Corte");

        Catalog duplicate = new Catalog();
        duplicate.setActive(true);

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(catalogRepository.findByIdAndTenantId(catalogId, tenantId)).thenReturn(Optional.of(catalog));
        when(catalogRepository.findByTenantIdAndNameIgnoreCase(tenantId, "Barba")).thenReturn(Optional.of(duplicate));

        assertThatThrownBy(() -> catalogService.updateService(catalogId, request("Barba")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateServiceAllowsRenamingWhenDuplicateIsInactive() {
        UUID tenantId = UUID.randomUUID();
        UUID catalogId = UUID.randomUUID();
        Catalog catalog = new Catalog();
        catalog.setId(catalogId);
        catalog.setName("Corte");

        Catalog duplicate = new Catalog();
        duplicate.setActive(false);

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(catalogRepository.findByIdAndTenantId(catalogId, tenantId)).thenReturn(Optional.of(catalog));
        when(catalogRepository.findByTenantIdAndNameIgnoreCase(tenantId, "Barba")).thenReturn(Optional.of(duplicate));
        when(catalogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CatalogResponse response = catalogService.updateService(catalogId, request("Barba"));

        assertThat(response.name()).isEqualTo("Barba");
    }

    @Test
    void getPublicServicesBySlugThrowsWhenTenantMissing() {
        when(tenantRepository.findBySlug("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getPublicServicesBySlug("unknown"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getPublicServicesBySlugReturnsListWhenTenantExists() {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        Catalog catalog = new Catalog();
        catalog.setId(UUID.randomUUID());
        catalog.setName("Corte");
        catalog.setPrice(new BigDecimal("30.00"));
        catalog.setDurationMinutes(30);

        when(tenantRepository.findBySlug("shop")).thenReturn(Optional.of(tenant));
        when(catalogRepository.findAllActiveByTenantSlug("shop")).thenReturn(List.of(catalog));

        List<CatalogResponse> result = catalogService.getPublicServicesBySlug("shop");

        assertThat(result).hasSize(1);
    }
}
