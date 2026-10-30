package org.azdev.barber_book.services;

import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.exception.BadRequestException;
import org.azdev.barber_book.dtos.ClientFrequencyItemResponse;
import org.azdev.barber_book.dtos.ClientFrequencyReportResponse;
import org.azdev.barber_book.exception.NotFoundException;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.models.enums.AppointmentStatus;
import org.azdev.barber_book.repositories.AppointmentRepository;
import org.azdev.barber_book.repositories.ClientFrequencyProjection;
import org.azdev.barber_book.repositories.TenantRepository;
import org.azdev.barber_book.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ClientFrequencyService {

    private final AppointmentRepository appointmentRepository;
    private final TenantRepository tenantRepository;
    private final SecurityUtils securityUtils;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ClientFrequencyReportResponse getReport(YearMonth requestedMonth, int page, int size) {
        var tenantId = securityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Barbearia não encontrada."));

        ZoneId zoneId = ZoneId.of(tenant.getTimezone());
        YearMonth currentMonth = YearMonth.now(clock.withZone(zoneId));
        if (requestedMonth != null && requestedMonth.isAfter(currentMonth)) {
            throw new BadRequestException("O relatório não pode ser consultado para meses futuros.");
        }
        YearMonth month = requestedMonth == null ? currentMonth : requestedMonth;

        var monthStart = month.atDay(1).atStartOfDay(zoneId).toOffsetDateTime();
        var monthEnd = month.plusMonths(1).atDay(1).atStartOfDay(zoneId).toOffsetDateTime();
        LocalDate today = LocalDate.now(clock.withZone(zoneId));

        Page<ClientFrequencyProjection> results = appointmentRepository.findClientFrequencyByTenant(
                tenantId,
                AppointmentStatus.COMPLETED,
                monthStart,
                monthEnd,
                PageRequest.of(page, size)
        );

        var clients = results.getContent().stream()
                .map(client -> toResponse(client, zoneId, today))
                .toList();

        return new ClientFrequencyReportResponse(
                month.toString(),
                results.getNumber(),
                results.getSize(),
                results.getTotalElements(),
                clients
        );
    }

    private ClientFrequencyItemResponse toResponse(
            ClientFrequencyProjection client,
            ZoneId zoneId,
            LocalDate today
    ) {
        var lastCompletedAt = Objects.requireNonNull(client.getLastCompletedAt());
        long daysSinceLastCompleted = Math.max(
                0,
                today.toEpochDay() - lastCompletedAt.atZoneSameInstant(zoneId).toLocalDate().toEpochDay()
        );

        return new ClientFrequencyItemResponse(
                client.getClientName(),
                client.getClientPhone(),
                Objects.requireNonNull(client.getCompletedAppointmentsThisMonth()),
                daysSinceLastCompleted
        );
    }
}
