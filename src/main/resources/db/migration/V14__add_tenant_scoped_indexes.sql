-- Índices de suporte para consultas frequentemente filtradas por tenant_id.
-- O PostgreSQL não indexa automaticamente colunas de chave estrangeira, então
-- essas listas tendem a degradar para varreduras completas de tabela conforme
-- o volume de dados cresce.
CREATE INDEX IF NOT EXISTS idx_users_tenant_id ON users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_services_tenant_active ON services(tenant_id, active);
CREATE INDEX IF NOT EXISTS idx_professionals_tenant_active ON professionals(tenant_id, active);
