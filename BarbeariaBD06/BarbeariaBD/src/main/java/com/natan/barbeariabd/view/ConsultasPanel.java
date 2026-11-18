package com.natan.barbeariabd.view;

import com.natan.barbeariabd.dao.ConsultaDAO;
import com.natan.barbeariabd.dao.ConsultaDAO.Consulta;
import com.natan.barbeariabd.model.Agendamento;
import com.natan.barbeariabd.model.Escolha;
import com.natan.barbeariabd.model.Periodo;
import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

final class ConsultasPanel extends Page {
    private final ConsultaDAO dao = new ConsultaDAO();
    private final JList<Consulta> catalogo = bloquear(new JList<>(Consulta.values()));
    private final JLabel titulo = Theme.label("", 20, Font.BOLD, Theme.TEXT);
    private final JLabel contexto = Theme.muted("");
    private final JLabel tipo = Theme.label("", 12, Font.BOLD, Theme.GOLD);
    private final JTextField busca = bloquear(Form.campo("", 14));
    private final JTextField inicio = bloquear(Form.data());
    private final JTextField fim = bloquear(Form.data());
    private final JComboBox<Escolha<String>> status = bloquear(new JComboBox<>());
    private final TableView<Object[]> tabela = new TableView<>();

    ConsultasPanel(Consumer<Banco> banco) {
        super(banco);
        JPanel layout = new JPanel(new BorderLayout(12, 0));
        layout.setOpaque(false);
        JPanel indice = Theme.card(new BorderLayout(0, 14));
        indice.setPreferredSize(new Dimension(248, 400));
        indice.add(Theme.label("Catálogo", 15, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        catalogo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        catalogo.setBackground(Theme.CARD);
        catalogo.setSelectionBackground(Color.decode("#4A3C2A"));
        catalogo.setFixedCellHeight(42);
        indice.add(Theme.scroll(catalogo));
        layout.add(indice, BorderLayout.WEST);

        JPanel dados = Theme.card(new BorderLayout(0, 16));
        var topo = new JPanel();
        topo.setOpaque(false);
        topo.setLayout(new BoxLayout(topo, BoxLayout.Y_AXIS));
        for (JLabel texto : new JLabel[] {titulo, contexto, tipo}) {
            texto.setAlignmentX(Component.LEFT_ALIGNMENT);
            texto.setMaximumSize(new Dimension(Integer.MAX_VALUE, texto.getFontMetrics(texto.getFont()).getHeight() + 2));
        }
        topo.add(tipo);
        topo.add(Box.createVerticalStrut(3));
        topo.add(titulo);
        topo.add(Box.createVerticalStrut(6));
        topo.add(contexto);
        topo.add(Box.createVerticalStrut(16));

        JPanel controles = new JPanel(new GridLayout(1, 4, 8, 0));
        controles.setOpaque(false);
        controles.setAlignmentX(Component.LEFT_ALIGNMENT);
        busca.putClientProperty("JTextField.placeholderText", "Nome ou código");
        controles.add(Theme.campo("Pesquisar", busca));
        controles.add(Theme.campo("De", inicio));
        controles.add(Theme.campo("Até", fim));
        controles.add(Theme.campo("Status", status));
        Form.opcoes(status, Agendamento.STATUS.stream().map(s -> new Escolha<>(s, Form.status(s))).toList(), "Todos", null);
        topo.add(controles);
        topo.add(Box.createVerticalStrut(9));

        JPanel acoes = Theme.row(7);
        acoes.setAlignmentX(Component.LEFT_ALIGNMENT);
        acoes.add(bloquear(Theme.button("Executar consulta", true, this::atualizar)));
        acoes.add(bloquear(Theme.button("Limpar filtros", false, this::limpar)));
        topo.add(acoes);
        busca.addActionListener(e -> atualizar());
        dados.add(topo, BorderLayout.NORTH);
        dados.add(tabela);
        layout.add(dados, BorderLayout.CENTER);
        add(layout, BorderLayout.CENTER);

        catalogo.setSelectedIndex(0);
        descrever();
        catalogo.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && catalogo.getSelectedValue() != null) {
                redefinir();
                descrever();
                atualizar();
            }
        });
    }

    private void descrever() {
        Consulta consulta = catalogo.getSelectedValue();
        titulo.setText(consulta.titulo);
        contexto.setText(consulta.descricao);
        contexto.setToolTipText(consulta.descricao);
        tipo.setText(consulta.tipo);
        inicio.setEnabled(consulta.temPeriodo);
        fim.setEnabled(consulta.temPeriodo);
        status.setEnabled(consulta.temStatus);
    }

    private void redefinir() {
        busca.setText("");
        inicio.setText("");
        fim.setText("");
        status.setSelectedIndex(0);
    }

    private void limpar() {
        redefinir();
        atualizar();
    }

    @Override
    void atualizar() {
        if (ocupado()) {
            return;
        }
        validar(() -> {
            Consulta consulta = catalogo.getSelectedValue();
            String filtro = busca.getText().strip();
            Periodo intervalo = consulta.temPeriodo ? Form.periodo(inicio, fim) : new Periodo(null, null);
            String situacao = Form.id(status);
            tabela.vazio("Executando consulta…");
            tabela.dados(java.util.List.of());
            // O estado vazio continua informativo se a consulta falhar.
            tabela.vazio("Nenhum resultado exibido.");
            executar("Consultando o MySQL…", () -> dao.executar(consulta, filtro, intervalo, situacao), resultado -> {
                tabela.vazio("Nenhum registro corresponde à consulta e aos filtros.");
                TableView.resultado(tabela, resultado);
            });
        });
    }
}
