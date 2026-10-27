package org.azdev.barber_book.notifications.services;

import lombok.extern.slf4j.Slf4j;
import org.azdev.barber_book.notifications.events.AppointmentCreatedEvent;
import org.azdev.barber_book.models.WhatsAppSettings;
import org.azdev.barber_book.repositories.WhatsAppSettingsRepository;
import org.azdev.barber_book.services.EvolutionApiClient;
import jakarta.persistence.EntityManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClientException;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class WhatsAppNotificationService {

    private final WhatsAppSettingsRepository settingsRepository;
    private final EvolutionApiClient evolutionApiClient;
    private final EntityManager entityManager;

    public WhatsAppNotificationService(
            WhatsAppSettingsRepository settingsRepository,
            EvolutionApiClient evolutionApiClient,
            EntityManager entityManager) {
        this.settingsRepository = settingsRepository;
        this.evolutionApiClient = evolutionApiClient;
        this.entityManager = entityManager;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAppointmentCreated(AppointmentCreatedEvent event) {
        log.info("Iniciando envio de WhatsApp em background para {}", event.clientPhone());

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

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        String dateFormatted = event.startTime().format(formatter);
        String message = String.format(
                "Olá %s! Seu agendamento para o dia %s foi recebido com sucesso pela barbearia. Te esperamos!",
                event.clientName(), dateFormatted);

        try {
            evolutionApiClient.sendText(
                    settings.getInstanceName(),
                    "55" + event.clientPhone(),
                    message);
            log.info("WhatsApp enviado com sucesso para {}", event.clientPhone());
        } catch (RestClientException e) {
            log.error("Falha ao enviar WhatsApp para {}", event.clientPhone(), e);
        }
    }
}
