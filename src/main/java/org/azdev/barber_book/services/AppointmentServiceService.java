package org.azdev.barber_book.services;

import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.dtos.AppointmentRequest;
import org.azdev.barber_book.dtos.AppointmentResponse;
import org.azdev.barber_book.models.Appointment;
import org.azdev.barber_book.models.Professional;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.repositories.AppointmentRepository;
import org.azdev.barber_book.repositories.AppointmentServiceRepository;
import org.azdev.barber_book.repositories.ProfessionalRepository;
import org.azdev.barber_book.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.azdev.barber_book.models.AppointmentService;

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
public class AppointmentServiceService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentServiceRepository serviceRepository;
    private final ProfessionalRepository professionalRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public AppointmentResponse createAppointment(AppointmentRequest dto) {

        AppointmentService catalogService = serviceRepository.findById(dto.serviceId())
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
                endTime
        );

        if (isSlotTaken) {
            throw new IllegalStateException("Ops! Este horário acabou de ser reservado por outra pessoa. Por favor, escolha outro horário.");
        }

        Appointment appointment = new Appointment();
        appointment.setClientName(dto.clientName());
        appointment.setClientPhone(dto.clientPhone());
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setStatus("CONFIRMED");
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

    public List<String> getAvailableSlots(UUID professionalId, LocalDate date) {

        ZoneId zoneId = ZoneId.of("America/Sao_Paulo");

        OffsetDateTime startOfDay = date.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime endOfDay = date.atTime(23, 59, 59).atZone(zoneId).toOffsetDateTime();

        List<Appointment> dailyAppointments = appointmentRepository
                .findDailyAgendaForProfessional(professionalId, startOfDay, endOfDay);

        LocalTime workStart = LocalTime.of(9, 0);
        LocalTime workEnd = LocalTime.of(18, 0);
        int slotDurationMinutes = 30;

        List<String> availableSlots = new ArrayList<>();
        LocalTime currentSlot = workStart;
        OffsetDateTime now = OffsetDateTime.now(zoneId);

        while (currentSlot.isBefore(workEnd)) {

            OffsetDateTime slotStart = date.atTime(currentSlot).atZone(zoneId).toOffsetDateTime();
            OffsetDateTime slotEnd = slotStart.plusMinutes(slotDurationMinutes);

            boolean isTaken = dailyAppointments.stream().anyMatch(appt ->
                    slotStart.isBefore(appt.getEndTime()) && slotEnd.isAfter(appt.getStartTime())
            );

            if (!isTaken && slotStart.isAfter(now)) {
                availableSlots.add(currentSlot.toString());
            }

            currentSlot = currentSlot.plusMinutes(slotDurationMinutes);
        }

        return availableSlots;
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> listAppointmentsByTenant() {
        UUID tenantId = securityUtils.getCurrentTenantId();
        List<Appointment> appointments = appointmentRepository.findAllByTenantId(tenantId);
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

        appointment.setStatus("CANCELLED");
        appointmentRepository.save(appointment);
    }
}
