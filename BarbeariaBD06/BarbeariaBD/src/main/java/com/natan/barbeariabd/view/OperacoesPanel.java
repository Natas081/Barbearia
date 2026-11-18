package com.natan.barbeariabd.view;

import com.natan.barbeariabd.dao.AtendimentoDAO;
import com.natan.barbeariabd.dao.OperacoesDAO;
import com.natan.barbeariabd.model.Atendimento;
import com.natan.barbeariabd.model.Escolha;
import com.natan.barbeariabd.model.Periodo;
import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

final class OperacoesPanel extends JPanel {
    private final JTabbedPane abas = new JTabbedPane();

    OperacoesPanel(Consumer<Page.Banco> banco, Runnable servicos, Runnable agendamentos) {
        super(new BorderLayout());
        setOpaque(false);
        abas.addTab("Funções SQL", new Funcoes(banco));
        abas.addTab("Histórico de preços", new Auditoria(banco, servicos));
        abas.addTab("Fechamento por lotes", new Lotes(banco));
        abas.addTab("Validação de conclusão", new Validacao(banco, agendamentos));
        abas.addChangeListener(e -> atualizar());
        add(abas);
    }

    void atualizar() {
        ((Page) abas.getSelectedComponent()).atualizar();
    }

    private static JPanel secao(String titulo, String descricao, JComponent controles) {
        JPanel topo = Theme.card(new BorderLayout(0, 12));
        JPanel texto = new JPanel(new GridLayout(0, 1, 0, 4));
        texto.setOpaque(false);
        texto.add(Theme.label(titulo, 18, Font.BOLD, Theme.TEXT));
        texto.add(Theme.muted(descricao));
        topo.add(texto, BorderLayout.NORTH);
        topo.add(controles, BorderLayout.CENTER);
        return topo;
    }

    private static class Funcoes extends Page {
        private final AtendimentoDAO dao = new AtendimentoDAO();
        private final JComboBox<Escolha<Integer>> visitas = bloquear(new JComboBox<>());
        private final JLabel valor = Theme.label("—", 35, Font.BOLD, Theme.GOLD);
        private final JLabel classe = Theme.label("—", 24, Font.BOLD, Theme.TEXT);
        private final JTextArea detalhes = new JTextArea("Selecione um atendimento para consultar as duas funções.");

        Funcoes(Consumer<Banco> banco) {
            super(banco);
            visitas.setPreferredSize(new Dimension(285, 34));
            var controles = Theme.row(7);
            controles.add(visitas);
            controles.add(bloquear(Theme.button("Executar funções", true, this::executarFuncoes)));
            controles.add(bloquear(Theme.button("Atualizar lista", false, this::atualizar)));
            add(secao("Total e classificação por atendimento", "Selecione um atendimento registrado no MySQL.", controles),
                    BorderLayout.NORTH);

            JPanel conteudo = new JPanel(new BorderLayout(0, 12));
            conteudo.setOpaque(false);
            JPanel cards = new JPanel(new GridLayout(1, 2, 12, 0));
            cards.setOpaque(false);
            cards.add(indicador("fn_total_atendimento", valor));
            cards.add(indicador("fn_classificacao_atendimento", classe));
            conteudo.add(cards, BorderLayout.NORTH);
            JPanel contexto = Theme.card(new BorderLayout());
            detalhes.setEditable(false);
            detalhes.setLineWrap(true);
            detalhes.setWrapStyleWord(true);
            detalhes.setOpaque(false);
            detalhes.setForeground(Theme.MUTED);
            contexto.add(detalhes);
            conteudo.add(contexto, BorderLayout.CENTER);
            add(conteudo, BorderLayout.CENTER);
            visitas.addActionListener(e -> limpar());
        }

        private JPanel indicador(String rotulo, JLabel valor) {
            JPanel card = Theme.card(new BorderLayout(0, 9));
            card.add(Theme.muted(rotulo), BorderLayout.NORTH);
            card.add(valor, BorderLayout.CENTER);
            return card;
        }

        @Override
        void atualizar() {
            Integer id = Form.id(visitas);
            executar("Carregando atendimentos…", dao::opcoes, opcoes -> {
                Form.opcoes(visitas, opcoes, "Selecione o atendimento", id);
                limpar();
            });
        }

