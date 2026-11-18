package com.natan.barbeariabd.dao;

import com.natan.barbeariabd.database.Jdbc;
import com.natan.barbeariabd.model.Escolha;
import com.natan.barbeariabd.model.Profissional;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ProfissionalDAO {
    public List<Profissional> listar() throws SQLException {
        return Jdbc.list("""
                SELECT p.cpf, p.nome, p.rua, p.numero, b.especialidade, b.horario_trabalho,
                       b.cpf_supervisor, g.bonificacao
                FROM profissional p
                LEFT JOIN barbeiro b ON b.cpf_profissional = p.cpf
                LEFT JOIN gerente g ON g.cpf_profissional = p.cpf
                ORDER BY p.nome, p.cpf
                """, r -> {
            String especialidade = r.getString(5);
            var barbeiro = especialidade == null ? null :
                    new Profissional.Barbeiro(especialidade, r.getString(6), r.getString(7));
            return new Profissional(r.getString(1), r.getString(2), r.getString(3), r.getString(4),
                    barbeiro, r.getBigDecimal(8));
        });
    }

    public List<Escolha<String>> barbeiros() throws SQLException {
        return Jdbc.list("""
                SELECT p.cpf, p.nome FROM profissional p JOIN barbeiro b ON b.cpf_profissional = p.cpf
                ORDER BY p.nome, p.cpf
                """, r -> new Escolha<>(r.getString(1), r.getString(2) + " · " + r.getString(1)));
    }

    public void salvar(Profissional p, boolean novo) throws SQLException {
        Jdbc.transaction(c -> {
            if (novo) {
                Jdbc.update(c, "INSERT INTO profissional (cpf, nome, rua, numero) VALUES (?, ?, ?, ?)",
                        p.cpf(), p.nome(), p.rua(), p.numero());
            } else {
                Jdbc.requireRow(Jdbc.update(c, "UPDATE profissional SET nome = ?, rua = ?, numero = ? WHERE cpf = ?",
                        p.nome(), p.rua(), p.numero(), p.cpf()));
            }
            salvarBarbeiro(c, p);
            if (p.bonificacao() == null) {
                Jdbc.update(c, "DELETE FROM gerente WHERE cpf_profissional = ?", p.cpf());
            } else {
                Jdbc.update(c, """
                        INSERT INTO gerente (cpf_profissional, bonificacao) VALUES (?, ?) AS novo
                        ON DUPLICATE KEY UPDATE bonificacao = novo.bonificacao
                        """, p.cpf(), p.bonificacao());
            }
            return null;
        });
    }

    private void salvarBarbeiro(Connection c, Profissional p) throws SQLException {
        var b = p.barbeiro();
        if (b == null) {
            protegerSupervisao(c, p.cpf());
            Jdbc.update(c, "DELETE FROM barbeiro WHERE cpf_profissional = ?", p.cpf());
            return;
        }
        if (p.cpf().equals(b.supervisor())) {
            throw Jdbc.rule("Selecione outro barbeiro como supervisor.");
        }
        Jdbc.update(c, """
                INSERT INTO barbeiro (cpf_profissional, especialidade, horario_trabalho, cpf_supervisor)
                VALUES (?, ?, ?, ?) AS novo
                ON DUPLICATE KEY UPDATE especialidade = novo.especialidade,
                    horario_trabalho = novo.horario_trabalho, cpf_supervisor = novo.cpf_supervisor
                """, p.cpf(), b.especialidade(), b.horario(), b.supervisor());
    }

    public void excluir(String cpf) throws SQLException {
        Jdbc.transaction(c -> {
            Jdbc.requireRow(Jdbc.list(c, "SELECT cpf FROM profissional WHERE cpf = ? FOR UPDATE",
                    r -> r.getString(1), cpf).size());
            protegerSupervisao(c, cpf);
            Jdbc.requireRow(Jdbc.update(c, "DELETE FROM profissional WHERE cpf = ?", cpf));
            return null;
        });
    }

    private void protegerSupervisao(Connection c, String cpf) throws SQLException {
        Jdbc.list(c, "SELECT cpf_profissional FROM barbeiro WHERE cpf_profissional = ? FOR UPDATE",
                r -> r.getString(1), cpf);
        if (!Jdbc.list(c, """
                SELECT 1 FROM barbeiro WHERE cpf_supervisor = ? AND cpf_profissional <> ? LIMIT 1 FOR UPDATE
                """, r -> 1, cpf, cpf).isEmpty()) {
            throw Jdbc.rule("Reatribua os barbeiros supervisionados antes de remover este vínculo.");
        }
    }
}
