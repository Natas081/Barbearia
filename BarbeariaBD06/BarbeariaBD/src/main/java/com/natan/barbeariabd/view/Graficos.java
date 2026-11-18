package com.natan.barbeariabd.view;

import com.natan.barbeariabd.model.Dashboard.Intervalo;
import com.natan.barbeariabd.model.Dashboard.Visita;
import com.natan.barbeariabd.service.SerieTemporal.Serie;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.labels.StandardPieToolTipGenerator;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.RingPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

final class Graficos {
    static final Color BRONZE = Color.decode("#C49270");
    static final Color SALVIA = Color.decode("#90B1A4");
    static final Color CINZA = Color.decode("#7C939D");
    private static final Color[] CORES = {Theme.GOLD, SALVIA, BRONZE, CINZA};
    private static final Font LEGENDA = new Font("Segoe UI", Font.PLAIN, 11);
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private Graficos() {}

    static ChartPanel barras(Map<String, ? extends Number> valores, boolean horizontal, boolean dinheiro, Color cor) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        valores.forEach((rotulo, valor) -> ds.addValue(valor, "Valor", rotulo));
        var chart = ChartFactory.createBarChart(null, null, dinheiro ? "Reais" : null, ds,
                horizontal ? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL, false, true, false);
        var plot = chart.getCategoryPlot();
        var renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        renderer.setMaximumBarWidth(.14);
        renderer.setSeriesPaint(0, cor);
        renderer.setDefaultToolTipGenerator(new StandardCategoryToolTipGenerator("{1} · {2}", formato(dinheiro)));
        plot.getDomainAxis().setMaximumCategoryLabelWidthRatio(horizontal ? .39f : .85f);
        if (!horizontal && valores.size() > 3) {
            plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        }
        numeros(plot.getRangeAxis(), dinheiro);
        return painel(chart);
    }

    static ChartPanel rosca(Map<String, Long> valores) {
        var ds = new DefaultPieDataset<String>();
        valores.forEach(ds::setValue);
        var chart = ChartFactory.createRingChart(null, ds, true, true, false);
        RingPlot plot = (RingPlot) chart.getPlot();
        plot.setSectionDepth(.34);
        plot.setLabelGenerator(null);
        plot.setShadowPaint(null);
        plot.setSeparatorsVisible(false);
        plot.setSectionOutlinesVisible(false);
        plot.setToolTipGenerator(new StandardPieToolTipGenerator("{0} · {1} ({2})",
                NumberFormat.getIntegerInstance(Form.BR), NumberFormat.getPercentInstance(Form.BR)));
        int i = 0;
        for (String nome : valores.keySet()) {
            Color cor = switch (nome) {
                case "Agendado", "Básico" -> Theme.GOLD;
                case "Confirmado", "Intermediário" -> CINZA;
                case "Concluído", "Premium" -> SALVIA;
                case "Cancelado" -> BRONZE;
                case "Sem serviços" -> Theme.MUTED;
                default -> CORES[i % CORES.length];
            };
            plot.setSectionPaint(nome, cor);
            i++;
        }
        chart.getLegend().setBackgroundPaint(Theme.CARD);
        chart.getLegend().setItemPaint(Theme.TEXT);
        chart.getLegend().setItemFont(LEGENDA);
        chart.getLegend().setFrame(org.jfree.chart.block.BlockBorder.NONE);
        return painel(chart);
    }

    static ChartPanel tempo(Serie serie, boolean ticket) {
        var ds = new TimeSeries(ticket ? "Ticket médio" : "Faturamento");
        serie.pontos().forEach((data, ponto) -> ds.add(new Day(data.getDayOfMonth(), data.getMonthValue(), data.getYear()),
                ticket ? ponto.ticket() : ponto.receita()));
        var conjunto = new TimeSeriesCollection(ds, UTC);
        var chart = ChartFactory.createTimeSeriesChart(null, null, "Reais", conjunto, false, true, false);
        var plot = chart.getXYPlot();
        var eixo = (DateAxis) plot.getDomainAxis();
        eixo.setTimeZone(UTC);
        var rotulo = new SimpleDateFormat(serie.intervalo() == Intervalo.MES ? "MMM/yy" : "dd/MM", Form.BR);
        rotulo.setTimeZone(UTC);
        eixo.setDateFormatOverride(rotulo);
        var tooltip = new SimpleDateFormat(serie.intervalo() == Intervalo.MES ? "MMMM yyyy" : "dd/MM/yyyy", Form.BR);
        tooltip.setTimeZone(UTC);
        var renderer = new XYLineAndShapeRenderer(true, serie.pontos().size() <= 48 || ticket);
        if (ticket && serie.pontos().size() > 48) {
            renderer.setSeriesShape(0, new Ellipse2D.Double(-2, -2, 4, 4));
        } else {
            renderer.setSeriesShape(0, new Ellipse2D.Double(-3, -3, 6, 6));
        }
        renderer.setSeriesPaint(0, ticket ? SALVIA : Theme.GOLD);
        renderer.setSeriesStroke(0, new BasicStroke(2.3f));
        renderer.setDefaultToolTipGenerator(new StandardXYToolTipGenerator("{1} · {2}", tooltip, formato(true)));
        plot.setRenderer(renderer);
        numeros(plot.getRangeAxis(), true);
        ((NumberAxis) plot.getRangeAxis()).setAutoRangeIncludesZero(!ticket);
        return painel(chart);
    }

    static ChartPanel dispersao(List<Visita> visitas) {
        var pontos = new XYSeries("Atendimento");
        visitas.forEach(v -> pontos.add(v.segundos() / 60.0, v.total().doubleValue()));
        var chart = ChartFactory.createScatterPlot(null, "Duração (min)", "Valor (R$)",
                new XYSeriesCollection(pontos), PlotOrientation.VERTICAL, false, true, false);
        var plot = chart.getXYPlot();
        plot.getRenderer().setSeriesPaint(0, SALVIA);
        plot.getRenderer().setSeriesShape(0, new Ellipse2D.Double(-4, -4, 8, 8));
        plot.getRenderer().setDefaultToolTipGenerator(
                new StandardXYToolTipGenerator("{1} min · {2}", NumberFormat.getNumberInstance(Form.BR), formato(true)));
        plot.setForegroundAlpha(.78f);
        numeros(plot.getRangeAxis(), true);
        return painel(chart);
    }

    static Map<String, Integer> histograma(List<BigDecimal> valores) {
        Map<String, Integer> resultado = new java.util.LinkedHashMap<>();
        if (valores.isEmpty()) {
            return resultado;
        }
        BigDecimal min = java.util.Collections.min(valores);
        BigDecimal max = java.util.Collections.max(valores);
        if (min.compareTo(max) == 0) {
            resultado.put(Form.numero(min, 2), valores.size());
            return resultado;
        }
        int faixas = Math.clamp((int) Math.ceil(Math.sqrt(valores.size())), 2, 8);
        BigDecimal largura = max.subtract(min).add(new BigDecimal("0.01"))
                .divide(BigDecimal.valueOf(faixas), 2, RoundingMode.CEILING);
        int[] contagem = new int[faixas];
        for (BigDecimal valor : valores) {
            int i = valor.subtract(min).divideToIntegralValue(largura).intValueExact();
            contagem[i]++;
        }
        for (int i = 0; i < faixas; i++) {
            BigDecimal a = min.add(largura.multiply(BigDecimal.valueOf(i)));
            BigDecimal b = a.add(largura).subtract(new BigDecimal("0.01"));
            resultado.put(Form.numero(a, 2) + "–" + Form.numero(b, 2), contagem[i]);
        }
        return resultado;
    }

    private static NumberFormat formato(boolean dinheiro) {
        return dinheiro ? NumberFormat.getCurrencyInstance(Form.BR) : NumberFormat.getIntegerInstance(Form.BR);
    }

    private static void numeros(ValueAxis axis, boolean dinheiro) {
        NumberAxis n = (NumberAxis) axis;
        n.setNumberFormatOverride(dinheiro ? NumberFormat.getNumberInstance(Form.BR) : formato(false));
        if (!dinheiro) {
            n.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        }
    }

    private static void eixo(org.jfree.chart.axis.Axis eixo) {
        eixo.setLabelPaint(Theme.MUTED);
        eixo.setLabelFont(LEGENDA);
        eixo.setTickLabelPaint(Theme.MUTED);
        eixo.setTickLabelFont(LEGENDA);
        eixo.setAxisLinePaint(Theme.BORDER);
        eixo.setTickMarkPaint(Theme.BORDER);
    }

    private static ChartPanel painel(JFreeChart chart) {
        chart.setBackgroundPaint(Theme.CARD);
        chart.setBorderVisible(false);
        Plot plot = chart.getPlot();
        plot.setBackgroundPaint(Theme.CARD);
        plot.setOutlineVisible(false);
        if (plot instanceof CategoryPlot p) {
            p.setRangeGridlinePaint(Theme.BORDER);
            eixo(p.getDomainAxis());
            eixo(p.getRangeAxis());
        }
        if (plot instanceof XYPlot p) {
            p.setDomainGridlinePaint(Theme.BORDER);
            p.setRangeGridlinePaint(Theme.BORDER);
            eixo(p.getDomainAxis());
            eixo(p.getRangeAxis());
        }
        ChartPanel painel = new ChartPanel(chart, false);
        painel.setMinimumDrawWidth(0);
        painel.setMinimumDrawHeight(0);
        painel.setMaximumDrawWidth(3840);
        painel.setMaximumDrawHeight(2160);
        painel.setPreferredSize(new Dimension(250, 191));
        painel.setBackground(Theme.CARD);
        painel.setFillZoomRectangle(true);
        return painel;
    }
}
