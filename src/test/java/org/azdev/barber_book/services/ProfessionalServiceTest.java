package org.azdev.barber_book.services;

import org.azdev.barber_book.dtos.ProfessionalRequest;
import org.azdev.barber_book.dtos.ProfessionalResponse;
import org.azdev.barber_book.exception.BadRequestException;
import org.azdev.barber_book.exception.NotFoundException;
import org.azdev.barber_book.exception.UnauthorizedException;
import org.azdev.barber_book.models.Catalog;
import org.azdev.barber_book.models.Professional;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.repositories.CatalogRepository;
import org.azdev.barber_book.repositories.ProfessionalRepository;
import org.azdev.barber_book.repositories.TenantRepository;
import org.azdev.barber_book.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfessionalServiceTest {

    @Mock
    private ProfessionalRepository professionalRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private CatalogRepository catalogRepository;

    @InjectMocks
    private ProfessionalService professionalService;

    @Test
    void createProfessionalCreatesNewWhenNoneExists() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(professionalRepository.findByTenantIdAndNameIgnoreCase(tenantId, "João"))
                .thenReturn(Optional.empty());
        when(tenantRepository.getReferenceById(tenantId)).thenReturn(tenant);
        when(professionalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProfessionalResponse response = professionalService.createProfessional(
                new ProfessionalRequest("João", Set.of()));

        assertThat(response.name()).isEqualTo("João");
        assertThat(response.active()).isTrue();
    }

    @Test
    void createProfessionalRejectsWhenActiveDuplicateExists() {
        UUID tenantId = UUID.randomUUID();
        Professional existing = new Professional();
        existing.setActive(true);

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(professionalRepository.findByTenantIdAndNameIgnoreCase(tenantId, "João"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> professionalService.createProfessional(new ProfessionalRequest("João", Set.of())))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createProfessionalReactivatesInactiveDuplicate() {
        UUID tenantId = UUID.randomUUID();
        Professional existing = new Professional();
        existing.setId(UUID.randomUUID());
        existing.setActive(false);
        existing.setName("João");

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(professionalRepository.findByTenantIdAndNameIgnoreCase(tenantId, "João"))
                .thenReturn(Optional.of(existing));
        when(professionalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProfessionalResponse response = professionalService.createProfessional(
                new ProfessionalRequest("João", Set.of()));

        assertThat(response.active()).isTrue();
    }

    @Test
    void createProfessionalRejectsMissingServiceIds() {
        UUID tenantId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(professionalRepository.findByTenantIdAndNameIgnoreCase(tenantId, "João"))
                .thenReturn(Optional.empty());
        when(tenantRepository.getReferenceById(tenantId)).thenReturn(tenant);
        when(catalogRepository.findAllById(Set.of(serviceId))).thenReturn(List.of());

        assertThatThrownBy(() -> professionalService.createProfessional(
                new ProfessionalRequest("João", Set.of(serviceId))))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createProfessionalRejectsServiceFromDifferentTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        Tenant otherTenant = new Tenant();
        otherTenant.setId(otherTenantId);

        Catalog foreignService = new Catalog();
        foreignService.setId(serviceId);
        foreignService.setTenant(otherTenant);

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(professionalRepository.findByTenantIdAndNameIgnoreCase(tenantId, "João"))
                .thenReturn(Optional.empty());
        when(tenantRepository.getReferenceById(tenantId)).thenReturn(tenant);
        when(catalogRepository.findAllById(Set.of(serviceId))).thenReturn(List.of(foreignService));

        assertThatThrownBy(() -> professionalService.createProfessional(
                new ProfessionalRequest("João", Set.of(serviceId))))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void createProfessionalLinksValidServiceFromSameTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        Catalog service = new Catalog();
        service.setId(serviceId);
        service.setTenant(tenant);

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(professionalRepository.findByTenantIdAndNameIgnoreCase(tenantId, "João"))
                .thenReturn(Optional.empty());
        when(tenantRepository.getReferenceById(tenantId)).thenReturn(tenant);
        when(catalogRepository.findAllById(Set.of(serviceId))).thenReturn(List.of(service));
        when(professionalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProfessionalResponse response = professionalService.createProfessional(
                new ProfessionalRequest("João", Set.of(serviceId)));

        assertThat(response.serviceIds()).containsExactly(serviceId);
    }

    @Test
    void listMyProfessionalsMapsRepositoryResults() {
        UUID tenantId = UUID.randomUUID();
        Professional professional = new Professional();
        professional.setId(UUID.randomUUID());
        professional.setName("Maria");

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(professionalRepository.findAllByTenantIdAndActiveTrue(tenantId)).thenReturn(List.of(professional));

        List<ProfessionalResponse> result = professionalService.listMyProfessionals();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Maria");
    }

    @Test
    void updateProfessionalThrowsWhenNotOwnedByTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(professionalRepository.findByIdAndTenantId(professionalId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> professionalService.updateProfessional(
                professionalId, new ProfessionalRequest("Novo Nome", Set.of())))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void updateProfessionalUpdatesNameAndServices() {
        UUID tenantId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();
        Professional professional = new Professional();
        professional.setId(professionalId);
        professional.setName("Antigo");

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(professionalRepository.findByIdAndTenantId(professionalId, tenantId)).thenReturn(Optional.of(professional));
        when(professionalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProfessionalResponse response = professionalService.updateProfessional(
                professionalId, new ProfessionalRequest("Novo Nome", Set.of()));

        assertThat(response.name()).isEqualTo("Novo Nome");
    }

    @Test
    void deleteProfessionalDeactivates() {
        UUID tenantId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();
        Professional professional = new Professional();
        professional.setId(professionalId);
        professional.setActive(true);

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(professionalRepository.findByIdAndTenantId(professionalId, tenantId)).thenReturn(Optional.of(professional));
        when(professionalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        professionalService.deleteProfessional(professionalId);

        assertThat(professional.isActive()).isFalse();
    }

    @Test
    void getProfessionalByIdReturnsOwnedProfessional() {
        UUID tenantId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();
        Professional professional = new Professional();
        professional.setId(professionalId);
        professional.setName("Carlos");

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(professionalRepository.findByIdAndTenantId(professionalId, tenantId)).thenReturn(Optional.of(professional));

        ProfessionalResponse response = professionalService.getProfessionalById(professionalId);

        assertThat(response.name()).isEqualTo("Carlos");
    }

    @Test
    void getPublicProfessionalByIdReturnsActiveProfessional() {
        UUID professionalId = UUID.randomUUID();
        Professional professional = new Professional();
        professional.setId(professionalId);
        professional.setActive(true);

        when(professionalRepository.findById(professionalId)).thenReturn(Optional.of(professional));

        ProfessionalResponse response = professionalService.getPublicProfessionalById(professionalId);

        assertThat(response.id()).isEqualTo(professionalId);
    }

    @Test
    void getPublicProfessionalByIdThrowsWhenInactiveOrMissing() {
        UUID professionalId = UUID.randomUUID();
        when(professionalRepository.findById(professionalId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> professionalService.getPublicProfessionalById(professionalId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getPublicProfessionalsBySlugThrowsWhenTenantMissing() {
        when(tenantRepository.findBySlug("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> professionalService.getPublicProfessionalsBySlug("unknown"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getPublicProfessionalsBySlugReturnsListWhenTenantExists() {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        Professional professional = new Professional();
        professional.setId(UUID.randomUUID());
        professional.setName("Pedro");

        when(tenantRepository.findBySlug("shop")).thenReturn(Optional.of(tenant));
        when(professionalRepository.findAllActiveByTenantSlug("shop")).thenReturn(List.of(professional));

        List<ProfessionalResponse> result = professionalService.getPublicProfessionalsBySlug("shop");

        assertThat(result).hasSize(1);
    }
}
