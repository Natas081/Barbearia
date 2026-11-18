package com.natan.barbeariabd.view;

import com.natan.barbeariabd.dao.AtendimentoDAO;
import com.natan.barbeariabd.dao.ServicoDAO;
import com.natan.barbeariabd.model.Atendimento;
import com.natan.barbeariabd.model.Escolha;
import com.natan.barbeariabd.model.Servico;
import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import static com.natan.barbeariabd.view.Theme.campo;

final class AtendimentoPanel extends ListaPage<Atendimento> {
    private final AtendimentoDAO dao = new AtendimentoDAO();
    private final Consumer<Banco> banco;

    AtendimentoPanel(Consumer<Banco> banco) {
        super(banco, "Buscar atendimento ou cliente", Atendimento::id,
                new TableView.Col<>("Código", Integer.class, Atendimento::id),
                new TableView.Col<>("Agenda", Integer.class, Atendimento::agendamento),
                new TableView.Col<>("Cliente", String.class, Atendimento::cliente),
                new TableView.Col<>("Início", LocalDateTime.class, Atendimento::inicio),
                new TableView.Col<>("Fim", LocalDateTime.class, Atendimento::fim),
                new TableView.Col<>("Total", BigDecimal.class, Atendimento::total),
                new TableView.Col<>("Classificação", String.class, t -> Form.classe(t.classificacao())));
        this.banco = banco;
        botao("Atualizar", false, this::atualizar);
        botao("Serviços lançados", false, this::abrirItens);
        botao("Editar horários", false, () -> editar(selecionado(), List.of(), null));
        botao("Excluir", false, this::excluir);
        botao("＋ Novo atendimento", true, () -> novo(null));
    }

    @Override
    List<Atendimento> listar() throws Exception {
        return dao.listar();
    }

    void abrirAgendamento(int agenda) {
        busca.setText("");
        executar("Localizando o atendimento da agenda…", this::listar, dados -> {
            tabela.dados(dados);
            tabela.selecionar(t -> Objects.equals(t.agendamento(), agenda));
            if (tabela.selecionado() == null) {
                novo(agenda);
            } else {
                mensagem("Atendimento da agenda #" + agenda + " selecionado.", Theme.GREEN);
            }
        });
    }

    private void novo(Integer agenda) {
        executar("Carregando agenda disponível…", dao::agendamentosDisponiveis, opcoes -> {
            if (agenda != null && opcoes.stream().noneMatch(o -> agenda.equals(o.id()))) {
                mensagem("O agendamento não está disponível para um novo atendimento.", Theme.RED);
                return;
            }
            editar(null, opcoes, agenda);
        });
    }

    private void editar(Atendimento atual, List<Escolha<Integer>> opcoes, Integer agendamento) {
        boolean novo = atual == null;
        var agenda = new JComboBox<Escolha<Integer>>();
        Form.opcoes(agenda, opcoes, "Sem vínculo com agenda", agendamento);
        if (!novo) {
            String legenda = atual.agendamento() == null ? "Sem vínculo com agenda"
                    : "#" + atual.agendamento() + " · " + atual.cliente();
            agenda.addItem(new Escolha<>(atual.agendamento(), legenda));
            agenda.setSelectedIndex(1);
            agenda.setEnabled(false);
        }
        LocalDateTime agora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        var inicio = Form.horario(novo ? agora : atual.inicio());
        var fim = Form.horario(novo ? null : atual.fim());
        var horarios = Form.editar(this, novo ? "Novo atendimento" : "Editar horários", () -> {
            var comeco = Form.horario(inicio, "Início", true);
            var termino = Form.horario(fim, "Fim", false);
            if (termino != null && !termino.isAfter(comeco)) {
                throw new IllegalArgumentException("O fim deve ser posterior ao início.");
            }
            return new LocalDateTime[] {comeco, termino};
        }, campo("Agendamento", agenda), campo("Início", inicio), campo("Fim (opcional)", fim));
        if (horarios != null) {
            Integer id = novo ? null : atual.id();
            Integer agendaId = Form.id(agenda);
            gravar(novo ? "Atendimento registrado. Use Serviços lançados para adicionar os itens."
                    : "Horários atualizados.", () -> dao.salvar(id, agendaId, horarios[0], horarios[1]));
        }
    }

