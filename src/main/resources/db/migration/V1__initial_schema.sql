-- 1. Habilitar extensão para gerar UUIDs automaticamente
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ==========================================
-- Tabela: TENANTS (Barbearias)
-- ==========================================
CREATE TABLE tenants (
                         id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                         name VARCHAR(255) NOT NULL,
                         slug VARCHAR(100) NOT NULL UNIQUE, -- O "link na bio"
                         owner_email VARCHAR(255) NOT NULL,
                         plan_status VARCHAR(50) DEFAULT 'TRIAL' NOT NULL,
                         trial_expires_at TIMESTAMP NOT NULL,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índice para busca rápida pelo link na bio (essencial para o Next.js)
CREATE INDEX idx_tenants_slug ON tenants(slug);

-- ==========================================
-- Tabela: USERS (Barbeiros/Admins)
-- ==========================================
CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       tenant_id UUID NOT NULL,
                       name VARCHAR(255) NOT NULL,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL, -- Hash BCrypt
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

-- Índice para login rápido
CREATE INDEX idx_users_email ON users(email);

-- ==========================================
-- Tabela: SERVICES (Catálogo de Serviços)
-- ==========================================
CREATE TABLE services (
                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          tenant_id UUID NOT NULL,
                          name VARCHAR(255) NOT NULL,
                          price DECIMAL(10, 2) NOT NULL,
                          duration_minutes INTEGER NOT NULL, -- Ex: 30, 45, 60
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT fk_services_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

-- ==========================================
-- Tabela: APPOINTMENTS (Agenda)
-- ==========================================
CREATE TABLE appointments (
                              id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                              tenant_id UUID NOT NULL,
                              service_id UUID NOT NULL,

    -- Dados do Cliente Final (Sem login)
                              client_name VARCHAR(255) NOT NULL,
                              client_phone VARCHAR(50) NOT NULL,

    -- Controle de Tempo
                              start_time TIMESTAMP NOT NULL,
                              end_time TIMESTAMP NOT NULL,

                              status VARCHAR(50) DEFAULT 'CONFIRMED' NOT NULL, -- CONFIRMED, CANCELLED, COMPLETED
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT fk_appointments_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
                              CONSTRAINT fk_appointments_service FOREIGN KEY (service_id) REFERENCES services(id)
);

-- ÍNDICES CRÍTICOS DE PERFORMANCE 🚀
-- 1. Busca de agenda do dia: "Me dê os agendamentos da Barbearia X entre Data A e B"
CREATE INDEX idx_appointments_tenant_date ON appointments(tenant_id, start_time);

-- 2. Histórico do cliente: "Quantas vezes o telefone Y veio aqui?"
CREATE INDEX idx_appointments_client_phone ON appointments(tenant_id, client_phone);