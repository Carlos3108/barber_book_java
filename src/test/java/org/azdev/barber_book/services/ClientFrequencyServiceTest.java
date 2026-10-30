package org.azdev.barber_book.services;

import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.exception.BadRequestException;
import org.azdev.barber_book.repositories.AppointmentRepository;
import org.azdev.barber_book.repositories.ClientFrequencyProjection;
import org.azdev.barber_book.repositories.TenantRepository;
import org.azdev.barber_book.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientFrequencyServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private SecurityUtils securityUtils;

    @Test
    void defaultsToTenantLocalMonthAndCalculatesDaysSinceLastCompletedVisit() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = tenant(tenantId);
        Clock clock = Clock.fixed(Instant.parse("2026-03-01T02:00:00Z"), ZoneId.of("UTC"));
        ClientFrequencyProjection projection = projection(
                "Cliente", "11911111111", 2L,
                OffsetDateTime.parse("2026-02-20T14:00:00-03:00"));
        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(appointmentRepository.findClientFrequencyByTenant(
                eq(tenantId), eq(org.azdev.barber_book.models.enums.AppointmentStatus.COMPLETED),
                eq(OffsetDateTime.parse("2026-02-01T00:00:00-03:00")),
                eq(OffsetDateTime.parse("2026-03-01T00:00:00-03:00")),
                eq(PageRequest.of(0, 25))))
                .thenReturn(new PageImpl<>(List.of(projection), PageRequest.of(0, 25), 1));

        ClientFrequencyService service = new ClientFrequencyService(
                appointmentRepository, tenantRepository, securityUtils, clock);

        var response = service.getReport(null, 0, 25);

        assertThat(response.month()).isEqualTo("2026-02");
        assertThat(response.totalClients()).isEqualTo(1);
        assertThat(response.clients().getFirst().completedAppointmentsThisMonth()).isEqualTo(2);
        assertThat(response.clients().getFirst().daysSinceLastCompleted()).isEqualTo(8);
    }

    @Test
    void usesExplicitMonthAndRequestedPage() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = tenant(tenantId);
        Clock clock = Clock.fixed(Instant.parse("2026-10-10T12:00:00Z"), ZoneId.of("UTC"));
        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(appointmentRepository.findClientFrequencyByTenant(
                eq(tenantId),
                eq(org.azdev.barber_book.models.enums.AppointmentStatus.COMPLETED),
                eq(OffsetDateTime.parse("2026-04-01T00:00:00-03:00")),
                eq(OffsetDateTime.parse("2026-05-01T00:00:00-03:00")),
                eq(PageRequest.of(2, 10))))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 10), 0));

        ClientFrequencyService service = new ClientFrequencyService(
                appointmentRepository, tenantRepository, securityUtils, clock);

        var response = service.getReport(YearMonth.of(2026, 4), 2, 10);

        assertThat(response.month()).isEqualTo("2026-04");
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(10);
    }

    @Test
    void rejectsFutureMonth() {
        UUID tenantId = UUID.randomUUID();
        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant(tenantId)));
        ClientFrequencyService service = new ClientFrequencyService(
                appointmentRepository,
                tenantRepository,
                securityUtils,
                Clock.fixed(Instant.parse("2026-10-10T12:00:00Z"), ZoneId.of("UTC")));

        assertThatThrownBy(() -> service.getReport(YearMonth.of(2026, 11), 0, 25))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("O relatório não pode ser consultado para meses futuros.");
    }

    private Tenant tenant(UUID id) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setTimezone("America/Sao_Paulo");
        return tenant;
    }

    private ClientFrequencyProjection projection(
            String name,
            String phone,
            Long monthlyCount,
            OffsetDateTime lastCompletedAt
    ) {
        return new ClientFrequencyProjection() {
            @Override
            public String getClientName() {
                return name;
            }

            @Override
            public String getClientPhone() {
                return phone;
            }

            @Override
            public Long getCompletedAppointmentsThisMonth() {
                return monthlyCount;
            }

            @Override
            public OffsetDateTime getLastCompletedAt() {
                return lastCompletedAt;
            }
        };
    }
}
