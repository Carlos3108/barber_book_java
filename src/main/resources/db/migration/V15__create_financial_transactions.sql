-- 1. Criação da tabela base com Constraints
CREATE TABLE financial_transactions
(
    id               UUID                        NOT NULL,
    created_at       TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at       TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    tenant_id        UUID                        NOT NULL,
    type             VARCHAR(50)                 NOT NULL,
    category         VARCHAR(50)                 NOT NULL,
    amount           DECIMAL(12, 2)              NOT NULL,
    transaction_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    description      VARCHAR(255),
    appointment_id   UUID,
    professional_id  UUID,

    CONSTRAINT pk_financial_transactions PRIMARY KEY (id),

    -- Foreign Keys para garantir integridade
    CONSTRAINT fk_fin_trans_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_fin_trans_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL,
    CONSTRAINT fk_fin_trans_professional FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE SET NULL,

    -- Check constraints de domínio
    CONSTRAINT chk_fin_trans_amount CHECK (amount >= 0),
    CONSTRAINT chk_fin_trans_type CHECK (type IN ('CREDIT', 'DEBIT'))
);

-- 2. Índices para otimizar os dashboards financeiros
CREATE INDEX idx_fin_trans_tenant_date ON financial_transactions (tenant_id, transaction_date);
CREATE INDEX idx_fin_trans_professional ON financial_transactions (professional_id);
CREATE INDEX idx_fin_trans_appointment ON financial_transactions (appointment_id);

-- 3. Habilitar a segurança Multi-Tenant (RLS)
ALTER TABLE financial_transactions ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON financial_transactions FOR ALL USING (tenant_id = current_setting('app.current_tenant')::UUID);
CREATE POLICY tenant_insert_policy ON financial_transactions FOR INSERT WITH CHECK (tenant_id = current_setting('app.current_tenant')::UUID);

-- 4. Alterações na tabela Tenants (Mantendo o que você já fez)
ALTER TABLE tenants ALTER COLUMN closing_time SET NOT NULL;
ALTER TABLE tenants ALTER COLUMN opening_time SET NOT NULL;
ALTER TABLE tenants ALTER COLUMN slot_interval SET NOT NULL;
ALTER TABLE tenants ALTER COLUMN timezone TYPE VARCHAR(255) USING (timezone::VARCHAR(255));
ALTER TABLE tenants ALTER COLUMN timezone SET NOT NULL;