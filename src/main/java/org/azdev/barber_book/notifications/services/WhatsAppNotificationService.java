package org.azdev.barber_book.notifications.services;

import lombok.extern.slf4j.Slf4j;
import org.azdev.barber_book.finance.events.AppointmentCompletedEvent;
import org.azdev.barber_book.notifications.events.AppointmentCancelledEvent;
import org.azdev.barber_book.notifications.events.AppointmentCreatedEvent;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.models.WhatsAppSettings;
import org.azdev.barber_book.repositories.TenantRepository;
import org.azdev.barber_book.repositories.WhatsAppSettingsRepository;
import org.azdev.barber_book.services.EvolutionApiClient;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClientException;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
public class WhatsAppNotificationService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Sao_Paulo");

    private final WhatsAppSettingsRepository settingsRepository;
    private final EvolutionApiClient evolutionApiClient;
    private final EntityManager entityManager;
    private final TenantRepository tenantRepository;

    @Autowired
    public WhatsAppNotificationService(
            WhatsAppSettingsRepository settingsRepository,
            EvolutionApiClient evolutionApiClient,
            EntityManager entityManager,
            TenantRepository tenantRepository) {
        this.settingsRepository = settingsRepository;
        this.evolutionApiClient = evolutionApiClient;
        this.entityManager = entityManager;
        this.tenantRepository = tenantRepository;
    }

    public WhatsAppNotificationService(
            WhatsAppSettingsRepository settingsRepository,
            EvolutionApiClient evolutionApiClient,
            EntityManager entityManager) {
        this(settingsRepository, evolutionApiClient, entityManager, null);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAppointmentCreated(AppointmentCreatedEvent event) {
        log.info("Iniciando envio de WhatsApp em background para o agendamento {}", event.appointmentId());

        entityManager.createNativeQuery(
                        "SELECT set_config('app.current_tenant_id', :tenantId, true)")
                .setParameter("tenantId", event.tenantId().toString())
                .getSingleResult();

        WhatsAppSettings settings = settingsRepository.findByTenantId(event.tenantId())
                .orElse(null);
        if (settings == null || settings.getInstanceName() == null) {
            log.info("WhatsApp não configurado para o tenant {}", event.tenantId());
            return;
        }

        ZoneId zoneId = getTenantZoneId(event.tenantId());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        String dateFormatted = event.startTime() != null
                ? event.startTime().atZoneSameInstant(zoneId).format(formatter)
                : "";
        String message = String.format(
                "Olá %s! Seu agendamento para o dia %s foi recebido com sucesso pela barbearia. Te esperamos!",
                event.clientName(), dateFormatted);

        try {
            evolutionApiClient.sendText(
                    settings.getInstanceName(),
                    "55" + event.clientPhone(),
                    message);
            log.info("WhatsApp enviado com sucesso para o agendamento {}", event.appointmentId());
        } catch (RestClientException e) {
            log.error("Falha ao enviar WhatsApp para o agendamento {} (erro: {})",
                    event.appointmentId(), e.getClass().getSimpleName());
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAppointmentCancelled(AppointmentCancelledEvent event) {
        log.info("Iniciando envio de WhatsApp de cancelamento para o agendamento {}", event.appointmentId());

        entityManager.createNativeQuery("SELECT set_config('app.current_tenant_id', :tenantId, true)")
                .setParameter("tenantId", event.tenantId().toString())
                .getSingleResult();

        WhatsAppSettings settings = settingsRepository.findByTenantId(event.tenantId()).orElse(null);
        if (settings == null || settings.getInstanceName() == null || !settings.isConnected()) return;

        ZoneId zoneId = getTenantZoneId(event.tenantId());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        String timeFormatted = event.startTime() != null
                ? event.startTime().atZoneSameInstant(zoneId).format(formatter)
                : "";

        String clientMessage = String.format(
                "Olá %s. Seu horário das %s com o(a) %s foi cancelado. Caso tenha sido um imprevisto, acesse nosso app para reagendar um novo horário.",
                event.clientName(), timeFormatted, event.professionalName());

        try {
            evolutionApiClient.sendText(settings.getInstanceName(), "55" + event.clientPhone(), clientMessage);

            if (event.professionalPhone() != null && !event.professionalPhone().isBlank()) {
                String profMessage = String.format(
                        "Aviso: O cliente %s cancelou o horário das %s. Sua agenda está livre nesse período! 🟢",
                        event.clientName(), timeFormatted);
                evolutionApiClient.sendText(settings.getInstanceName(), "55" + event.professionalPhone(), profMessage);
            }

            log.info("Notificações de cancelamento enviadas para o agendamento {}", event.appointmentId());
        } catch (Exception e) {
            log.error("Falha ao enviar WhatsApp de cancelamento para {}: {}", event.appointmentId(), e.getMessage());
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAppointmentCompleted(AppointmentCompletedEvent event) {
        log.info("Iniciando envio de WhatsApp de pós-venda para o agendamento {}", event.appointmentId());

        entityManager.createNativeQuery("SELECT set_config('app.current_tenant_id', :tenantId, true)")
                .setParameter("tenantId", event.tenantId().toString())
                .getSingleResult();

        WhatsAppSettings settings = settingsRepository.findByTenantId(event.tenantId()).orElse(null);
        if (settings == null || settings.getInstanceName() == null || !settings.isConnected()) return;

        String message = String.format(
                "Fala %s, o visual ficou no grau? 😎 Esperamos que tenha curtido o serviço com o(a) %s. Avalie nossa barbearia no Google clicando aqui: [Seu Link] - Até a próxima!",
                event.clientName(), event.professionalName());

        try {
            evolutionApiClient.sendText(settings.getInstanceName(), "55" + event.clientPhone(), message);
            log.info("WhatsApp de pós-venda enviado para o agendamento {}", event.appointmentId());
        } catch (Exception e) {
            log.error("Falha ao enviar WhatsApp de pós-venda para {}: {}", event.appointmentId(), e.getMessage());
        }
    }

    private ZoneId getTenantZoneId(UUID tenantId) {
        if (tenantId == null || tenantRepository == null) {
            return DEFAULT_ZONE;
        }
        try {
            return tenantRepository.findById(tenantId)
                    .map(Tenant::getTimezone)
                    .filter(tz -> tz != null && !tz.isBlank())
                    .map(ZoneId::of)
                    .orElse(DEFAULT_ZONE);
        } catch (Exception e) {
            log.warn("Erro ao obter timezone para o tenant {}, usando fallback {}", tenantId, DEFAULT_ZONE, e);
            return DEFAULT_ZONE;
        }
    }
}
