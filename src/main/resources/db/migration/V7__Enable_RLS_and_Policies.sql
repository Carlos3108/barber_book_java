-- ==============================================================================
-- MIGRATION: Habilitar Row Level Security (RLS) e Políticas de Isolamento
-- ==============================================================================

-- 1. Habilitar o RLS nas tabelas principais (seguro rodar múltiplas vezes)
ALTER TABLE services ENABLE ROW LEVEL SECURITY;
ALTER TABLE professionals ENABLE ROW LEVEL SECURITY;
ALTER TABLE appointments ENABLE ROW LEVEL SECURITY;

ALTER TABLE services FORCE ROW LEVEL SECURITY;
ALTER TABLE professionals FORCE ROW LEVEL SECURITY;
ALTER TABLE appointments FORCE ROW LEVEL SECURITY;

-- 2. Limpar políticas residuais (Prevenção de falhas do Flyway)
DROP POLICY IF EXISTS "Isolamento de Tenant - Servicos" ON services;
DROP POLICY IF EXISTS "Isolamento de Tenant - Profissionais" ON professionals;
DROP POLICY IF EXISTS "Isolamento de Tenant - Agendamentos" ON appointments;
DROP POLICY IF EXISTS "Leitura Publica - Servicos" ON services;
DROP POLICY IF EXISTS "Leitura Publica - Profissionais" ON professionals;
DROP POLICY IF EXISTS "Criacao Publica - Agendamentos" ON appointments;

-- 3. Criar as Políticas (Policies) Privadas
CREATE POLICY "Isolamento de Tenant - Servicos" ON services
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);

CREATE POLICY "Isolamento de Tenant - Profissionais" ON professionals
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);

CREATE POLICY "Isolamento de Tenant - Agendamentos" ON appointments
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);

-- 4. BYPASS PARA ROTAS PÚBLICAS (A Vitrine do Cliente)
CREATE POLICY "Leitura Publica - Servicos" ON services
    FOR SELECT
                                                                      USING (
                                                                      current_setting('app.access_mode', true) = 'PUBLIC_ACCESS'
                                                                      AND active = true
                                                                      );

CREATE POLICY "Leitura Publica - Profissionais" ON professionals
    FOR SELECT
                   USING (
                   current_setting('app.access_mode', true) = 'PUBLIC_ACCESS'
                   AND active = true
                   );

CREATE POLICY "Criacao Publica - Agendamentos" ON appointments
    FOR INSERT
    WITH CHECK (
        current_setting('app.access_mode', true) = 'PUBLIC_ACCESS'
    );