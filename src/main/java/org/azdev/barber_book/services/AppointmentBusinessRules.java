package org.azdev.barber_book.services;

import org.azdev.barber_book.exception.BadRequestException;
import org.azdev.barber_book.exception.ConflictException;
import org.azdev.barber_book.exception.UnauthorizedException;
import org.azdev.barber_book.models.Catalog;
import org.azdev.barber_book.models.Professional;
import org.azdev.barber_book.models.enums.AppointmentStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class AppointmentBusinessRules {

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
