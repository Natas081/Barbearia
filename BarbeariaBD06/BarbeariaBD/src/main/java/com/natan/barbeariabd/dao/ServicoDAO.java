package com.natan.barbeariabd.dao;

import com.natan.barbeariabd.database.Jdbc;
import com.natan.barbeariabd.model.Servico;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class ServicoDAO {
    public void inserir(Servico servico) throws SQLException {
        Jdbc.connect(c -> {
            try (var s = c.prepareStatement(
                    "INSERT INTO servico (nome, descricao, duracao, preco) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                Jdbc.bind(s, servico.getNome(), servico.getDescricao(), servico.getDuracao(), servico.getPreco());
                s.executeUpdate();
                try (var keys = s.getGeneratedKeys()) {
                    if (keys.next()) {
                        servico.setIdServico(keys.getInt(1));
                    }
                }
            }
            return null;
        });
    }

    public void atualizar(Servico servico) throws SQLException {
        Jdbc.requireRow(Jdbc.update("UPDATE servico SET nome = ?, descricao = ?, duracao = ? WHERE id_servico = ?",
                servico.getNome(), servico.getDescricao(), servico.getDuracao(), servico.getIdServico()));
    }

    public void reajustar(int id, BigDecimal anterior, BigDecimal novo) throws SQLException {
        Jdbc.transaction(c -> {
            var precos = Jdbc.list(c, "SELECT preco FROM servico WHERE id_servico = ? FOR UPDATE",
                    r -> r.getBigDecimal(1), id);
            Jdbc.requireRow(precos.size());
            if (precos.getFirst().compareTo(anterior) != 0) {
                throw Jdbc.rule("O preço foi alterado em outra sessão. Atualize a lista antes de reajustar.");
            }
            try (var call = c.prepareCall("{call sp_atualizar_preco_servico(?, ?)}")) {
                call.setInt(1, id);
                call.setBigDecimal(2, novo);
                call.execute();
            }
            return null;
        });
    }

    public void excluir(int idServico) throws SQLException {
        Jdbc.requireRow(Jdbc.update("DELETE FROM servico WHERE id_servico = ?", idServico));
    }

    public List<Servico> listar() throws SQLException {
        return Jdbc.list("SELECT id_servico, nome, descricao, duracao, preco FROM servico ORDER BY nome",
                r -> new Servico(r.getInt(1), r.getString(2), r.getString(3), r.getInt(4), r.getBigDecimal(5)));
    }
}
