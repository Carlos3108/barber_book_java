ALTER TABLE professional_services ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Permitir leitura de vínculos do próprio tenant"
ON professional_services
FOR SELECT
               USING (
               professional_id IN (SELECT id FROM professionals)
               );

CREATE POLICY "Permitir modificação de vínculos do próprio tenant"
ON professional_services
FOR ALL
USING (
    professional_id IN (SELECT id FROM professionals)
)
WITH CHECK (
    professional_id IN (SELECT id FROM professionals) AND
    service_id IN (SELECT id FROM services)
);