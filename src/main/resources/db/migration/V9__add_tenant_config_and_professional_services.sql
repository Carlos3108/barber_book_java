ALTER TABLE tenants ADD COLUMN opening_time TIME DEFAULT '09:00:00';
ALTER TABLE tenants ADD COLUMN closing_time TIME DEFAULT '18:00:00';
ALTER TABLE tenants ADD COLUMN timezone VARCHAR(50) DEFAULT 'America/Sao_Paulo';

CREATE TABLE professional_services (
                                       professional_id UUID NOT NULL,
                                       service_id UUID NOT NULL,
                                       PRIMARY KEY (professional_id, service_id),
                                       CONSTRAINT fk_professional FOREIGN KEY (professional_id) REFERENCES professionals (id) ON DELETE CASCADE,
                                       CONSTRAINT fk_service FOREIGN KEY (service_id) REFERENCES services (id) ON DELETE CASCADE
);