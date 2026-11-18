package com.natan.barbeariabd.view;

import com.natan.barbeariabd.dao.DashboardDAO;
import com.natan.barbeariabd.dao.ProfissionalDAO;
import com.natan.barbeariabd.dao.ServicoDAO;
import com.natan.barbeariabd.model.Agendamento;
import com.natan.barbeariabd.model.Dashboard.*;
import com.natan.barbeariabd.model.Escolha;
import com.natan.barbeariabd.service.Estatistica;
import com.natan.barbeariabd.service.SerieTemporal;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

final class DashboardPanel extends Page {
    private final DashboardDAO dao = new DashboardDAO();
    private final JTextField inicio = bloquear(Form.data());
    private final JTextField fim = bloquear(Form.data());
    private final JComboBox<Escolha<String>> barbeiros = bloquear(new JComboBox<>());
    private final JComboBox<Escolha<Integer>> servicos = bloquear(new JComboBox<>());
    private final JComboBox<Escolha<String>> status = bloquear(new JComboBox<>());
    private final JComboBox<Intervalo> intervalo = bloquear(new JComboBox<>(Intervalo.values()));
    private final JPanel indicadores = new JPanel(new GridLayout(2, 4, 11, 10));
    private final JPanel graficos = new JPanel(new GridLayout(0, 2, 11, 11));
    private final JPanel medidas = new JPanel(new GridLayout(0, 4, 8, 4));
    private final JLabel escopo = Theme.muted("Dados do MySQL");


    private record Painel(Dados dados, Filtro filtro, List<Escolha<String>> barbeiros, List<Escolha<Integer>> servicos,
                          Estatistica.Medidas medidas, SerieTemporal.Serie serie, OptionalDouble correlacao,
                          Map<String, Integer> histograma) {}

    DashboardPanel(Consumer<Banco> banco) {
        super(banco);
        Form.opcoes(barbeiros, List.of(), "Todos", null);
        Form.opcoes(servicos, List.of(), "Todos", null);
        Form.opcoes(status, Agendamento.STATUS.stream().map(s -> new Escolha<>(s, Form.status(s))).toList(), "Todos", null);
        intervalo.setSelectedItem(Intervalo.SEMANA);

        JPanel linhaFiltros = new JPanel(new GridLayout(1, 6, 9, 0));
        linhaFiltros.setOpaque(false);
        linhaFiltros.add(Theme.campo("De", inicio));
        linhaFiltros.add(Theme.campo("Até", fim));
        linhaFiltros.add(Theme.campo("Barbeiro", barbeiros));
        linhaFiltros.add(Theme.campo("Serviço", servicos));
        linhaFiltros.add(Theme.campo("Status da agenda", status));
        linhaFiltros.add(Theme.campo("Agrupar por", intervalo));

        JPanel acoes = new JPanel(new BorderLayout());
        acoes.setOpaque(false);
        var botoes = Theme.row(6);
        botoes.add(bloquear(Theme.button("Aplicar filtros", true, this::atualizar)));
        botoes.add(bloquear(Theme.button("Limpar", false, this::limpar)));
        botoes.add(bloquear(Theme.button("Atualizar", false, this::atualizar)));
        acoes.add(escopo, BorderLayout.CENTER);
        acoes.add(botoes, BorderLayout.EAST);

        JPanel filtros = Theme.card(new BorderLayout(0, 8));
        filtros.add(linhaFiltros, BorderLayout.NORTH);
        filtros.add(acoes, BorderLayout.SOUTH);
        add(filtros, BorderLayout.NORTH);

        Relatorio relatorio = new Relatorio();
        relatorio.setOpaque(false);
        relatorio.setLayout(new BoxLayout(relatorio, BoxLayout.Y_AXIS));
        relatorio.setBorder(new EmptyBorder(0, 0, 4, 6));
        indicadores.setOpaque(false);
        graficos.setOpaque(false);
        medidas.setOpaque(false);
        relatorio.add(indicadores);
        relatorio.add(Box.createVerticalStrut(14));
        relatorio.add(graficos);
        relatorio.add(Box.createVerticalStrut(14));
        var analise = Theme.card(new BorderLayout(0, 10));
        var titulo = new JPanel(new GridLayout(0, 1, 0, 4));
        titulo.setOpaque(false);
        titulo.add(Theme.label("Estatística dos atendimentos", 17, Font.BOLD, Theme.TEXT));
        titulo.add(Theme.muted("Valores por atendimento finalizado · variância e desvio padrão populacionais."));
        analise.add(titulo, BorderLayout.NORTH);
        analise.add(medidas);
        relatorio.add(analise);
        add(Theme.scroll(relatorio), BorderLayout.CENTER);
        exibirIndicadores(null);
    }

