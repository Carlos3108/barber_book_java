package org.azdev.barber_book.config;

import jakarta.persistence.EntityManager;
import org.azdev.barber_book.security.SecurityUtils;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantRLSAspectTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private Session session;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    private TenantRLSAspect aspect;

    @Test
    void setsTenantIdWhenAuthenticatedUserHasTenant() throws SQLException {
        aspect = new TenantRLSAspect(entityManager, securityUtils);
        UUID tenantId = UUID.randomUUID();

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(connection.createStatement()).thenReturn(statement);

        aspect.setTenantIdInPostgresSession();

        ArgumentCaptor<Work> workCaptor = ArgumentCaptor.forClass(Work.class);
        verify(session).doWork(workCaptor.capture());

        workCaptor.getValue().execute(connection);

        verify(statement).execute("SET LOCAL app.current_tenant_id = '" + tenantId + "'");
    }

    @Test
    void setsPublicAccessWhenSecurityUtilsThrows() throws SQLException {
        aspect = new TenantRLSAspect(entityManager, securityUtils);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(securityUtils.getCurrentTenantId()).thenThrow(new IllegalStateException("no auth"));
        when(connection.createStatement()).thenReturn(statement);

        aspect.setTenantIdInPostgresSession();

        ArgumentCaptor<Work> workCaptor = ArgumentCaptor.forClass(Work.class);
        verify(session).doWork(workCaptor.capture());

        workCaptor.getValue().execute(connection);

        verify(statement).execute("SET LOCAL app.access_mode = 'PUBLIC_ACCESS'");
    }

    @Test
    void doesNothingWhenTenantIdIsNull() {
        aspect = new TenantRLSAspect(entityManager, securityUtils);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(securityUtils.getCurrentTenantId()).thenReturn(null);

        aspect.setTenantIdInPostgresSession();

        verify(session, org.mockito.Mockito.never()).doWork(org.mockito.ArgumentMatchers.any());
    }
}
