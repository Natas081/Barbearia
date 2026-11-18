package com.natan.barbeariabd.dao;

import com.natan.barbeariabd.database.Jdbc;
import com.natan.barbeariabd.model.Periodo;
import com.natan.barbeariabd.model.Resultado;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OperacoesDAO {
    public record Fechamento(Resultado lotes, Resultado atendimentos) {}

    public Resultado auditoria(Periodo periodo, Integer servico) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT l.id_log AS Registro, l.id_servico AS Código,
                    COALESCE(s.nome, CONCAT('Serviço #', l.id_servico, ' (excluído)')) AS Serviço,
                    l.preco_anterior AS Anterior, l.preco_novo AS Novo, l.alterado_em AS Data,
                    l.usuario_bd AS `Usuário MySQL`
                FROM log_preco_servico l LEFT JOIN servico s ON s.id_servico = l.id_servico WHERE 1=1
                """);
        List<Object> parametros = new ArrayList<>();
        periodo.aplicar(sql, parametros, "l.alterado_em");
        if (servico != null) {
            sql.append(" AND l.id_servico = ?");
            parametros.add(servico);
        }
        sql.append(" ORDER BY l.alterado_em DESC, l.id_log DESC");
        return ResultadosSQL.ler(sql.toString(), parametros.toArray());
    }

    public Fechamento fechamento() throws SQLException {
        return Jdbc.transaction(this::lerFechamento);
    }

    public Fechamento gerarFechamento(BigDecimal limite) throws SQLException {
        return Jdbc.connect(c -> {
            int lock = Jdbc.list(c, "SELECT GET_LOCK(CONCAT(DATABASE(), '.fechamento'), 5)", r -> r.getInt(1))
                    .getFirst();
            if (lock != 1) {
                throw Jdbc.rule("Já existe um fechamento em execução. Tente novamente em instantes.");
            }
            try (var call = c.prepareCall("{call sp_gerar_fechamento(?)}")) {
                call.setBigDecimal(1, limite);
                call.execute();
            }
            c.setAutoCommit(false);
            Fechamento resultado = lerFechamento(c);
            c.commit();
            // A conexão dedicada libera o bloqueio ao fechar, inclusive em caso de erro.
            return resultado;
        });
    }

    private Fechamento lerFechamento(Connection c) throws SQLException {
        Resultado lotes = ResultadosSQL.ler(c, """
                SELECT numero_lote AS Lote, COUNT(*) AS Atendimentos, SUM(valor_atendimento) AS Total,
                    MAX(processado_em) AS Processado
                FROM fechamento_atendimento GROUP BY numero_lote ORDER BY numero_lote
                """);
        Resultado atendimentos = ResultadosSQL.ler(c, """
                SELECT numero_lote AS Lote, id_atendimento AS Atendimento, horario_final AS Fim,
                    valor_atendimento AS Valor, acumulado_lote AS Acumulado, processado_em AS Processado
                FROM fechamento_atendimento ORDER BY numero_lote, horario_final, id_atendimento
                """);
        return new Fechamento(lotes, atendimentos);
    }
}
