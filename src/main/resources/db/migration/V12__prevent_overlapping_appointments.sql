-- Garante no banco que um profissional não tenha horários sobrepostos
-- para agendamentos ativos.
--
-- NOTA: a criação da constraint de exclusão foi deferida para V13, que primeiro
-- limpa/normaliza os dados legados (agendamentos sem tenant/profissional válidos
-- e sobreposições históricas). Criar a constraint aqui, antes da limpeza, falharia
-- em qualquer banco com dados legados conflitantes.
CREATE EXTENSION IF NOT EXISTS btree_gist;
