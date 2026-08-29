package org.azdev.barber_book.services;

import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.dtos.AppointmentRequest;
import org.azdev.barber_book.dtos.AppointmentResponse;
import org.azdev.barber_book.models.Appointment;
import org.azdev.barber_book.models.Professional;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.models.enums.AppointmentStatus;
import org.azdev.barber_book.repositories.AppointmentRepository;
import org.azdev.barber_book.repositories.CatalogRepository;
import org.azdev.barber_book.repositories.ProfessionalRepository;
import org.azdev.barber_book.repositories.TenantRepository;
import org.azdev.barber_book.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.azdev.barber_book.models.Catalog;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final CatalogRepository serviceRepository;
    private final ProfessionalRepository professionalRepository;
    private final SecurityUtils securityUtils;
    private final TenantRepository tenantRepository;

    @Transactional
    public AppointmentResponse createAppointment(AppointmentRequest dto) {

        Catalog catalogService = serviceRepository.findById(dto.serviceId())
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado."));

        if (!catalogService.isActive()) {
            throw new IllegalArgumentException("Este serviço não está mais disponível.");
        }

        Professional professional = professionalRepository.findById(dto.professionalId())
                .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado."));

        if (!professional.isActive()) {
            throw new IllegalArgumentException("Este profissional não está disponível no momento.");
        }

        if (!catalogService.getTenant().getId().equals(professional.getTenant().getId())) {
            throw new SecurityException("Inconsistência de dados: O serviço e o profissional não pertencem à mesma barbearia.");
        }

        Tenant tenant = professional.getTenant();

        OffsetDateTime startTime = dto.startTime();
        OffsetDateTime endTime = startTime.plusMinutes(catalogService.getDurationMinutes());

        boolean isSlotTaken = appointmentRepository.hasOverlappingAppointment(
                professional.getId(),
                startTime,
                endTime,
                List.of(AppointmentStatus.PENDING, AppointmentStatus.CANCELLED, AppointmentStatus.COMPLETED, AppointmentStatus.CONFIRMED)
        );

        if (isSlotTaken) {
            throw new IllegalStateException("Ops! Este horário acabou de ser reservado por outra pessoa. Por favor, escolha outro horário.");
        }

        Appointment appointment = new Appointment();
        appointment.setClientName(dto.clientName());
        appointment.setClientPhone(dto.clientPhone());
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setService(catalogService);
        appointment.setProfessional(professional);
        appointment.setTenant(tenant);

        appointment = appointmentRepository.save(appointment);

        return mapToResponse(appointment);
    }

    private AppointmentResponse mapToResponse(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getClientName(),
                appointment.getClientPhone(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus(),
                appointment.getProfessional().getName(),
                appointment.getService().getName(),
                appointment.getService().getPrice()
        );
    }

    public List<String> getAvailableSlots(UUID professionalId, LocalDate date, UUID serviceId) {

        Catalog service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado."));
        int serviceDuration = service.getDurationMinutes();

        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado."));

        Tenant tenant = professional.getTenant();
        ZoneId zoneId = ZoneId.of(tenant.getTimezone());
        LocalTime workStart = tenant.getOpeningTime();
        LocalTime workEnd = tenant.getClosingTime();

        OffsetDateTime startOfDay = date.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime endOfDay = date.atTime(23, 59, 59).atZone(zoneId).toOffsetDateTime();

        List<Appointment> dailyAppointments = appointmentRepository
                .findDailyAgendaForProfessional(
                        professionalId,
                        startOfDay,
                        endOfDay,
                        List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED));

        int gridStepMinutes = tenant.getSlotInterval();
        List<String> availableSlots = new ArrayList<>();
        LocalTime currentSlot = workStart;

        OffsetDateTime nowComMargem = OffsetDateTime.now(zoneId).plusMinutes(30);

        while (!currentSlot.plusMinutes(serviceDuration).isAfter(workEnd)) {

            OffsetDateTime slotStart = date.atTime(currentSlot).atZone(zoneId).toOffsetDateTime();
            OffsetDateTime slotEnd = slotStart.plusMinutes(serviceDuration);

            boolean isTaken = dailyAppointments.stream().anyMatch(appt ->
                    slotStart.isBefore(appt.getEndTime()) && slotEnd.isAfter(appt.getStartTime())
            );

            if (!isTaken && slotStart.isAfter(nowComMargem)) {
                availableSlots.add(currentSlot.toString());
            }

            currentSlot = currentSlot.plusMinutes(gridStepMinutes);
        }

        return availableSlots;
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> listAppointmentsByTenant(LocalDate startDate, LocalDate endDate) {
        UUID tenantId = securityUtils.getCurrentTenantId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Barbearia não encontrada."));

        ZoneId zoneId = ZoneId.of(tenant.getTimezone());

        OffsetDateTime start = startDate.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime end = endDate.atTime(23, 59, 59).atZone(zoneId).toOffsetDateTime();

        List<Appointment> appointments = appointmentRepository
                .findByTenantIdAndStartTimeBetweenOrderByStartTimeAsc(tenantId, start, end);

        return appointments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelAppointment(UUID appointmentId) {
        UUID tenantId = securityUtils.getCurrentTenantId();

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado."));

        if (!appointment.getTenant().getId().equals(tenantId)) {
            throw new SecurityException("Você não tem permissão para cancelar este agendamento.");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }

    @Transactional
    public void completeAppointment(UUID appointmentId) {
        UUID tenantId = securityUtils.getCurrentTenantId();

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado."));

        if (!appointment.getTenant().getId().equals(tenantId)) {
            throw new SecurityException("Você não tem permissão para alterar este agendamento.");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);
    }
}
