ALTER TABLE professionals
    ADD COLUMN IF NOT EXISTS phone VARCHAR(20);

CREATE TABLE IF NOT EXISTS whatsapp_settings (
                                   id UUID PRIMARY KEY,
                                   tenant_id UUID NOT NULL UNIQUE,
                                   instance_name VARCHAR(255),
                                   connected BOOLEAN NOT NULL DEFAULT FALSE,
                                   is_reminder_enabled BOOLEAN NOT NULL DEFAULT FALSE,
                                   is_daily_summary_enabled BOOLEAN NOT NULL DEFAULT FALSE,
                                   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                   CONSTRAINT fk_whatsapp_settings_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE
);

ALTER TABLE whatsapp_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE whatsapp_settings FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_policy ON whatsapp_settings;
CREATE POLICY tenant_isolation_policy ON whatsapp_settings
    USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::UUID);

CREATE INDEX IF NOT EXISTS idx_whatsapp_settings_tenant_id ON whatsapp_settings(tenant_id);