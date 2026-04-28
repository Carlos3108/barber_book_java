package org.azdev.barber_book.repositories;

import org.azdev.barber_book.models.AppointmentService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AppointmentServiceRepository extends JpaRepository<AppointmentService, UUID> {

    List<AppointmentService> findAllByTenantId(UUID tenantId);

}
