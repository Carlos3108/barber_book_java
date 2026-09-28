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

-- Remove conflitos históricos sobrepostos, mantendo por profissional o maior conjunto
-- possível de agendamentos que não se sobrepõem entre si (varredura gulosa ordenada
-- por horário de início: mantém um agendamento apenas se ele não conflitar com o
-- último agendamento mantido para aquele profissional).
DO $$
DECLARE
    rec RECORD;
    last_end TIMESTAMPTZ;
    last_professional UUID;
BEGIN
    FOR rec IN
        SELECT id, professional_id, start_time, end_time
        FROM appointments
        WHERE status IN ('PENDING', 'CONFIRMED', 'COMPLETED')
        ORDER BY professional_id, start_time, created_at, id
    LOOP
        IF last_professional IS DISTINCT FROM rec.professional_id THEN
            last_end := NULL;
        END IF;

        IF last_end IS NOT NULL AND rec.start_time < last_end THEN
            DELETE FROM appointments WHERE id = rec.id;
        ELSE
            last_end := rec.end_time;
        END IF;

        last_professional := rec.professional_id;
    END LOOP;
END $$;

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
