package org.azdev.barber_book.repositories;

import org.azdev.barber_book.models.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    List<Appointment> findByTenantIdAndStartTimeBetweenOrderByStartTimeAsc(
            UUID tenantId,
            OffsetDateTime startOfDay,
            OffsetDateTime endOfDay
    );

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Appointment a " +
            "WHERE a.tenant.id = :tenantId " +
            "AND a.status = 'CONFIRMED' " +
            "AND ((a.startTime < :newEndTime AND a.endTime > :newStartTime))")
    boolean hasOverlappingAppointment(
            @Param("tenantId") UUID tenantId,
            @Param("newStartTime") OffsetDateTime newStartTime,
            @Param("newEndTime") OffsetDateTime newEndTime
    );
}
