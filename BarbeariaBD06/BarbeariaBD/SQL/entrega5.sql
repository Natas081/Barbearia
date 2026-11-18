
-- ETAPA 05 - FUNCOES, PROCEDIMENTOS E TRIGGERS
-- Projeto: BarbeariaBD
-- SGBD: MySQL 8.0+

USE barbearia;

-- ==========================================
-- 1. TABELAS AUXILIARES
-- ==========================================

-- Armazena o historico de alteracoes de precos.
CREATE TABLE IF NOT EXISTS log_preco_servico (
                                                 id_log BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                 id_servico INT NOT NULL,
                                                 preco_anterior DECIMAL(10,2) NOT NULL,
    preco_novo DECIMAL(10,2) NOT NULL,
    alterado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_bd VARCHAR(288) NOT NULL
    );

-- Armazena o fechamento dos atendimentos por lote.
CREATE TABLE IF NOT EXISTS fechamento_atendimento (
                                                      id_atendimento INT PRIMARY KEY,
                                                      horario_final TIMESTAMP NOT NULL,
                                                      numero_lote INT NOT NULL,
                                                      valor_atendimento DECIMAL(12,2) NOT NULL,
    acumulado_lote DECIMAL(14,2) NOT NULL,
    processado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- ==========================================
-- 2. REMOCAO DAS ROTINAS ANTERIORES
-- ==========================================

DROP TRIGGER IF EXISTS trg_log_preco_servico;
DROP TRIGGER IF EXISTS trg_validar_conclusao_agendamento;

DROP PROCEDURE IF EXISTS sp_atualizar_preco_servico;
DROP PROCEDURE IF EXISTS sp_gerar_fechamento;

DROP FUNCTION IF EXISTS fn_classificacao_atendimento;
DROP FUNCTION IF EXISTS fn_total_atendimento;

DELIMITER $$

-- ==========================================
-- 3. FUNCOES
-- ==========================================

-- FUNCAO 1: Calcula o valor total do atendimento.
-- Justificativa: utiliza o preco efetivamente praticado,
-- preservando o historico mesmo apos reajustes.

CREATE FUNCTION fn_total_atendimento(p_id INT)
    RETURNS DECIMAL(12,2)
    READS SQL DATA
BEGIN
    DECLARE v_total DECIMAL(12,2);

    IF p_id IS NULL OR NOT EXISTS (
        SELECT 1
        FROM atendimento
        WHERE id_atendimento = p_id
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Atendimento inexistente';
END IF;

SELECT COALESCE(SUM(preco_praticado), 0)
INTO v_total
FROM item_atendimento
WHERE id_atendimento = p_id;

RETURN v_total;
END$$

-- FUNCAO 2: Classifica o valor do atendimento.
-- Justificativa: permite analisar faixas de faturamento.
-- Utiliza estrutura condicional IF / ELSEIF / ELSE.

CREATE FUNCTION fn_classificacao_atendimento(p_id INT)
    RETURNS VARCHAR(20)
    READS SQL DATA
BEGIN
    DECLARE v_total DECIMAL(12,2);

    SET v_total = fn_total_atendimento(p_id);

    IF v_total = 0 THEN
        RETURN 'Sem servicos';
    ELSEIF v_total < 100 THEN
        RETURN 'Basico';
    ELSEIF v_total < 200 THEN
        RETURN 'Intermediario';
ELSE
        RETURN 'Premium';
END IF;
END$$

-- ==========================================
-- 4. PROCEDIMENTOS
-- ==========================================

-- PROCEDIMENTO 1: Atualiza o preco de um servico.
-- Justificativa: centraliza os reajustes e valida
-- a existencia do servico e o valor informado.

CREATE PROCEDURE sp_atualizar_preco_servico(
    IN p_id INT,
    IN p_preco DECIMAL(10,2)
)
BEGIN
    IF p_preco IS NULL OR p_preco < 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Preco invalido';
END IF;

    IF p_id IS NULL OR NOT EXISTS (
        SELECT 1
        FROM servico
        WHERE id_servico = p_id
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Servico nao encontrado';
END IF;

UPDATE servico
SET preco = p_preco
WHERE id_servico = p_id;
END$$

-- PROCEDIMENTO 2: Gera fechamento com CURSOR.
-- Justificativa: percorre os atendimentos em ordem
-- cronologica e os distribui em lotes de faturamento.
-- O lote atual depende dos valores processados antes.
-- Cada atendimento permanece indivisivel.

CREATE PROCEDURE sp_gerar_fechamento(
    IN p_limite_lote DECIMAL(14,2)
)
BEGIN
    DECLARE v_fim BOOLEAN DEFAULT FALSE;
    DECLARE v_id INT;
    DECLARE v_horario TIMESTAMP;
    DECLARE v_valor DECIMAL(12,2);
    DECLARE v_lote INT DEFAULT 1;
    DECLARE v_acumulado DECIMAL(14,2) DEFAULT 0;
    DECLARE v_qtd INT DEFAULT 0;

    DECLARE cur CURSOR FOR
SELECT a.id_atendimento, a.horario_final
FROM atendimento a
WHERE a.horario_final IS NOT NULL
  AND EXISTS (
    SELECT 1
    FROM item_atendimento i
    WHERE i.id_atendimento = a.id_atendimento
)
ORDER BY a.horario_final, a.id_atendimento;

DECLARE CONTINUE HANDLER FOR NOT FOUND
        SET v_fim = TRUE;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
BEGIN
ROLLBACK;
RESIGNAL;
END;

    IF p_limite_lote IS NULL OR p_limite_lote <= 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Limite do lote deve ser positivo';
END IF;

START TRANSACTION;

DELETE FROM fechamento_atendimento;

OPEN cur;

leitura: LOOP
        FETCH cur INTO v_id, v_horario;

        IF v_fim THEN
            LEAVE leitura;
END IF;

        SET v_valor = fn_total_atendimento(v_id);

        IF v_qtd > 0
           AND v_acumulado + v_valor > p_limite_lote THEN

            SET v_lote = v_lote + 1;
            SET v_acumulado = 0;
            SET v_qtd = 0;
END IF;

        SET v_acumulado = v_acumulado + v_valor;
        SET v_qtd = v_qtd + 1;

INSERT INTO fechamento_atendimento (
    id_atendimento,
    horario_final,
    numero_lote,
    valor_atendimento,
    acumulado_lote
)
VALUES (
           v_id,
           v_horario,
           v_lote,
           v_valor,
           v_acumulado
       );
END LOOP;

CLOSE cur;

COMMIT;
END$$

-- ==========================================
-- 5. TRIGGERS
-- ==========================================

-- TRIGGER 1: Registra alteracoes de preco.
-- Justificativa: garante auditoria automatica,
-- registrando valores anteriores e novos.

CREATE TRIGGER trg_log_preco_servico
    AFTER UPDATE ON servico
    FOR EACH ROW
BEGIN
    IF NOT (OLD.preco <=> NEW.preco) THEN
        INSERT INTO log_preco_servico (
            id_servico,
            preco_anterior,
            preco_novo,
            usuario_bd
        )
        VALUES (
            NEW.id_servico,
            OLD.preco,
            NEW.preco,
            USER()
        );
END IF;
END$$

-- TRIGGER 2: Valida conclusao de agendamento.
-- Justificativa: impede concluir um agendamento
-- sem atendimento finalizado e servico registrado.

CREATE TRIGGER trg_validar_conclusao_agendamento
    BEFORE UPDATE ON agendamento
    FOR EACH ROW
BEGIN
    IF NEW.status = 'Concluido'
       AND NOT (OLD.status <=> 'Concluido') THEN

        IF NOT EXISTS (
            SELECT 1
            FROM atendimento a
            WHERE a.id_agendamento = NEW.id_agendamento
              AND a.horario_final IS NOT NULL
              AND EXISTS (
                  SELECT 1
                  FROM item_atendimento i
                  WHERE i.id_atendimento = a.id_atendimento
              )
        ) THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Conclusao exige atendimento finalizado com servico registrado';
END IF;
END IF;
END$$

DELIMITER ;
