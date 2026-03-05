package org.azdev.barber_book.repositories;

import org.azdev.barber_book.models.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByTenantIdAndStartTimeBetweenOrderByStartTimeAsc(
            UUID tenantId,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay
    );

    // 🛡️ Prevenção de Double Booking (Verifica se já existe agendamento que conflita com o horário desejado)
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Appointment a " +
            "WHERE a.tenant.id = :tenantId " +
            "AND a.status = 'CONFIRMED' " +
            "AND ((a.startTime < :newEndTime AND a.endTime > :newStartTime))")
    boolean hasOverlappingAppointment(
            @Param("tenantId") UUID tenantId,
            @Param("newStartTime") LocalDateTime newStartTime,
            @Param("newEndTime") LocalDateTime newEndTime
    );
}
