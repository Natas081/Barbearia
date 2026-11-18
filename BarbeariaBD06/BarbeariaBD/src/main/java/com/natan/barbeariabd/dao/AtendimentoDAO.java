package com.natan.barbeariabd.dao;

import com.natan.barbeariabd.database.Jdbc;
import com.natan.barbeariabd.model.Agendamento;
import com.natan.barbeariabd.model.Atendimento;
import com.natan.barbeariabd.model.Escolha;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class AtendimentoDAO {
    private static final String DETALHES = """
            SELECT t.id_atendimento, a.id_agendamento, c.nome, p.nome, a.status, t.horario_inicio, t.horario_final,
                   fn_total_atendimento(t.id_atendimento), fn_classificacao_atendimento(t.id_atendimento)
            FROM atendimento t LEFT JOIN agendamento a ON a.id_agendamento = t.id_agendamento
            LEFT JOIN cliente c ON c.cpf = a.cpf_cliente LEFT JOIN profissional p ON p.cpf = a.cpf_barbeiro
            """;

    public List<Atendimento> listar() throws SQLException {
        return Jdbc.list(DETALHES + " ORDER BY t.horario_inicio DESC, t.id_atendimento DESC", AtendimentoDAO::ler);
    }

    public List<Escolha<Integer>> opcoes() throws SQLException {
        return Jdbc.list("""
                SELECT t.id_atendimento, COALESCE(c.nome, 'Sem vínculo com agenda')
                FROM atendimento t LEFT JOIN agendamento a ON a.id_agendamento = t.id_agendamento
                LEFT JOIN cliente c ON c.cpf = a.cpf_cliente
                ORDER BY t.horario_inicio DESC, t.id_atendimento DESC
                """, r -> new Escolha<>(r.getInt(1), "#" + r.getInt(1) + " · " + r.getString(2)));
    }

    public Atendimento demonstrarFuncoes(int id) throws SQLException {
        List<Atendimento> linhas = Jdbc.list(DETALHES + " WHERE t.id_atendimento = ?", AtendimentoDAO::ler, id);
        Jdbc.requireRow(linhas.size());
        return linhas.getFirst();
    }

    private static Atendimento ler(java.sql.ResultSet r) throws SQLException {
        return new Atendimento(r.getInt(1), r.getObject(2, Integer.class), r.getString(3), r.getString(4), r.getString(5),
                r.getObject(6, LocalDateTime.class), r.getObject(7, LocalDateTime.class),
                r.getBigDecimal(8), r.getString(9));
    }

    public List<Escolha<Integer>> agendamentosDisponiveis() throws SQLException {
        return Jdbc.list("""
                SELECT a.id_agendamento, c.nome FROM agendamento a JOIN cliente c ON c.cpf = a.cpf_cliente
                WHERE a.status <> 'Concluido'
                  AND NOT EXISTS (SELECT 1 FROM atendimento t WHERE t.id_agendamento = a.id_agendamento)
                ORDER BY a.id_agendamento DESC
                """, r -> new Escolha<>(r.getInt(1), "#" + r.getInt(1) + " · " + r.getString(2)));
    }

    public void salvar(Integer id, Integer agenda, LocalDateTime inicio, LocalDateTime fim) throws SQLException {
        Jdbc.transaction(c -> {
            if (id == null) {
                verificarAgenda(c, agenda);
                Jdbc.update(c, "INSERT INTO atendimento (id_agendamento, horario_inicio, horario_final) VALUES (?, ?, ?)",
                        agenda, inicio, fim);
            } else {
                bloquearAtendimento(c, id);
                Jdbc.requireRow(Jdbc.update(c, "UPDATE atendimento SET horario_inicio = ?, horario_final = ? WHERE id_atendimento = ?",
                        inicio, fim, id));
            }
            return null;
        });
    }

    public void excluir(int id) throws SQLException {
        Jdbc.transaction(c -> {
            bloquearAtendimento(c, id);
            if (!Jdbc.list(c, "SELECT 1 FROM item_atendimento WHERE id_atendimento = ? LIMIT 1 FOR UPDATE",
                    r -> 1, id).isEmpty() || !Jdbc.list(c, """
                    SELECT 1 FROM fechamento_atendimento WHERE id_atendimento = ? FOR UPDATE
                    """, r -> 1, id).isEmpty()) {
                throw Jdbc.rule("Este atendimento possui serviços ou fechamento registrado. O histórico foi preservado.");
            }
            Jdbc.requireRow(Jdbc.update(c, "DELETE FROM atendimento WHERE id_atendimento = ?", id));
            return null;
        });
    }

    public List<Atendimento.Item> itens(int id) throws SQLException {
        return Jdbc.list("""
                SELECT s.id_servico, s.nome, i.preco_praticado FROM item_atendimento i
                JOIN servico s ON s.id_servico = i.id_servico WHERE i.id_atendimento = ? ORDER BY s.nome
                """, r -> new Atendimento.Item(r.getInt(1), r.getString(2), r.getBigDecimal(3)), id);
    }

    public void salvarItem(int atendimento, int servico, BigDecimal preco, boolean novo) throws SQLException {
        Jdbc.transaction(c -> {
            bloquearAtendimento(c, atendimento);
            if (novo) {
                Jdbc.update(c, "INSERT INTO item_atendimento (id_atendimento, id_servico, preco_praticado) VALUES (?, ?, ?)",
                        atendimento, servico, preco);
            } else {
                Jdbc.requireRow(Jdbc.update(c, """
                        UPDATE item_atendimento SET preco_praticado = ? WHERE id_atendimento = ? AND id_servico = ?
                        """, preco, atendimento, servico));
            }
            return null;
        });
    }

    public void excluirItem(int atendimento, int servico) throws SQLException {
        Jdbc.transaction(c -> {
            bloquearAtendimento(c, atendimento);
            Jdbc.requireRow(Jdbc.update(c, "DELETE FROM item_atendimento WHERE id_atendimento = ? AND id_servico = ?",
                    atendimento, servico));
            return null;
        });
    }

    private void bloquearAtendimento(Connection c, int id) throws SQLException {
        String consulta = "SELECT id_agendamento FROM atendimento WHERE id_atendimento = ?";
        List<Integer> anterior = Jdbc.list(c, consulta, r -> r.getObject(1, Integer.class), id);
        Jdbc.requireRow(anterior.size());
        // A conclusão também bloqueia o agendamento. Adquirir o bloqueio na mesma ordem serializa os dois fluxos.
        verificarAgenda(c, anterior.getFirst());
        List<Integer> atual = Jdbc.list(c, consulta + " FOR UPDATE", r -> r.getObject(1, Integer.class), id);
        Jdbc.requireRow(atual.size());
        if (!Objects.equals(anterior.getFirst(), atual.getFirst())) {
            throw Jdbc.rule("O vínculo do atendimento mudou em outra sessão. Atualize a lista.");
        }
    }

    private void verificarAgenda(Connection c, Integer agenda) throws SQLException {
        if (agenda == null) {
            return;
        }
        List<String> status = Jdbc.list(c, "SELECT status FROM agendamento WHERE id_agendamento = ? FOR UPDATE",
                r -> r.getString(1), agenda);
        Jdbc.requireRow(status.size());
        if (Agendamento.CONCLUIDO.equals(status.getFirst())) {
            throw Jdbc.rule("Reabra o agendamento antes de editar o atendimento ou seus serviços.");
        }
    }
}
