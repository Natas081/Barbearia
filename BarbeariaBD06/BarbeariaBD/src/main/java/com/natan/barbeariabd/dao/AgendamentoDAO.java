package com.natan.barbeariabd.dao;

import com.natan.barbeariabd.database.Jdbc;
import com.natan.barbeariabd.model.Agendamento;
import com.natan.barbeariabd.model.Escolha;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class AgendamentoDAO {
    public List<Agendamento> listar() throws SQLException {
        return Jdbc.list("""
                SELECT a.id_agendamento, a.cpf_cliente, c.nome, a.cpf_barbeiro, p.nome,
                       a.horario_marcado, a.status, t.id_atendimento
                FROM agendamento a JOIN cliente c ON c.cpf = a.cpf_cliente
                JOIN profissional p ON p.cpf = a.cpf_barbeiro
                LEFT JOIN atendimento t ON t.id_agendamento = a.id_agendamento
                ORDER BY a.horario_marcado DESC, a.id_agendamento DESC
                """, r -> new Agendamento(r.getInt(1), r.getString(2), r.getString(3), r.getString(4), r.getString(5),
                r.getObject(6, LocalDateTime.class), r.getString(7), r.getObject(8, Integer.class)));
    }

    public List<Escolha<String>> clientes() throws SQLException {
        return Jdbc.list("SELECT cpf, nome FROM cliente ORDER BY nome, cpf",
                r -> new Escolha<>(r.getString(1), r.getString(2) + " · " + r.getString(1)));
    }

    public void salvar(Agendamento a) throws SQLException {
        if (a.id() == null) {
            if (Agendamento.CONCLUIDO.equals(a.status())) {
                throw Jdbc.rule("Cadastre o agendamento antes de registrar o atendimento e concluir.");
            }
            Jdbc.update("""
                    INSERT INTO agendamento (cpf_cliente, cpf_barbeiro, horario_marcado, status) VALUES (?, ?, ?, ?)
                    """, a.clienteCpf(), a.barbeiroCpf(), a.horario(), a.status());
        } else {
            Jdbc.requireRow(Jdbc.update("""
                    UPDATE agendamento SET cpf_cliente = ?, cpf_barbeiro = ?, horario_marcado = ?, status = ?
                    WHERE id_agendamento = ?
                    """, a.clienteCpf(), a.barbeiroCpf(), a.horario(), a.status(), a.id()));
        }
    }

    public void concluir(int id) throws SQLException {
        Jdbc.requireRow(Jdbc.update("UPDATE agendamento SET status = 'Concluido' WHERE id_agendamento = ?", id));
    }

    public void excluir(int id) throws SQLException {
        Jdbc.transaction(c -> {
            Jdbc.requireRow(Jdbc.list(c, "SELECT id_agendamento FROM agendamento WHERE id_agendamento = ? FOR UPDATE",
                    r -> r.getInt(1), id).size());
            if (!Jdbc.list(c, "SELECT id_atendimento FROM atendimento WHERE id_agendamento = ? FOR UPDATE",
                    r -> r.getInt(1), id).isEmpty()) {
                throw Jdbc.rule("Há um atendimento vinculado a este agendamento. O histórico foi preservado.");
            }
            Jdbc.requireRow(Jdbc.update(c, "DELETE FROM agendamento WHERE id_agendamento = ?", id));
            return null;
        });
    }
}
