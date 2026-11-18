package com.natan.barbeariabd.view;

import com.natan.barbeariabd.dao.ServicoDAO;
import com.natan.barbeariabd.model.Servico;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import static com.natan.barbeariabd.view.Theme.campo;

final class ServicoPanel extends ListaPage<Servico> {
    private final ServicoDAO dao = new ServicoDAO();

    ServicoPanel(Consumer<Banco> banco) {
        super(banco, "Buscar serviço ou código", Servico::getIdServico,
                new TableView.Col<>("Código", Integer.class, Servico::getIdServico),
                new TableView.Col<>("Serviço", String.class, Servico::getNome),
                new TableView.Col<>("Descrição", String.class, Servico::getDescricao),
                new TableView.Col<>("Minutos", Integer.class, Servico::getDuracao),
                new TableView.Col<>("Preço atual", BigDecimal.class, Servico::getPreco));
        botao("Atualizar", false, this::atualizar);
        botao("Editar", false, () -> editar(selecionado()));
        botao("Reajustar preço", false, this::reajustar);
        botao("Excluir", false, this::excluir);
        botao("＋ Novo serviço", true, () -> editar(null));
    }

    @Override
    List<Servico> listar() throws Exception {
        return dao.listar();
    }

    private void editar(Servico atual) {
        boolean novo = atual == null;
        var nome = Form.campo(novo ? "" : atual.getNome(), 28);
        var descricao = Form.campo(novo ? "" : atual.getDescricao(), 28);
        var duracao = Form.campo(novo ? "" : Integer.toString(atual.getDuracao()), 28);
        var preco = Form.campo(novo ? "" : atual.getPreco().toPlainString(), 28);
        preco.setEditable(novo);
        Servico item = Form.editar(this, novo ? "Novo serviço" : "Editar serviço", () -> new Servico(
                        novo ? null : atual.getIdServico(), Form.texto(nome, "Nome", 100, true),
                        Form.texto(descricao, "Descrição", 255, false), Form.inteiro(duracao, "Duração"),
                        novo ? Form.dinheiro(preco, "Preço", 8, false) : atual.getPreco()),
                campo("Nome", nome), campo("Descrição", descricao), campo("Duração (minutos)", duracao),
                campo(novo ? "Preço inicial (R$)" : "Preço atual (use Reajustar preço)", preco));
        if (item != null) {
            if (novo) {
                gravar("Serviço cadastrado.", () -> dao.inserir(item));
            } else {
                gravar("Serviço atualizado.", () -> dao.atualizar(item));
            }
        }
    }

    private void reajustar() {
        var servico = selecionado();
        var novo = Form.campo("", 24);
        BigDecimal preco = Form.editar(this, "Reajustar preço", () -> Form.dinheiro(novo, "Novo preço", 8, false),
                Theme.muted(servico.getNome() + " · Atual " + Form.moeda(servico.getPreco())), campo("Novo preço (R$)", novo));
        if (preco == null) {
            return;
        }
        if (preco.compareTo(servico.getPreco()) == 0) {
            mensagem("O novo preço é igual ao atual.", Theme.MUTED);
            return;
        }
        String texto = "Confirmar o reajuste de " + Form.moeda(servico.getPreco()) + " para " + Form.moeda(preco) + "?";
        if (Form.confirmar(this, "Confirmar reajuste", texto)) {
            gravar("Preço atualizado. Consulte o reajuste no histórico em Operações do banco.",
                    () -> dao.reajustar(servico.getIdServico(), servico.getPreco(), preco));
        }
    }

    private void excluir() {
        var servico = selecionado();
        if (Form.confirmar(this, "Excluir serviço", "Excluir " + servico.getNome() + "?")) {
            gravar("Serviço excluído.", () -> dao.excluir(servico.getIdServico()));
        }
    }
}
