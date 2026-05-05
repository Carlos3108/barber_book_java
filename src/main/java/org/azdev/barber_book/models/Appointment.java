package org.azdev.barber_book.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Entity
@Table(name = "appointments")
@Getter @Setter
public class Appointment extends BaseEntity{
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "client_phone", nullable = false)
    private String clientPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private AppointmentService service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private String status = "CONFIRMED"; // CONFIRMED, CANCELLED
}
