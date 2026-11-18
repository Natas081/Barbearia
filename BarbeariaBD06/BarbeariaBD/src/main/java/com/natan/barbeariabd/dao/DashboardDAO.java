package com.natan.barbeariabd.dao;

import com.natan.barbeariabd.database.Jdbc;
import com.natan.barbeariabd.model.Dashboard.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class DashboardDAO {
    public Dados consultar(Filtro filtro) throws SQLException {
        return Jdbc.transaction(c -> {
            List<Agenda> agenda = agenda(c, filtro);
            Sql visitasSql = visitasSql(filtro);
            Object[] parametros = visitasSql.valores().toArray();
            List<Visita> visitas = Jdbc.list(c, visitasSql.texto() + """
                    SELECT f.cpf_cliente, f.cpf_barbeiro, DATE(f.horario_final),
                           COALESCE(SUM(i.preco_praticado), 0), COUNT(i.id_servico),
                           TIMESTAMPDIFF(SECOND, f.horario_inicio, f.horario_final),
                           fn_classificacao_atendimento(f.id_atendimento)
                    FROM filtrados f LEFT JOIN item_atendimento i ON i.id_atendimento = f.id_atendimento
                    GROUP BY f.id_atendimento, f.cpf_cliente, f.cpf_barbeiro, f.horario_inicio, f.horario_final
                    ORDER BY f.horario_final, f.id_atendimento
                    """, r -> new Visita(r.getString(1), r.getString(2), r.getObject(3, LocalDate.class),
                    r.getBigDecimal(4), r.getLong(5), r.getLong(6), r.getString(7)), parametros);
            List<ServicoMedido> servicos = Jdbc.list(c, visitasSql.texto() + """
                    SELECT s.nome, COUNT(*), SUM(i.preco_praticado)
                    FROM filtrados f JOIN item_atendimento i ON i.id_atendimento = f.id_atendimento
                    JOIN servico s ON s.id_servico = i.id_servico GROUP BY s.id_servico, s.nome
                    ORDER BY s.nome
                    """, r -> new ServicoMedido(r.getString(1), r.getLong(2), r.getBigDecimal(3)), parametros);
            long clientes = filtro.ativo()
                    ? distintos(Stream.concat(agenda.stream().map(Agenda::cliente), visitas.stream().map(Visita::cliente)))
                    : Jdbc.list(c, "SELECT COUNT(*) FROM cliente", r -> r.getLong(1)).getFirst();
            long profissionais = filtro.ativo()
                    ? distintos(Stream.concat(agenda.stream().map(Agenda::barbeiro), visitas.stream().map(Visita::barbeiro)))
                    : Jdbc.list(c, "SELECT COUNT(*) FROM profissional", r -> r.getLong(1)).getFirst();
            return new Dados(clientes, profissionais, agenda, visitas, servicos);
        });
    }

    private long distintos(Stream<String> cpfs) {
        return cpfs.filter(java.util.Objects::nonNull).distinct().count();
    }

    private List<Agenda> agenda(Connection c, Filtro filtro) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT a.cpf_cliente, a.cpf_barbeiro, p.nome, a.status
                FROM agendamento a JOIN profissional p ON p.cpf = a.cpf_barbeiro
                LEFT JOIN atendimento t ON t.id_agendamento = a.id_agendamento
                WHERE 1=1
                """);
        List<Object> valores = new ArrayList<>();
        filtro.periodo().aplicar(sql, valores, "COALESCE(a.horario_marcado, t.horario_inicio)");
        restringir(sql, valores, filtro);
        return Jdbc.list(c, sql.toString(), r -> new Agenda(r.getString(1), r.getString(2), r.getString(3),
                r.getString(4)), valores.toArray());
    }

    private record Sql(String texto, List<Object> valores) {}

    private Sql visitasSql(Filtro filtro) {
        StringBuilder sql = new StringBuilder("""
                WITH filtrados AS (
                    SELECT t.id_atendimento, t.horario_inicio, t.horario_final, a.cpf_cliente, a.cpf_barbeiro
                    FROM atendimento t LEFT JOIN agendamento a ON a.id_agendamento = t.id_agendamento
                    WHERE t.horario_final IS NOT NULL
                """);
        List<Object> valores = new ArrayList<>();
        filtro.periodo().aplicar(sql, valores, "t.horario_final");
        restringir(sql, valores, filtro);
        sql.append(") ");
        return new Sql(sql.toString(), valores);
    }

    private void restringir(StringBuilder sql, List<Object> valores, Filtro filtro) {
        if (filtro.barbeiro() != null) {
            sql.append(" AND a.cpf_barbeiro = ?");
            valores.add(filtro.barbeiro());
        }
        if (filtro.status() != null) {
            sql.append(" AND a.status = ?");
            valores.add(filtro.status());
        }
        if (filtro.servico() != null) {
            sql.append("""
                     AND EXISTS (SELECT 1 FROM item_atendimento f
                                 WHERE f.id_atendimento = t.id_atendimento AND f.id_servico = ?)
                    """);
            valores.add(filtro.servico());
        }
    }
}
