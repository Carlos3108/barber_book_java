-- ==============================================================================
-- MIGRAÇÃO V2: Refatoração Arquitetural e Criação de Profissionais
-- ==============================================================================

-- 1. Removendo a "Verdade Dupla" da tabela de tenants
-- O e-mail do dono já vive na tabela de 'users'
ALTER TABLE tenants
DROP COLUMN owner_email;

-- 2. Adicionando o Soft Delete no Catálogo de Serviços
-- Usamos 'DEFAULT TRUE' para que serviços existentes (se houver) continuem ativos e não quebrem a tabela
ALTER TABLE services
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

-- 3. Atualizando a Agenda para suportar Fuso Horário (OffsetDateTime)
-- Convertendo o TIMESTAMP normal para TIMESTAMP WITH TIME ZONE
ALTER TABLE appointments
ALTER COLUMN start_time TYPE TIMESTAMP WITH TIME ZONE USING start_time AT TIME ZONE 'UTC',
ALTER COLUMN end_time TYPE TIMESTAMP WITH TIME ZONE USING end_time AT TIME ZONE 'UTC';