    private void limpar() {
        inicio.setText("");
        fim.setText("");
        barbeiros.setSelectedIndex(0);
        servicos.setSelectedIndex(0);
        status.setSelectedIndex(0);
        intervalo.setSelectedItem(Intervalo.SEMANA);
        atualizar();
    }

    @Override
    void atualizar() {
        validar(() -> {
            var filtro = new Filtro(Form.periodo(inicio, fim), Form.id(barbeiros), Form.id(servicos),
                    Form.id(status), (Intervalo) intervalo.getSelectedItem());
            executar("Calculando indicadores no MySQL…", () -> carregar(filtro), this::exibir);
        });
    }

    private Painel carregar(Filtro filtro) throws Exception {
        var profissionais = new ProfissionalDAO().barbeiros();
        var catalogo = new ServicoDAO().listar().stream()
                .map(s -> new Escolha<>(s.getIdServico(), s.getNome())).toList();
        Dados dados = dao.consultar(filtro);
        var valores = dados.visitas().stream().map(Visita::total).toList();
        return new Painel(dados, filtro, profissionais, catalogo, Estatistica.calcular(valores),
                SerieTemporal.calcular(dados.visitas(), filtro), Estatistica.pearson(dados.visitas()),
                Graficos.histograma(valores));
    }

    private <T> List<Escolha<T>> incluindoFiltro(List<Escolha<T>> opcoes, T id) {
        if (id == null || opcoes.stream().anyMatch(o -> id.equals(o.id()))) {
            return opcoes;
        }
        var todas = new ArrayList<>(opcoes);
        todas.add(new Escolha<>(id, id + " · removido"));
        return todas;
    }

    private void exibir(Painel painel) {
        Dados dados = painel.dados();
        Filtro f = painel.filtro();
        Form.opcoes(barbeiros, incluindoFiltro(painel.barbeiros(), f.barbeiro()), "Todos", f.barbeiro());
        Form.opcoes(servicos, incluindoFiltro(painel.servicos(), f.servico()), "Todos", f.servico());
        String label = f.ativo() ? "Recorte filtrado" : "Toda a base";
        escopo.setText(label + " · " + (painel.serie().intervalo() == f.intervalo()
                ? painel.serie().intervalo().toString() : painel.serie().intervalo() + " (período extenso)"));
        exibirIndicadores(painel);
        exibirGraficos(painel);
        exibirMedidas(painel);

        if (dados.agenda().isEmpty() && dados.visitas().isEmpty()) {
            mensagem("Sem agendamentos ou atendimentos finalizados neste recorte.", Theme.MUTED);
        } else {
            mensagem("Dados atualizados.", Theme.GREEN);
        }
        revalidate();
        repaint();
    }

