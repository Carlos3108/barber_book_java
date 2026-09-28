package org.azdev.barber_book.services;

import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.exception.BadRequestException;
import org.azdev.barber_book.exception.ConflictException;
import org.azdev.barber_book.exception.UnauthorizedException;
import org.azdev.barber_book.models.Catalog;
import org.azdev.barber_book.models.Professional;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.models.enums.AppointmentStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AppointmentBusinessRules {

    private final Clock clock;

    public void validateCreation(Catalog catalogService, Professional professional) {
        if (!catalogService.isActive()) {
            throw new BadRequestException("Este serviço não está mais disponível.");
        }

        if (!professional.isActive()) {
            throw new BadRequestException("Este profissional não está disponível no momento.");
        }

        if (!catalogService.getTenant().getId().equals(professional.getTenant().getId())) {
            throw new ConflictException("Inconsistência de dados: o serviço e o profissional não pertencem à mesma barbearia.");
        }

        validateProfessionalOffersService(catalogService, professional);
    }

    /**
     * Ensures the professional is actually eligible to perform the requested service
     * (i.e. the service is part of the professional's assigned service list).
     */
    public void validateProfessionalOffersService(Catalog catalogService, Professional professional) {
        boolean offersService = professional.getServices().stream()
                .anyMatch(offered -> offered.getId().equals(catalogService.getId()));

        if (!offersService) {
            throw new BadRequestException("Este profissional não realiza o serviço selecionado.");
        }
    }

    /**
     * Validates that a requested appointment slot respects the tenant's minimum lead time,
     * business hours and slot grid alignment. This mirrors the rules used to generate the
     * available slots so a client can't book a time the availability endpoint never offered.
     */
    public void validateSlotAgainstSchedule(Tenant tenant, OffsetDateTime startTime, OffsetDateTime endTime) {
        ZoneId zoneId = ZoneId.of(tenant.getTimezone());

        OffsetDateTime nowWithMargin = OffsetDateTime.now(clock.withZone(zoneId)).plusMinutes(30);
        if (startTime.isBefore(nowWithMargin)) {
            throw new BadRequestException("O horário selecionado não respeita a antecedência mínima de 30 minutos.");
        }

        LocalDate startDate = startTime.atZoneSameInstant(zoneId).toLocalDate();
        LocalDate endDate = endTime.atZoneSameInstant(zoneId).toLocalDate();
        LocalTime startLocal = startTime.atZoneSameInstant(zoneId).toLocalTime();
        LocalTime endLocal = endTime.atZoneSameInstant(zoneId).toLocalTime();

        if (!startDate.equals(endDate)) {
            throw new BadRequestException("O agendamento deve começar e terminar dentro do mesmo dia de funcionamento.");
        }

        LocalTime openingTime = tenant.getOpeningTime();
        LocalTime closingTime = tenant.getClosingTime();

        if (startLocal.isBefore(openingTime) || endLocal.isAfter(closingTime)) {
            throw new BadRequestException("O horário selecionado está fora do horário de funcionamento da barbearia.");
        }

        int slotInterval = (tenant.getSlotInterval() != null && tenant.getSlotInterval() > 0)
                ? tenant.getSlotInterval()
                : 30;

        long minutesFromOpening = Duration.between(openingTime, startLocal).toMinutes();
        if (minutesFromOpening % slotInterval != 0) {
            throw new BadRequestException("O horário selecionado não está alinhado à grade de agendamentos.");
        }
    }

    public void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Data inicial não pode ser maior que a data final.");
        }
    }

    public void validateCancellationOwnership(UUID tenantId, UUID appointmentTenantId) {
        if (!appointmentTenantId.equals(tenantId)) {
            throw new UnauthorizedException("Você não tem permissão para cancelar este agendamento.");
        }
    }

    public void validateCompletionOwnership(UUID tenantId, UUID appointmentTenantId) {
        if (!appointmentTenantId.equals(tenantId)) {
            throw new UnauthorizedException("Você não tem permissão para alterar este agendamento.");
        }
    }

    public void validateStatusForCancellation(AppointmentStatus status) {
        if (status != AppointmentStatus.PENDING && status != AppointmentStatus.CONFIRMED) {
            throw new BadRequestException("Somente agendamentos pendentes ou confirmados podem ser cancelados.");
        }
    }

    public void validateStatusForCompletion(AppointmentStatus status) {
        if (status != AppointmentStatus.PENDING && status != AppointmentStatus.CONFIRMED) {
            throw new BadRequestException("Somente agendamentos pendentes ou confirmados podem ser concluídos.");
        }
    }
}