        private void limpar() {
            valor.setText("—");
            classe.setText("—");
            detalhes.setText("Execute as funções para ver o total e a classificação.");
        }

        private void executarFuncoes() {
            validar(() -> {
                int id = Form.idObrigatorio(visitas, "o atendimento");
                limpar();
                executar("Executando as funções do MySQL…", () -> dao.demonstrarFuncoes(id), this::exibir);
            });
        }

        private void exibir(Atendimento t) {
            valor.setText(Form.moeda(t.total()));
            classe.setText(Form.classe(t.classificacao()));
            String pessoas = t.agendamento() == null ? "Sem vínculo com agenda"
                    : "Agenda #" + t.agendamento() + " · " + t.cliente() + " / " + t.barbeiro()
                    + " · " + Form.status(t.statusAgenda());
            String fim = t.fim() == null ? "Em aberto" : t.fim().format(Form.DATA_HORA);
            detalhes.setText("#" + t.id() + " · " + pessoas + "\nInício " + t.inicio().format(Form.DATA_HORA) + " · " + fim);
            mensagem("Funções executadas no MySQL.", Theme.GREEN);
        }
    }

    private static class Auditoria extends Page {
        private final OperacoesDAO dao = new OperacoesDAO();
        private final TableView<Object[]> tabela = new TableView<>();
        private final JTextField inicio = bloquear(Form.data());
        private final JTextField fim = bloquear(Form.data());
        private final JTextField servico = bloquear(Form.campo("", 9));

        Auditoria(Consumer<Banco> banco, Runnable servicos) {
            super(banco);
            JPanel filtros = Theme.row(7);
            filtros.add(Theme.campo("De", inicio));
            filtros.add(Theme.campo("Até", fim));
            filtros.add(Theme.campo("Código do serviço", servico));
            var acoes = Theme.row(7);
            acoes.add(bloquear(Theme.button("Atualizar histórico", true, this::atualizar)));
            acoes.add(bloquear(Theme.button("Limpar filtros", false, () -> {
                inicio.setText("");
                fim.setText("");
                servico.setText("");
                atualizar();
            })));
            acoes.add(Theme.button("Ir para serviços", false, servicos));
            JPanel controles = new JPanel(new BorderLayout(0, 9));
            controles.setOpaque(false);
            controles.add(filtros, BorderLayout.NORTH);
            controles.add(acoes, BorderLayout.SOUTH);
            add(secao("Histórico de reajustes", "Registros produzidos pelo trigger trg_log_preco_servico.", controles),
                    BorderLayout.NORTH);
            var card = Theme.card(new BorderLayout());
            card.add(tabela);
            add(card, BorderLayout.CENTER);
        }

        @Override
        void atualizar() {
            validar(() -> {
                Periodo p = Form.periodo(inicio, fim);
                Integer id = servico.getText().isBlank() ? null : Form.inteiro(servico, "Código do serviço");
                tabela.dados(List.of());
                executar("Consultando o histórico…", () -> dao.auditoria(p, id), r -> {
                    tabela.vazio("Nenhum reajuste no recorte selecionado.");
                    TableView.resultado(tabela, r);
                });
            });
        }
    }

    private static class Lotes extends Page {
        private final OperacoesDAO dao = new OperacoesDAO();
        private final TableView<Object[]> lotes = new TableView<>();
        private final TableView<Object[]> detalhes = new TableView<>();
        private final JTextField limite = bloquear(Form.campo("", 12));
        private final JLabel detalhesTitulo = Theme.muted("Atendimentos de todos os lotes");