    private void exibirIndicadores(Painel p) {
        indicadores.removeAll();
        Dados d = p == null ? null : p.dados();
        boolean filtrado = p != null && p.filtro().ativo();
        String[] titulos = {
                filtrado ? "Clientes no recorte" : "Clientes cadastrados",
                filtrado ? "Barbeiros no recorte" : "Profissionais cadastrados",
                "Agendamentos", "Atendimentos finalizados",
                "Faturamento", "Ticket médio", "Serviços executados", "Agenda concluída"
        };
        String[] legendas = {"Pessoas distintas", filtrado ? "Barbeiros distintos" : "Inclui gerentes", "Todos os status do recorte",
                "Com horário de fim", "Preços praticados", "Por atendimento finalizado", "Itens dos atendimentos", "Concluídos / agendamentos"};
        String[] valores = new String[titulos.length];
        Arrays.fill(valores, "—");
        if (d != null) {
            valores[0] = Form.numero(d.clientes(), 0);
            valores[1] = Form.numero(d.profissionais(), 0);
            valores[2] = Form.numero(d.agenda().size(), 0);
            valores[3] = Form.numero(d.visitas().size(), 0);
            valores[4] = Form.moeda(p.medidas().soma());
            valores[5] = Form.moeda(p.medidas().media());
            valores[6] = Form.numero(d.visitas().stream().mapToLong(Visita::itens).sum(), 0);
            long concluidos = d.agenda().stream().filter(a -> Agendamento.CONCLUIDO.equals(a.status())).count();
            if (!d.agenda().isEmpty()) {
                valores[7] = Form.numero(100.0 * concluidos / d.agenda().size(), 1) + "%";
            }
        }
        for (int i = 0; i < titulos.length; i++) {
            var card = Theme.card(new BorderLayout(0, 5));
            card.setBorder(new EmptyBorder(10, 14, 10, 14));
            card.add(Theme.muted(titulos[i]), BorderLayout.NORTH);
            float tamanho = valores[i].length() > 16 ? 19 : valores[i].length() > 12 ? 24 : 29;
            var principal = Theme.label(valores[i], tamanho, Font.BOLD, i == 4 || i == 5 ? Theme.GOLD : Theme.TEXT);
            principal.setToolTipText(valores[i]);
            card.add(principal, BorderLayout.CENTER);
            card.add(Theme.label(legendas[i], 11, Font.PLAIN, Theme.MUTED), BorderLayout.SOUTH);
            indicadores.add(card);
        }
    }

    private void exibirGraficos(Painel p) {
        graficos.removeAll();
        var d = p.dados();
        boolean temVisitas = !d.visitas().isEmpty();
        boolean temServicos = !d.servicos().isEmpty();
        String r = p.correlacao().isPresent() ? "Pearson r = " + Form.numero(p.correlacao().getAsDouble(), 2)
                : "Pearson indisponível com menos de 3 observações ou sem variação";
        graficos.add(grafico("Faturamento", "Soma por " + p.serie().intervalo().toString().toLowerCase(Form.BR), temVisitas ? Graficos.tempo(p.serie(), false) : null));
        graficos.add(grafico("Evolução do ticket médio", "Média por " + p.serie().intervalo().toString().toLowerCase(Form.BR), temVisitas ? Graficos.tempo(p.serie(), true) : null));

        var status = frequencias(d.agenda(), a -> Form.status(a.status()));
        var cpfsPorNome = d.agenda().stream().collect(Collectors.groupingBy(Agenda::nomeBarbeiro,
                Collectors.mapping(Agenda::barbeiro, Collectors.toSet())));
        var porBarbeiro = frequencias(d.agenda(), a -> cpfsPorNome.get(a.nomeBarbeiro()).size() > 1
                ? a.nomeBarbeiro() + " · " + a.barbeiro() : a.nomeBarbeiro());
        graficos.add(grafico("Agendamentos por status", "Distribuição dos registros da agenda",
                status.isEmpty() ? null : Graficos.rosca(status)));
        graficos.add(grafico("Agenda por barbeiro", "Até 8 barbeiros por quantidade de agendamentos",
                porBarbeiro.isEmpty() ? null : Graficos.barras(top(porBarbeiro), true, false, Graficos.SALVIA)));

        Map<String, Long> execucoes = d.servicos().stream().collect(Collectors.toMap(ServicoMedido::nome, ServicoMedido::quantidade));
        Map<String, BigDecimal> receita = d.servicos().stream().collect(Collectors.toMap(ServicoMedido::nome, ServicoMedido::receita));
        graficos.add(grafico("Serviços mais realizados", "Até 8 serviços por frequência",
                temServicos ? Graficos.barras(top(execucoes), true, false, Theme.GOLD) : null));
        graficos.add(grafico("Receita por serviço", "Até 8 serviços por faturamento",
                temServicos ? Graficos.barras(top(receita), true, true, Graficos.BRONZE) : null));

        var classes = frequencias(d.visitas(), v -> Form.classe(v.classificacao()));
        graficos.add(grafico("Distribuição de valores", "Atendimentos por faixa de valor em reais",
                temVisitas ? Graficos.barras(p.histograma(), false, false, Graficos.CINZA) : null));
        graficos.add(grafico("Classificação dos atendimentos", "Categorias de fn_classificacao_atendimento",
                temVisitas ? Graficos.rosca(classes) : null));
        graficos.add(grafico("Duração × valor", temVisitas ? r + " · n = " + d.visitas().size() : "Um ponto por atendimento",
                temVisitas ? Graficos.dispersao(d.visitas()) : null));
    }

