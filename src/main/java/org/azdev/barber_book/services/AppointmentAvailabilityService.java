package org.azdev.barber_book.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.azdev.barber_book.exception.BadRequestException;
import org.azdev.barber_book.exception.NotFoundException;
import org.azdev.barber_book.models.Appointment;
import org.azdev.barber_book.models.Catalog;
import org.azdev.barber_book.models.Professional;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.models.enums.AppointmentStatus;
import org.azdev.barber_book.repositories.AppointmentRepository;
import org.azdev.barber_book.repositories.CatalogRepository;
import org.azdev.barber_book.repositories.ProfessionalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentAvailabilityService {

    private final AppointmentRepository appointmentRepository;
    private final CatalogRepository catalogRepository;
    private final ProfessionalRepository professionalRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<String> getAvailableSlots(UUID professionalId, LocalDate date, UUID serviceId) {
        Catalog service = catalogRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Serviço não encontrado."));

        Professional professional = professionalRepository.findByIdWithTenant(professionalId)
                .orElseThrow(() -> new NotFoundException("Profissional não encontrado."));

        Tenant tenant = professional.getTenant();
        ZoneId zoneId = ZoneId.of(tenant.getTimezone());
        LocalTime workStart = tenant.getOpeningTime();
        LocalTime workEnd = tenant.getClosingTime();
        LocalDate tenantToday = LocalDate.now(clock.withZone(zoneId));
        LocalTime tenantNowTime = LocalTime.now(clock.withZone(zoneId));
        OffsetDateTime nowWithMargin = OffsetDateTime.now(clock.withZone(zoneId)).plusMinutes(30);

        log.info("Slots check -> professionalId={}, serviceId={}, tenantId={}, timezone={}, requestedDate={}, tenantToday={}, tenantNow={}, openingTime={}, closingTime={}",
                professionalId,
                serviceId,
                tenant.getId(),
                zoneId,
                date,
                tenantToday,
                tenantNowTime,
                workStart,
                workEnd);

        if (date.isBefore(tenantToday)) {
            return List.of();
        }

        if (date.isEqual(tenantToday)) {
            if (!tenantNowTime.isBefore(workEnd)) {
                return List.of();
            }
            OffsetDateTime endOfWorkDayWithMargin = date.atTime(workEnd).atZone(zoneId).toOffsetDateTime();
            if (!nowWithMargin.isBefore(endOfWorkDayWithMargin)) {
                return List.of();
            }
        }

        if (service.getDurationMinutes() <= 0) {
            throw new BadRequestException("A duração do serviço deve ser maior que zero.");
        }

        OffsetDateTime startOfDay = date.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime endOfDay = date.atTime(23, 59, 59).atZone(zoneId).toOffsetDateTime();

        List<Appointment> dailyAppointments = appointmentRepository.findDailyAgendaForProfessional(
                professionalId,
                startOfDay,
                endOfDay,
                List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED)
        );

        int gridStepMinutes = tenant.getSlotInterval();
        List<String> availableSlots = new ArrayList<>();
        LocalTime currentSlot = workStart;

        while (!currentSlot.plusMinutes(service.getDurationMinutes()).isAfter(workEnd)) {
            OffsetDateTime slotStart = date.atTime(currentSlot).atZone(zoneId).toOffsetDateTime();
            OffsetDateTime slotEnd = slotStart.plusMinutes(service.getDurationMinutes());

            boolean isTaken = dailyAppointments.stream().anyMatch(appt ->
                    slotStart.isBefore(appt.getEndTime()) && slotEnd.isAfter(appt.getStartTime())
            );

            if (!isTaken && !slotStart.isBefore(nowWithMargin)) {
                availableSlots.add(currentSlot.toString());
            }

            currentSlot = currentSlot.plusMinutes(gridStepMinutes);
        }

        return availableSlots;
    }
}