    private void excluir() {
        var atendimento = selecionado();
        if (Form.confirmar(this, "Excluir atendimento", "Excluir o atendimento #" + atendimento.id() + "?")) {
            gravar("Atendimento excluído.", () -> dao.excluir(atendimento.id()));
        }
    }

    private void abrirItens() {
        Atendimento registro = selecionado();
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Serviços do atendimento #" + registro.id(), Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        var itens = new ItensPanel(banco, registro);
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (!itens.ocupado()) {
                    dialog.dispose();
                }
            }
        });
        JPanel layout = new JPanel(new BorderLayout());
        layout.setBorder(BorderFactory.createEmptyBorder(16, 16, 10, 16));
        layout.add(itens);
        dialog.setContentPane(layout);
        dialog.setSize(810, 465);
        dialog.setMinimumSize(new Dimension(760, 400));
        dialog.setLocationRelativeTo(this);
        itens.atualizar();
        dialog.setVisible(true);
        atualizar();
    }

    private final class ItensPanel extends ListaPage<Atendimento.Item> {
        private final Atendimento visita;

        ItensPanel(Consumer<Banco> banco, Atendimento visita) {
            super(banco, "Buscar serviço lançado", Atendimento.Item::servico,
                    new TableView.Col<>("Código", Integer.class, Atendimento.Item::servico),
                    new TableView.Col<>("Serviço", String.class, Atendimento.Item::nome),
                    new TableView.Col<>("Preço praticado", BigDecimal.class, Atendimento.Item::preco));
            this.visita = visita;
            botao("Editar preço praticado", false, () -> editarItem(selecionado(), List.of()));
            botao("Remover", false, this::excluirItem);
            botao("＋ Lançar serviço", true, () -> executar("Carregando serviços…", new ServicoDAO()::listar,
                    servicos -> editarItem(null, servicos)));
        }

        @Override
        List<Atendimento.Item> listar() throws Exception {
            return dao.itens(visita.id());
        }

        private void editarItem(Atendimento.Item atual, List<Servico> catalogo) {
            boolean novo = atual == null;
            var combo = new JComboBox<Escolha<Integer>>();
            List<Escolha<Integer>> opcoes = novo
                    ? catalogo.stream().map(s -> new Escolha<>(s.getIdServico(), s.getNome())).toList()
                    : List.of(new Escolha<>(atual.servico(), atual.nome()));
            Form.opcoes(combo, opcoes, novo ? "Selecione o serviço" : null, novo ? null : atual.servico());
            combo.setEnabled(novo);
            var campoPreco = Form.campo(novo ? "" : atual.preco().toPlainString(), 25);
            if (novo) {
                combo.addActionListener(e -> campoPreco.setText(catalogo.stream()
                        .filter(s -> Objects.equals(s.getIdServico(), Form.id(combo)))
                        .map(s -> s.getPreco().toPlainString()).findFirst().orElse("")));
            }
            var item = Form.editar(this, novo ? "Lançar serviço" : "Editar preço praticado",
                    () -> new Atendimento.Item(Form.idObrigatorio(combo, "o serviço"), null,
                            Form.dinheiro(campoPreco, "Preço praticado", 8, false)),
                    campo("Serviço", combo), campo("Preço praticado (R$)", campoPreco));
            if (item != null) {
                gravar("Preço praticado salvo no atendimento.",
                        () -> dao.salvarItem(visita.id(), item.servico(), item.preco(), novo));
            }
        }

        private void excluirItem() {
            var item = selecionado();
            if (Form.confirmar(this, "Remover serviço", "Remover " + item.nome() + " deste atendimento?")) {
                gravar("Serviço removido do atendimento.", () -> dao.excluirItem(visita.id(), item.servico()));
            }
        }
    }
}
