package org.azdev.barber_book.config;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.azdev.barber_book.security.SecurityUtils;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantRLSAspect {

    private final EntityManager entityManager;
    private final SecurityUtils securityUtils;

    @Before("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void setTenantIdInPostgresSession() {

        Session session = entityManager.unwrap(Session.class);

        try {
            UUID tenantId = securityUtils.getCurrentTenantId();

            if (tenantId != null) {
                session.doWork(connection -> {
                    try (var statement = connection.createStatement()) {
                        statement.execute("SET LOCAL app.current_tenant_id = '" + tenantId + "'");
                    }
                });
                log.debug("RLS Configurado para Tenant: {}", tenantId);
            }
        } catch (Exception e) {
            session.doWork(connection -> {
                try (var statement = connection.createStatement()) {
                    statement.execute("SET LOCAL app.access_mode = 'PUBLIC_ACCESS'");
                }
            });
            log.debug("RLS Configurado para Acesso Público (Link na Bio)");
        }
    }
}