    private <T> Map<String, Long> frequencias(List<T> itens, Function<T, String> chave) {
        return itens.stream().collect(Collectors.groupingBy(chave, TreeMap::new, Collectors.counting()));
    }

    private <T extends Number & Comparable<? super T>> Map<String, T> top(Map<String, T> freq) {
        Map<String, T> resultado = new LinkedHashMap<>();
        freq.entrySet().stream().sorted(Map.Entry.<String, T>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey())).limit(8)
                .forEach(e -> resultado.put(e.getKey(), e.getValue()));
        return resultado;
    }

    private JPanel grafico(String titulo, String legenda, JComponent chart) {
        JPanel card = Theme.card(new BorderLayout(0, 7));
        var cabecalho = new JPanel(new GridLayout(0, 1, 0, 3));
        cabecalho.setOpaque(false);
        cabecalho.add(Theme.label(titulo, 17, Font.BOLD, Theme.TEXT));
        var subtitulo = Theme.muted(legenda);
        subtitulo.setToolTipText(legenda);
        cabecalho.add(subtitulo);
        card.add(cabecalho, BorderLayout.NORTH);
        if (chart == null) {
            JLabel mensagem = Theme.muted("Sem dados neste recorte");
            mensagem.setHorizontalAlignment(SwingConstants.CENTER);
            mensagem.setPreferredSize(new Dimension(100, 191));
            chart = mensagem;
        }
        card.add(chart);
        return card;
    }

    private void exibirMedidas(Painel p) {
        medidas.removeAll();
        var m = p.medidas();
        String moda;
        if (m.n() == 0) {
            moda = "—";
        } else if (m.modas().isEmpty()) {
            moda = "Sem moda";
        } else if (m.modas().size() == 1) {
            moda = Form.moeda(m.modas().getFirst());
        } else {
            moda = m.modas().size() + " modas empatadas";
        }
        String empate = m.modas().size() > 1 ? m.modas().stream().limit(20).map(Form::moeda).collect(Collectors.joining(", "))
                + (m.modas().size() > 20 ? " …" : "") : moda;
        medida("Observações", Form.numero(m.n(), 0), null);
        medida("Média", Form.moeda(m.media()), null);
        medida("Mediana", Form.moeda(m.mediana()), null);
        medida("Moda", moda, empate);
        medida("Mínimo", Form.moeda(m.minimo()), null);
        medida("Máximo", Form.moeda(m.maximo()), null);
        medida("Variância (R$²)", m.variancia() == null ? "—" : Form.numero(m.variancia(), 2), null);
        medida("Desvio padrão", Form.moeda(m.desvio()), null);
    }

    private void medida(String nome, String valor, String dica) {
        JPanel medida = new JPanel(new GridLayout(0, 1, 0, 3));
        medida.setBorder(new EmptyBorder(5, 0, 5, 0));
        medida.setOpaque(false);
        medida.add(Theme.muted(nome));
        JLabel label = Theme.label(valor, 17, Font.BOLD, Theme.TEXT);
        label.setToolTipText(dica == null ? valor : dica);
        medida.add(label);
        medidas.add(medida);
    }

    private static final class Relatorio extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle area, int orientation, int direction) { return 18; }
        @Override public int getScrollableBlockIncrement(Rectangle area, int orientation, int direction) { return Math.max(36, area.height - 36); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
