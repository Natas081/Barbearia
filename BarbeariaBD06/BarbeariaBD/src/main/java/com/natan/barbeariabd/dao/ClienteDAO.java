package com.natan.barbeariabd.dao;

import com.natan.barbeariabd.database.Jdbc;
import com.natan.barbeariabd.model.Cliente;
import java.sql.SQLException;
import java.util.List;

public class ClienteDAO {
    public void inserir(Cliente cliente) throws SQLException {
        Jdbc.update("INSERT INTO cliente (cpf, nome, rua, numero) VALUES (?, ?, ?, ?)",
                cliente.getCpf(), cliente.getNome(), cliente.getRua(), cliente.getNumero());
    }

    public void atualizar(Cliente cliente) throws SQLException {
        Jdbc.requireRow(Jdbc.update("UPDATE cliente SET nome = ?, rua = ?, numero = ? WHERE cpf = ?",
                cliente.getNome(), cliente.getRua(), cliente.getNumero(), cliente.getCpf()));
    }

    public void excluir(String cpf) throws SQLException {
        Jdbc.transaction(c -> {
            Jdbc.requireRow(Jdbc.list(c, "SELECT cpf FROM cliente WHERE cpf = ? FOR UPDATE", r -> r.getString(1), cpf).size());
            if (!Jdbc.list(c, "SELECT 1 FROM dependente WHERE cpf_cliente = ? LIMIT 1 FOR UPDATE", r -> 1, cpf).isEmpty()) {
                throw Jdbc.rule("O cliente possui dependentes cadastrados. A exclusão removeria esses vínculos.");
            }
            Jdbc.requireRow(Jdbc.update(c, "DELETE FROM cliente WHERE cpf = ?", cpf));
            return null;
        });
    }

    public List<Cliente> listar() throws SQLException {
        return Jdbc.list("SELECT cpf, nome, rua, numero FROM cliente ORDER BY nome, cpf",
                r -> new Cliente(r.getString(1), r.getString(2), r.getString(3), r.getString(4)));
    }
}
