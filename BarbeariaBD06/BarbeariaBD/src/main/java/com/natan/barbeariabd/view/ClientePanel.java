package com.natan.barbeariabd.view;

import com.natan.barbeariabd.dao.ClienteDAO;
import com.natan.barbeariabd.model.Cliente;
import java.util.List;
import java.util.function.Consumer;
import static com.natan.barbeariabd.view.Theme.campo;

final class ClientePanel extends ListaPage<Cliente> {
    private final ClienteDAO dao = new ClienteDAO();

    ClientePanel(Consumer<Banco> banco) {
        super(banco, "Buscar nome, CPF ou endereço", Cliente::getCpf,
                new TableView.Col<>("Nome", String.class, Cliente::getNome),
                new TableView.Col<>("CPF", String.class, Cliente::getCpf),
                new TableView.Col<>("Rua", String.class, Cliente::getRua),
                new TableView.Col<>("Número", String.class, Cliente::getNumero));
        botao("Atualizar", false, this::atualizar);
        botao("Editar", false, () -> editar(selecionado()));
        botao("Excluir", false, this::excluir);
        botao("＋ Novo cliente", true, () -> editar(null));
    }

    @Override
    List<Cliente> listar() throws Exception {
        return dao.listar();
    }

    private void editar(Cliente atual) {
        boolean novo = atual == null;
        var cpf = Form.campo(novo ? "" : atual.getCpf(), 28);
        cpf.setEditable(novo);
        var nome = Form.campo(novo ? "" : atual.getNome(), 28);
        var rua = Form.campo(novo ? "" : atual.getRua(), 28);
        var numero = Form.campo(novo ? "" : atual.getNumero(), 28);
        Cliente cliente = Form.editar(this, novo ? "Novo cliente" : "Editar cliente", () -> new Cliente(
                        Form.cpf(cpf), Form.texto(nome, "Nome", 100, true), Form.texto(rua, "Rua", 100, true),
                        Form.texto(numero, "Número", 10, true)),
                campo("CPF", cpf), campo("Nome completo", nome), campo("Rua", rua), campo("Número", numero));
        if (cliente != null) {
            if (novo) {
                gravar("Cliente cadastrado.", () -> dao.inserir(cliente));
            } else {
                gravar("Cliente atualizado.", () -> dao.atualizar(cliente));
            }
        }
    }

    private void excluir() {
        var cliente = selecionado();
        if (Form.confirmar(this, "Excluir cliente", "Excluir " + cliente.getNome() + " e seus telefones?")) {
            gravar("Cliente excluído.", () -> dao.excluir(cliente.getCpf()));
        }
    }
}
