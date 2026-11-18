package com.natan.barbeariabd.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

public final class MainFrame extends JFrame {
    private enum Rota {
        DASHBOARD("Dashboard", "Acompanhe a operação, o faturamento e os atendimentos da barbearia."),
        CLIENTES("Clientes", "Organize os cadastros e os endereços dos clientes."),
        PROFISSIONAIS("Profissionais", "Gerencie barbeiros, gerentes e vínculos de supervisão."),
        SERVICOS("Serviços", "Gerencie o catálogo, a duração e os preços dos serviços."),
        AGENDA("Agendamentos", "Marque horários, acompanhe os status e conclua os agendamentos atendidos."),
        ATENDIMENTOS("Atendimentos", "Registre horários e serviços com o preço praticado em cada atendimento."),
        CONSULTAS("Consultas e views", "Explore as consultas acadêmicas e as views do MySQL."),
        BANCO("Operações do banco", "Execute funções, confira reajustes e gere fechamentos financeiros.");

        final String titulo;
        final String descricao;

        Rota(String titulo, String descricao) {
            this.titulo = titulo;
            this.descricao = descricao;
        }
    }

    private final CardLayout cards = new CardLayout();
    private final JPanel paginas = new JPanel(cards);
    private final JLabel titulo = Theme.label("", 32, Font.BOLD, Theme.TEXT);
    private final JLabel descricao = Theme.muted("");
    private final JLabel conexao = Theme.muted("●  MySQL · aguardando consulta");
    private final JLabel ultima = Theme.muted("Sem atualização registrada");
    private final Map<Rota, JToggleButton> botoes = new EnumMap<>(Rota.class);
    private final Map<Rota, Runnable> atualizar = new EnumMap<>(Rota.class);
    private final AtendimentoPanel atendimentos = new AtendimentoPanel(this::banco);

