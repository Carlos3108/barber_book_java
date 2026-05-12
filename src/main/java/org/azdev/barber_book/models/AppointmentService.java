package org.azdev.barber_book.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "services")
@Getter @Setter
public class AppointmentService extends BaseEntity {
    @Column(nullable = false)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean status = true;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes; // 20, 45, 60...

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
}
