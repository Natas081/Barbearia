-- Executar uma vez, depois dos scripts da base e da Etapa 05.
-- Valores nulos preservam a distinção entre registros antigos e horários marcados.
USE barbearia;

ALTER TABLE agendamento
    ADD COLUMN horario_marcado DATETIME NULL,
    ADD INDEX idx_agendamento_horario (horario_marcado);
