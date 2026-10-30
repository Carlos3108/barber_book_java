package org.azdev.barber_book.notifications.services;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.models.WhatsAppSettings;
import org.azdev.barber_book.notifications.events.AppointmentCancelledEvent;
import org.azdev.barber_book.notifications.events.AppointmentCreatedEvent;
import org.azdev.barber_book.repositories.TenantRepository;
import org.azdev.barber_book.repositories.WhatsAppSettingsRepository;
import org.azdev.barber_book.services.EvolutionApiClient;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WhatsAppNotificationServiceTest {

    @Test
    void logsDoNotContainClientPersonalDataOrProviderErrorDetails() {
        UUID appointmentId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String clientName = "Cliente confidencial";
        String clientPhone = "11987654321";
        WhatsAppSettings settings = new WhatsAppSettings();
        settings.setInstanceName("instance");

        WhatsAppSettingsRepository settingsRepository = mock(WhatsAppSettingsRepository.class);
        EvolutionApiClient evolutionApiClient = mock(EvolutionApiClient.class);
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(entityManager.createNativeQuery(
                "SELECT set_config('app.current_tenant_id', :tenantId, true)")).thenReturn(query);
        when(query.setParameter("tenantId", tenantId.toString())).thenReturn(query);
        when(settingsRepository.findByTenantId(tenantId)).thenReturn(Optional.of(settings));
        doThrow(new RestClientException("provider echoed " + clientPhone + " and " + clientName))
                .when(evolutionApiClient).sendText(
                "instance", "55" + clientPhone,
                "Olá " + clientName + "! Seu agendamento para o dia 29/10 12:00 foi recebido com sucesso pela barbearia. Te esperamos!");

        Logger logger = (Logger) LoggerFactory.getLogger(WhatsAppNotificationService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            WhatsAppNotificationService service = new WhatsAppNotificationService(
                    settingsRepository, evolutionApiClient, entityManager);
            service.handleAppointmentCreated(new AppointmentCreatedEvent(
                    appointmentId, tenantId, clientName, clientPhone,
                    OffsetDateTime.parse("2026-10-29T12:00:00-03:00")));

            String loggedMessages = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .reduce("", (first, second) -> first + "\n" + second);
            assertThat(loggedMessages)
                    .contains(appointmentId.toString())
                    .doesNotContain(clientName, clientPhone);
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void formatsUtcTimeToTenantTimezoneInCreatedMessage() {
        UUID appointmentId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String clientName = "Carlos";
        String clientPhone = "11999998888";

        WhatsAppSettings settings = new WhatsAppSettings();
        settings.setInstanceName("instance-sp");

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setTimezone("America/Sao_Paulo");

        WhatsAppSettingsRepository settingsRepository = mock(WhatsAppSettingsRepository.class);
        EvolutionApiClient evolutionApiClient = mock(EvolutionApiClient.class);
        EntityManager entityManager = mock(EntityManager.class);
        TenantRepository tenantRepository = mock(TenantRepository.class);

        Query query = mock(Query.class);
        when(entityManager.createNativeQuery("SELECT set_config('app.current_tenant_id', :tenantId, true)")).thenReturn(query);
        when(query.setParameter("tenantId", tenantId.toString())).thenReturn(query);
        when(settingsRepository.findByTenantId(tenantId)).thenReturn(Optional.of(settings));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        WhatsAppNotificationService service = new WhatsAppNotificationService(
                settingsRepository, evolutionApiClient, entityManager, tenantRepository);

        // 18:00 UTC is 15:00 in America/Sao_Paulo (UTC-3)
        service.handleAppointmentCreated(new AppointmentCreatedEvent(
                appointmentId, tenantId, clientName, clientPhone,
                OffsetDateTime.parse("2026-10-30T18:00:00Z")));

        verify(evolutionApiClient).sendText(
                "instance-sp",
                "55" + clientPhone,
                "Olá Carlos! Seu agendamento para o dia 30/10 15:00 foi recebido com sucesso pela barbearia. Te esperamos!"
        );
    }

    @Test
    void formatsUtcTimeToTenantTimezoneInCancelledMessage() {
        UUID appointmentId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String clientName = "Carlos";
        String clientPhone = "11999998888";
        String professionalName = "João";
        String professionalPhone = "11977776666";

        WhatsAppSettings settings = new WhatsAppSettings();
        settings.setInstanceName("instance-sp");
        settings.setConnected(true);

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setTimezone("America/Sao_Paulo");

        WhatsAppSettingsRepository settingsRepository = mock(WhatsAppSettingsRepository.class);
        EvolutionApiClient evolutionApiClient = mock(EvolutionApiClient.class);
        EntityManager entityManager = mock(EntityManager.class);
        TenantRepository tenantRepository = mock(TenantRepository.class);

        Query query = mock(Query.class);
        when(entityManager.createNativeQuery("SELECT set_config('app.current_tenant_id', :tenantId, true)")).thenReturn(query);
        when(query.setParameter("tenantId", tenantId.toString())).thenReturn(query);
        when(settingsRepository.findByTenantId(tenantId)).thenReturn(Optional.of(settings));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        WhatsAppNotificationService service = new WhatsAppNotificationService(
                settingsRepository, evolutionApiClient, entityManager, tenantRepository);

        // 18:00 UTC is 15:00 in America/Sao_Paulo (UTC-3)
        service.handleAppointmentCancelled(new AppointmentCancelledEvent(
                appointmentId, tenantId, clientName, clientPhone,
                professionalName, professionalPhone,
                OffsetDateTime.parse("2026-10-30T18:00:00Z")));

        verify(evolutionApiClient).sendText(
                "instance-sp",
                "55" + clientPhone,
                "Olá Carlos. Seu horário das 15:00 com o(a) João foi cancelado. Caso tenha sido um imprevisto, acesse nosso app para reagendar um novo horário."
        );

        verify(evolutionApiClient).sendText(
                "instance-sp",
                "55" + professionalPhone,
                "Aviso: O cliente Carlos cancelou o horário das 15:00. Sua agenda está livre nesse período! 🟢"
        );
    }
}