        Lotes(Consumer<Banco> banco) {
            super(banco);
            var controls = Theme.row(7);
            controls.add(Theme.campo("Limite por lote (R$)", limite));
            var acoes = Theme.row(7);
            acoes.add(bloquear(Theme.button("Gerar fechamento", true, this::gerar)));
            acoes.add(bloquear(Theme.button("Atualizar", false, this::atualizar)));
            acoes.add(Theme.button("Exibir todos os lotes", false, lotes.table()::clearSelection));
            JPanel barra = new JPanel(new BorderLayout(0, 8));
            barra.setOpaque(false);
            barra.add(controls, BorderLayout.NORTH);
            barra.add(acoes, BorderLayout.SOUTH);
            add(secao("Fechamento financeiro", "Selecione um lote para conferir os atendimentos, valores e acumulados.", barra),
                    BorderLayout.NORTH);
            lotes.vazio("Nenhum lote gerado.");
            detalhes.vazio("Nenhum atendimento no fechamento.");
            lotes.table().getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    Object[] item = lotes.selecionado();
                    detalhesTitulo.setText(item == null ? "Atendimentos de todos os lotes" : "Atendimentos do lote " + item[0]);
                    detalhes.filtrar(item == null ? null : r -> Objects.equals(r[0], item[0]));
                }
            });
            var resumo = Theme.card(new BorderLayout(0, 7));
            resumo.add(Theme.muted("Resumo por lote"), BorderLayout.NORTH);
            resumo.add(lotes);
            var itens = Theme.card(new BorderLayout(0, 7));
            itens.add(detalhesTitulo, BorderLayout.NORTH);
            itens.add(detalhes);
            var split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, resumo, itens);
            split.setResizeWeight(.42);
            split.setContinuousLayout(true);
            split.setBorder(null);
            resumo.setMinimumSize(new Dimension(500, 96));
            itens.setMinimumSize(new Dimension(500, 106));
            add(split, BorderLayout.CENTER);
            SwingUtilities.invokeLater(() -> split.setDividerLocation(.42));
        }

        @Override
        void atualizar() {
            executar("Consultando fechamento…", dao::fechamento, this::exibir);
        }

        private void gerar() {
            validar(() -> {
                if (ocupado()) {
                    return;
                }
                BigDecimal valor = Form.dinheiro(limite, "Limite por lote", 12, true);
                boolean confirmar = Form.confirmar(this, "Reconstruir fechamento",
                        "O procedimento substitui o fechamento auxiliar anterior por novos lotes.\n"
                                + "Os atendimentos e os serviços são preservados. Um atendimento acima do limite fica em lote próprio.\n"
                                + "Gerar fechamento com limite de " + Form.moeda(valor) + " por lote?");
                if (confirmar) {
                    executar("Gerando fechamento no MySQL…", () -> dao.gerarFechamento(valor), dados -> {
                        exibir(dados);
                        mensagem("Fechamento atualizado: " + dados.lotes().linhas().size() + " lotes gerados.", Theme.GREEN);
                    });
                }
            });
        }

        private void exibir(OperacoesDAO.Fechamento dados) {
            TableView.resultado(lotes, dados.lotes());
            detalhes.filtrar(null);
            TableView.resultado(detalhes, dados.atendimentos());
        }
    }

    private static class Validacao extends Page {
        Validacao(Consumer<Banco> banco, Runnable abrir) {
            super(banco);
            var acoes = Theme.row(7);
            acoes.add(Theme.button("Ir para agendamentos", true, abrir));
            var conteudo = new JTextArea("""
                    1. Selecione um agendamento sem atendimento e clique em Concluir.
                    2. Confira a rejeição do trigger no rodapé da tela de agendamentos.
                    3. Abra Atendimento, registre os horários e lance pelo menos um serviço.
                    4. Volte ao agendamento e clique em Concluir para aplicar a mudança de status.
                    """);
            conteudo.setEditable(false);
            conteudo.setLineWrap(true);
            conteudo.setWrapStyleWord(true);
            conteudo.setOpaque(false);
            conteudo.setForeground(Theme.TEXT);
            conteudo.setFont(UIManager.getFont("Label.font").deriveFont(15f));
            var painel = Theme.card(new BorderLayout(0, 14));
            painel.add(Theme.label("trg_validar_conclusao_agendamento", 18, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
            painel.add(conteudo, BorderLayout.CENTER);
            painel.add(acoes, BorderLayout.SOUTH);
            add(painel);
        }

        @Override
        void atualizar() {
            // A validação funciona no UPDATE real do módulo Agendamentos.
        }
    }
}
