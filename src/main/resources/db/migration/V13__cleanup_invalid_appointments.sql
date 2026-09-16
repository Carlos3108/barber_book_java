-- Limpa registros legados inconsistentes antes de reforçar a integridade da agenda.
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Corrige tenant_id inconsistente quando o profissional aponta para uma barbearia válida.
UPDATE appointments a
SET tenant_id = p.tenant_id
FROM professionals p
WHERE a.professional_id = p.id
  AND (
      a.tenant_id IS NULL
      OR a.tenant_id <> p.tenant_id
  );

-- Remove registros que não têm vínculo confiável com tenant, profissional, serviço ou horários válidos.
DELETE FROM appointments a
WHERE a.professional_id IS NULL
   OR a.service_id IS NULL
   OR a.tenant_id IS NULL
   OR a.start_time IS NULL
   OR a.end_time IS NULL
   OR a.start_time >= a.end_time
   OR NOT EXISTS (SELECT 1 FROM professionals p WHERE p.id = a.professional_id)
   OR NOT EXISTS (SELECT 1 FROM services s WHERE s.id = a.service_id)
   OR NOT EXISTS (SELECT 1 FROM tenants t WHERE t.id = a.tenant_id);

-- Remove conflitos históricos sobrepostos preservando o registro mais antigo por profissional.
WITH overlapping AS (
    SELECT a.id,
           ROW_NUMBER() OVER (
               PARTITION BY a.professional_id
               ORDER BY a.start_time, a.created_at, a.id
           ) AS rn
    FROM appointments a
    WHERE a.status IN ('PENDING', 'CONFIRMED', 'COMPLETED')
      AND EXISTS (
          SELECT 1
          FROM appointments b
          WHERE b.professional_id = a.professional_id
            AND b.id <> a.id
            AND b.status IN ('PENDING', 'CONFIRMED', 'COMPLETED')
            AND b.start_time < a.end_time
            AND a.start_time < b.end_time
      )
)
DELETE FROM appointments a
USING overlapping o
WHERE a.id = o.id
  AND o.rn > 1;

-- Garante que as colunas críticas da agenda não fiquem vazias.
ALTER TABLE appointments
    ALTER COLUMN professional_id SET NOT NULL,
    ALTER COLUMN tenant_id SET NOT NULL,
    ALTER COLUMN start_time SET NOT NULL,
    ALTER COLUMN end_time SET NOT NULL;

-- Reforça a regra de não sobreposição para agendamentos ativos.
ALTER TABLE appointments
    DROP CONSTRAINT IF EXISTS appointments_no_overlap;

ALTER TABLE appointments
    ADD CONSTRAINT appointments_no_overlap
        EXCLUDE USING gist (
            professional_id WITH =,
            tstzrange(start_time, end_time, '[)') WITH &&
        )
        WHERE (status IN ('PENDING', 'CONFIRMED', 'COMPLETED'));
