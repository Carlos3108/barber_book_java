package org.azdev.barber_book.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.azdev.barber_book.dtos.AppointmentRequest;
import org.azdev.barber_book.dtos.AppointmentResponse;
import org.azdev.barber_book.exception.BadRequestException;
import org.azdev.barber_book.exception.ConflictException;
import org.azdev.barber_book.exception.NotFoundException;
import org.azdev.barber_book.exception.UnauthorizedException;
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
@Slf4j
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final CatalogRepository serviceRepository;
    private final ProfessionalRepository professionalRepository;
    private final SecurityUtils securityUtils;
    private final TenantRepository tenantRepository;
    private final AppointmentAvailabilityService appointmentAvailabilityService;

    @Transactional
    public AppointmentResponse createAppointment(AppointmentRequest dto) {

        Catalog catalogService = serviceRepository.findById(dto.serviceId())
                .orElseThrow(() -> new NotFoundException("Serviço não encontrado."));

        if (!catalogService.isActive()) {
            throw new BadRequestException("Este serviço não está mais disponível.");
        }

        Professional professional = professionalRepository.findById(dto.professionalId())
                .orElseThrow(() -> new NotFoundException("Profissional não encontrado."));

        if (!professional.isActive()) {
            throw new BadRequestException("Este profissional não está disponível no momento.");
        }

        if (!catalogService.getTenant().getId().equals(professional.getTenant().getId())) {
            throw new ConflictException("Inconsistência de dados: o serviço e o profissional não pertencem à mesma barbearia.");
        }

        Tenant tenant = professional.getTenant();

        OffsetDateTime startTime = dto.startTime();
        OffsetDateTime endTime = startTime.plusMinutes(catalogService.getDurationMinutes());

        boolean isSlotTaken = appointmentRepository.hasOverlappingAppointment(
                professional.getId(),
                startTime,
                endTime,
                List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED)
        );

        if (isSlotTaken) {
            throw new ConflictException("Ops! Este horário acabou de ser reservado por outra pessoa. Por favor, escolha outro horário.");
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

    @Transactional(readOnly = true)
    public List<String> getAvailableSlots(UUID professionalId, LocalDate date, UUID serviceId) {
        return appointmentAvailabilityService.getAvailableSlots(professionalId, date, serviceId);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> listAppointmentsByTenant(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Data inicial não pode ser maior que a data final.");
        }

        UUID tenantId = securityUtils.getCurrentTenantId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Barbearia não encontrada."));

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
                .orElseThrow(() -> new NotFoundException("Agendamento não encontrado."));

        if (!appointment.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("Você não tem permissão para cancelar este agendamento.");
        }

        if (appointment.getStatus() != AppointmentStatus.PENDING
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BadRequestException("Somente agendamentos pendentes ou confirmados podem ser cancelados.");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }

    @Transactional
    public void completeAppointment(UUID appointmentId) {
        UUID tenantId = securityUtils.getCurrentTenantId();

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Agendamento não encontrado."));

        if (!appointment.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("Você não tem permissão para alterar este agendamento.");
        }

        if (appointment.getStatus() != AppointmentStatus.PENDING
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BadRequestException("Somente agendamentos pendentes ou confirmados podem ser concluídos.");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);
    }
}
