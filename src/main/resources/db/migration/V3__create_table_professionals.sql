CREATE TABLE professionals (
                               id UUID PRIMARY KEY,
                               name VARCHAR(255) NOT NULL,
                               active BOOLEAN NOT NULL DEFAULT TRUE,
                               tenant_id UUID NOT NULL,
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT fk_professionals_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

ALTER TABLE appointments
    ADD COLUMN professional_id UUID NOT NULL;

ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_professional FOREIGN KEY (professional_id) REFERENCES professionals(id);