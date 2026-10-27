package org.azdev.barber_book.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "whatsapp_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppSettings extends BaseEntity{

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    private Tenant tenant;

    @Column(name = "instance_name")
    private String instanceName;

    @Column(nullable = false)
    @Builder.Default
    private boolean connected = false;

    @Column(name = "is_reminder_enabled", nullable = false)
    @Builder.Default
    private boolean isReminderEnabled = false;

    @Column(name = "is_daily_summary_enabled", nullable = false)
    @Builder.Default
    private boolean isDailySummaryEnabled = false;

}