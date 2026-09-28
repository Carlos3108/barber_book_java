package org.azdev.barber_book.services;

import org.azdev.barber_book.exception.BadRequestException;
import org.azdev.barber_book.exception.ConflictException;
import org.azdev.barber_book.exception.UnauthorizedException;
import org.azdev.barber_book.models.Catalog;
import org.azdev.barber_book.models.Professional;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.models.enums.AppointmentStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentBusinessRulesTest {

    private final Clock fixedClock = Clock.fixed(
            OffsetDateTime.parse("2026-09-01T08:00:00-03:00").toInstant(),
            ZoneId.of("America/Sao_Paulo")
    );

    private final AppointmentBusinessRules rules = new AppointmentBusinessRules(fixedClock);

    private Tenant buildTenant() {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setTimezone("America/Sao_Paulo");
        tenant.setOpeningTime(LocalTime.of(9, 0));
        tenant.setClosingTime(LocalTime.of(18, 0));
        tenant.setSlotInterval(30);
        return tenant;
    }

    @Test
    void shouldRejectServiceNotOfferedByProfessional() {
        Tenant tenant = buildTenant();

        Catalog service = new Catalog();
        service.setId(UUID.randomUUID());
        service.setActive(true);
        service.setTenant(tenant);

        Professional professional = new Professional();
        professional.setId(UUID.randomUUID());
        professional.setActive(true);
        professional.setTenant(tenant);
        // professional.getServices() intentionally left empty

        assertThatThrownBy(() -> rules.validateCreation(service, professional))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Este profissional não realiza o serviço selecionado.");
    }

    @Test
    void shouldRejectServiceAndProfessionalFromDifferentTenants() {
        Tenant tenantA = buildTenant();
        Tenant tenantB = buildTenant();

        Catalog service = new Catalog();
        service.setId(UUID.randomUUID());
        service.setActive(true);
        service.setTenant(tenantA);

        Professional professional = new Professional();
        professional.setId(UUID.randomUUID());
        professional.setActive(true);
        professional.setTenant(tenantB);
        professional.getServices().add(service);

        assertThatThrownBy(() -> rules.validateCreation(service, professional))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void shouldAcceptSlotWithinBusinessHoursAndAlignedToGrid() {
        Tenant tenant = buildTenant();
        OffsetDateTime start = OffsetDateTime.parse("2026-09-02T10:00:00-03:00");
        OffsetDateTime end = start.plusMinutes(30);

        assertThatCode(() -> rules.validateSlotAgainstSchedule(tenant, start, end))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectSlotBeforeMinimumLeadTime() {
        Tenant tenant = buildTenant();
        OffsetDateTime start = OffsetDateTime.parse("2026-09-01T08:10:00-03:00");
        OffsetDateTime end = start.plusMinutes(30);

        assertThatThrownBy(() -> rules.validateSlotAgainstSchedule(tenant, start, end))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("antecedência mínima");
    }

    @Test
    void shouldRejectSlotOutsideBusinessHours() {
        Tenant tenant = buildTenant();
        OffsetDateTime start = OffsetDateTime.parse("2026-09-02T19:00:00-03:00");
        OffsetDateTime end = start.plusMinutes(30);

        assertThatThrownBy(() -> rules.validateSlotAgainstSchedule(tenant, start, end))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("horário de funcionamento");
    }

    @Test
    void shouldRejectSlotNotAlignedToGrid() {
        Tenant tenant = buildTenant();
        OffsetDateTime start = OffsetDateTime.parse("2026-09-02T10:15:00-03:00");
        OffsetDateTime end = start.plusMinutes(30);

        assertThatThrownBy(() -> rules.validateSlotAgainstSchedule(tenant, start, end))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("grade de agendamentos");
    }

    @Test
    void shouldRejectInactiveService() {
        Tenant tenant = buildTenant();
        Catalog service = new Catalog();
        service.setId(UUID.randomUUID());
        service.setActive(false);
        service.setTenant(tenant);

        Professional professional = new Professional();
        professional.setId(UUID.randomUUID());
        professional.setActive(true);
        professional.setTenant(tenant);
        professional.getServices().add(service);

        assertThatThrownBy(() -> rules.validateCreation(service, professional))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("serviço não está mais disponível");
    }

    @Test
    void shouldRejectInactiveProfessional() {
        Tenant tenant = buildTenant();
        Catalog service = new Catalog();
        service.setId(UUID.randomUUID());
        service.setActive(true);
        service.setTenant(tenant);

        Professional professional = new Professional();
        professional.setId(UUID.randomUUID());
        professional.setActive(false);
        professional.setTenant(tenant);
        professional.getServices().add(service);

        assertThatThrownBy(() -> rules.validateCreation(service, professional))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("profissional não está disponível");
    }

    @Test
    void shouldAcceptValidCreation() {
        Tenant tenant = buildTenant();
        Catalog service = new Catalog();
        service.setId(UUID.randomUUID());
        service.setActive(true);
        service.setTenant(tenant);

        Professional professional = new Professional();
        professional.setId(UUID.randomUUID());
        professional.setActive(true);
        professional.setTenant(tenant);
        professional.getServices().add(service);

        assertThatCode(() -> rules.validateCreation(service, professional)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectDateRangeWithStartAfterEnd() {
        assertThatThrownBy(() -> rules.validateDateRange(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 1)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldAcceptValidDateRange() {
        assertThatCode(() -> rules.validateDateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10)))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectCancellationOwnershipMismatch() {
        UUID tenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();

        assertThatThrownBy(() -> rules.validateCancellationOwnership(tenantId, otherTenantId))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void shouldAcceptCancellationOwnershipMatch() {
        UUID tenantId = UUID.randomUUID();

        assertThatCode(() -> rules.validateCancellationOwnership(tenantId, tenantId)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectCompletionOwnershipMismatch() {
        UUID tenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();

        assertThatThrownBy(() -> rules.validateCompletionOwnership(tenantId, otherTenantId))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void shouldAcceptCompletionOwnershipMatch() {
        UUID tenantId = UUID.randomUUID();

        assertThatCode(() -> rules.validateCompletionOwnership(tenantId, tenantId)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectCancellationForCompletedStatus() {
        assertThatThrownBy(() -> rules.validateStatusForCancellation(AppointmentStatus.COMPLETED))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldAcceptCancellationForPendingOrConfirmedStatus() {
        assertThatCode(() -> rules.validateStatusForCancellation(AppointmentStatus.PENDING)).doesNotThrowAnyException();
        assertThatCode(() -> rules.validateStatusForCancellation(AppointmentStatus.CONFIRMED)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectCompletionForCancelledStatus() {
        assertThatThrownBy(() -> rules.validateStatusForCompletion(AppointmentStatus.CANCELLED))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldAcceptCompletionForPendingOrConfirmedStatus() {
        assertThatCode(() -> rules.validateStatusForCompletion(AppointmentStatus.PENDING)).doesNotThrowAnyException();
        assertThatCode(() -> rules.validateStatusForCompletion(AppointmentStatus.CONFIRMED)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectSlotSpanningDifferentDays() {
        Tenant tenant = buildTenant();
        OffsetDateTime start = OffsetDateTime.parse("2026-09-02T23:45:00-03:00");
        OffsetDateTime end = OffsetDateTime.parse("2026-09-03T00:15:00-03:00");

        assertThatThrownBy(() -> rules.validateSlotAgainstSchedule(tenant, start, end))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("mesmo dia");
    }
}