    public MainFrame() {
        super("BarbeariaBD");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 640));
        Rectangle available = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.max(1080, Math.min(1380, available.width - 36)),
                Math.max(640, Math.min(880, available.height - 35)));

        registrar(Rota.DASHBOARD, new DashboardPanel(this::banco));
        registrar(Rota.CLIENTES, new ClientePanel(this::banco));
        registrar(Rota.PROFISSIONAIS, new ProfissionalPanel(this::banco));
        registrar(Rota.SERVICOS, new ServicoPanel(this::banco));
        registrar(Rota.AGENDA, new AgendamentoPanel(this::banco, this::abrirAtendimento));
        registrar(Rota.ATENDIMENTOS, atendimentos);
        registrar(Rota.CONSULTAS, new ConsultasPanel(this::banco));
        var operacoes = new OperacoesPanel(this::banco,
                () -> abrir(Rota.SERVICOS, true), () -> abrir(Rota.AGENDA, true));
        paginas.add(operacoes, Rota.BANCO.name());
        atualizar.put(Rota.BANCO, operacoes::atualizar);

        JPanel root = new JPanel(new BorderLayout());
        root.add(navegacao(), BorderLayout.WEST);
        JPanel conteudo = new JPanel(new BorderLayout(0, 17));
        conteudo.setBorder(new EmptyBorder(24, 24, 11, 24));
        JPanel cabecalho = new JPanel(new BorderLayout(0, 4));
        cabecalho.add(titulo, BorderLayout.NORTH);
        cabecalho.add(descricao, BorderLayout.SOUTH);
        conteudo.add(cabecalho, BorderLayout.NORTH);
        conteudo.add(paginas, BorderLayout.CENTER);

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                new EmptyBorder(9, 1, 0, 1)));
        rodape.add(conexao, BorderLayout.WEST);
        ultima.setHorizontalAlignment(SwingConstants.RIGHT);
        rodape.add(ultima, BorderLayout.CENTER);
        conteudo.add(rodape, BorderLayout.SOUTH);
        root.add(conteudo);
        setContentPane(root);
        setLocationRelativeTo(null);
        abrir(Rota.DASHBOARD, true);
    }

    private void registrar(Rota rota, Page pagina) {
        paginas.add(pagina, rota.name());
        atualizar.put(rota, pagina::atualizar);
    }

    private void abrir(Rota rota, boolean carregar) {
        titulo.setText(rota.titulo);
        descricao.setText(rota.descricao);
        botoes.get(rota).setSelected(true);
        cards.show(paginas, rota.name());
        if (carregar) {
            atualizar.get(rota).run();
        }
    }

    private void abrirAtendimento(int agenda) {
        abrir(Rota.ATENDIMENTOS, false);
        atendimentos.abrirAgendamento(agenda);
    }

    private JPanel navegacao() {
        JPanel painel = new JPanel();
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
        painel.setBackground(Theme.SIDE);
        painel.setPreferredSize(new Dimension(211, 500));
        painel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER), new EmptyBorder(23, 12, 16, 12)));

        JPanel marca = new JPanel(new BorderLayout(9, 0));
        marca.setAlignmentX(Component.LEFT_ALIGNMENT);
        marca.setOpaque(false);
        marca.setBorder(new EmptyBorder(0, 5, 0, 0));
        var icone = new JLabel(new NavIcon(null, 34));
        icone.setBorder(new EmptyBorder(0, 0, 0, 0));
        marca.add(icone, BorderLayout.WEST);
        var texto = new JPanel(new GridLayout(0, 1, 0, 1));
        texto.setOpaque(false);
        texto.add(Theme.label("BarbeariaBD", 19, Font.BOLD, Theme.TEXT));
        texto.add(Theme.label("Gestão de barbearia", 11, Font.PLAIN, Theme.MUTED));
        marca.add(texto);
        marca.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        painel.add(marca);
        painel.add(Box.createVerticalStrut(30));

        var group = new ButtonGroup();
        secao(painel, "GESTÃO");
        for (Rota rota : Rota.values()) {
            if (rota == Rota.CONSULTAS) {
                painel.add(Box.createVerticalStrut(20));
                secao(painel, "DADOS");
            }
            var botao = new NavButton(rota);
            group.add(botao);
            botoes.put(rota, botao);
            painel.add(botao);
            painel.add(Box.createVerticalStrut(4));
        }
        painel.add(Box.createVerticalGlue());
        return painel;
    }

    private void secao(JPanel painel, String nome) {
        JLabel label = Theme.label(nome, 10, Font.BOLD, Theme.MUTED);
        label.setBorder(new EmptyBorder(0, 10, 8, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        painel.add(label);
    }

    private void banco(Page.Banco estado) {
        boolean ativo = estado == Page.Banco.CONECTADO;
        conexao.setText(ativo ? "●  MySQL conectado" : "●  MySQL indisponível");
        conexao.setForeground(ativo ? Theme.GREEN : Theme.RED);
        if (ativo) {
            ultima.setText("Último contato · " + LocalDateTime.now().format(Form.DATA_HORA_SEGUNDOS));
        }
    }

    private final class NavButton extends JToggleButton {
        NavButton(Rota rota) {
            super(rota.titulo, new NavIcon(rota, 17));
            setHorizontalAlignment(SwingConstants.LEFT);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setBorder(new EmptyBorder(0, 12, 0, 6));
            setContentAreaFilled(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 41));
            setPreferredSize(new Dimension(183, 41));
            setIconTextGap(10);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addChangeListener(e -> setForeground(isSelected() ? Theme.GOLD : Theme.TEXT));
            addActionListener(e -> abrir(rota, true));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (isSelected()) {
                g.setColor(Color.decode("#312922"));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 11, 11);
                g.setColor(Theme.GOLD);
                g.fillRoundRect(0, 11, 3, getHeight() - 22, 3, 3);
            } else if (getModel().isRollover()) {
                g.setColor(Theme.CARD);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 11, 11);
            }
            if (hasFocus()) {
                g.setColor(Theme.BORDER);
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private record NavIcon(Rota rota, int size) implements Icon {
        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }

        @Override
        public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.translate(x, y);
            g.scale(size / 20.0, size / 20.0);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(1.55f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(rota == null || c instanceof JToggleButton b && b.isSelected() ? Theme.GOLD : Theme.MUTED);
            switch (rota == null ? Rota.SERVICOS : rota) {
                case SERVICOS -> {
                    g.drawOval(2, 2, 5, 5);
                    g.drawOval(2, 13, 5, 5);
                    g.drawLine(7, 6, 18, 16);
                    g.drawLine(7, 14, 18, 4);
                }
                case DASHBOARD -> {
                    g.drawRoundRect(2, 2, 7, 7, 2, 2);
                    g.drawRoundRect(11, 2, 7, 7, 2, 2);
                    g.drawRoundRect(2, 11, 7, 7, 2, 2);
                    g.drawRoundRect(11, 11, 7, 7, 2, 2);
                }
                case CLIENTES -> {
                    g.drawOval(7, 2, 6, 6);
                    g.drawRoundRect(4, 11, 12, 6, 6, 6);
                }
                case PROFISSIONAIS -> {
                    g.drawOval(3, 3, 5, 5);
                    g.drawOval(12, 3, 5, 5);
                    g.drawRoundRect(1, 11, 8, 5, 4, 4);
                    g.drawRoundRect(11, 11, 8, 5, 4, 4);
                }
                case AGENDA -> {
                    g.drawRoundRect(2, 4, 16, 14, 3, 3);
                    g.drawLine(2, 8, 18, 8);
                    g.drawLine(6, 2, 6, 6);
                    g.drawLine(14, 2, 14, 6);
                    g.drawLine(7, 12, 13, 12);
                }
                case ATENDIMENTOS -> {
                    g.drawRoundRect(4, 3, 12, 15, 3, 3);
                    g.drawLine(7, 3, 13, 3);
                    g.drawLine(7, 10, 9, 12);
                    g.drawLine(9, 12, 13, 8);
                }
                case CONSULTAS -> {
                    g.drawRoundRect(2, 3, 16, 14, 3, 3);
                    g.drawLine(2, 7, 18, 7);
                    g.drawLine(7, 7, 7, 17);
                    g.drawLine(7, 12, 18, 12);
                }
                case BANCO -> {
                    g.drawOval(3, 2, 14, 5);
                    g.drawArc(3, 7, 14, 5, 180, 180);
                    g.drawArc(3, 13, 14, 5, 180, 180);
                    g.drawLine(3, 4, 3, 15);
                    g.drawLine(17, 4, 17, 15);
                }
            }
            g.dispose();
        }
    }
}
