-- Garante no banco que um profissional não tenha horários sobrepostos
-- para agendamentos ativos.
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE appointments
    DROP CONSTRAINT IF EXISTS appointments_no_overlap;

ALTER TABLE appointments
    ADD CONSTRAINT appointments_no_overlap
        EXCLUDE USING gist (
            professional_id WITH =,
            tstzrange(start_time, end_time, '[)') WITH &&
        )
        WHERE (status IN ('PENDING', 'CONFIRMED', 'COMPLETED'));
