package com.natan.barbeariabd.view;

import com.natan.barbeariabd.database.Jdbc;
import com.natan.barbeariabd.model.Dashboard.Intervalo;
import com.natan.barbeariabd.model.Escolha;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.RingPlot;
import org.jfree.chart.plot.XYPlot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@EnabledIfEnvironmentVariable(named = "BARBEARIA_RUN_IT", matches = "true")
class InterfaceTest {
    @Test
    void navegaEAtualizaOsNoveGraficosComRecortesReais() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "O teste visual requer ambiente gráfico.");
        assertEquals("barbearia_etapa06_teste", Jdbc.list("SELECT DATABASE()", r -> r.getString(1)).getFirst());
        MainFrame frame = edt(() -> {
            Theme.install();
            MainFrame janela = new MainFrame();
            janela.setSize(1380, 880);
            return janela;
        });
        try {
            DashboardPanel painel = edt(() -> filhos(frame, DashboardPanel.class).getFirst());
            esperar(painel);
            edt(() -> {
                var charts = filhos(painel, ChartPanel.class);
                assertEquals(9, charts.size());
                assertEquals(30, xy(charts.get(8)).getDataset().getItemCount(0));
                assertTrue(filhos(painel, JLabel.class).stream().anyMatch(l -> l.getText().equals(Form.moeda(new java.math.BigDecimal("1120")))));
                capturar(frame, "dashboard");
                filtros(painel, "01/09/2026", "03/09/2026", null, null, null);
                return null;
            });
            esperar(painel);
            edt(() -> {
                var charts = filhos(painel, ChartPanel.class);
                assertEquals(9, charts.size());
                assertEquals(3, xy(charts.get(0)).getDataset().getItemCount(0));
                assertEquals(24, xy(charts.get(0)).getDataset().getYValue(0, 0), 0.01);
                assertEquals(24, xy(charts.get(1)).getDataset().getYValue(0, 2), 0.01);
                var porStatus = ((RingPlot) charts.get(2).getChart().getPlot()).getDataset();
                assertEquals(3, porStatus.getItemCount());
                assertEquals(1, categoria(charts.get(3)).getColumnCount());
                assertEquals(3, categoria(charts.get(4)).getValue(0, 0).intValue());
                assertEquals(72, categoria(charts.get(5)).getValue(0, 0).intValue());
                assertEquals(3, categoria(charts.get(6)).getValue(0, 0).intValue());
                var porClasse = ((RingPlot) charts.get(7).getChart().getPlot()).getDataset();
                assertEquals("Básico", porClasse.getKey(0));
                assertEquals(3, porClasse.getValue(0).intValue());
                assertEquals(3, xy(charts.get(8)).getDataset().getItemCount(0));
                filtros(painel, "01/09/2026", "03/09/2026", "30000000001", 1, "Concluido");
                return null;
            });
            esperar(painel);
            edt(() -> {
                var charts = filhos(painel, ChartPanel.class);
                assertEquals(1, xy(charts.get(8)).getDataset().getItemCount(0));
                assertEquals(24, categoria(charts.get(5)).getValue(0, 0).intValue());
                filtros(painel, "01/01/2018", "02/01/2018", null, null, null);
                return null;
            });
            esperar(painel);
            edt(() -> {
                assertTrue(filhos(painel, ChartPanel.class).isEmpty());
                assertEquals(9, filhos(painel, JLabel.class).stream().filter(l -> l.getText().equals("Sem dados neste recorte")).count());
                botao(painel, "Limpar").doClick(0);
                return null;
            });
            esperar(painel);
            assertEquals(9, edt(() -> filhos(painel, ChartPanel.class).size()));
            for (String destino : List.of("Clientes", "Profissionais", "Serviços", "Agendamentos", "Atendimentos",
                    "Consultas e views", "Operações do banco")) {
                edt(() -> {
                    JToggleButton rota = filhos(frame, JToggleButton.class).stream()
                            .filter(b -> destino.equals(b.getText())).findFirst().orElseThrow();
                    rota.doClick(0);
                    assertTrue(rota.isSelected());
                    return null;
                });
                Page atual = edt(() -> filhos(frame, Page.class).stream()
                        .filter(page -> visivelNaArvore(page, frame)).findFirst().orElseThrow());
                esperar(atual);
                edt(() -> {
                    assertTrue(filhos(atual, JLabel.class).stream().noneMatch(l -> Theme.RED.equals(l.getForeground())));
                    if ("Consultas e views".equals(destino)) {
                        capturar(frame, "consultas");
                    }
                    return null;
                });
            }
        } finally {
            edt(() -> { frame.dispose(); return null; });
        }
    }

    private void filtros(DashboardPanel painel, String inicio, String fim, String barbeiro, Integer servico, String status) {
        var campos = filhos(painel, JTextField.class);
        campos.get(0).setText(inicio);
        campos.get(1).setText(fim);
        var combos = filhos(painel, JComboBox.class);
        selecionar(combos.get(0), barbeiro);
        selecionar(combos.get(1), servico);
        selecionar(combos.get(2), status);
        combos.get(3).setSelectedItem(Intervalo.DIA);
        botao(painel, "Aplicar filtros").doClick(0);
    }

    private void selecionar(JComboBox<?> combo, Object id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i) instanceof Escolha<?> item && java.util.Objects.equals(item.id(), id)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        fail("Opção de filtro ausente: " + id);
    }

    private JButton botao(Container c, String nome) {
        return filhos(c, JButton.class).stream().filter(b -> nome.equals(b.getText())).findFirst().orElseThrow();
    }

    private XYPlot xy(ChartPanel chart) {
        return chart.getChart().getXYPlot();
    }

    private org.jfree.data.category.CategoryDataset categoria(ChartPanel chart) {
        return ((CategoryPlot) chart.getChart().getPlot()).getDataset();
    }

    private void esperar(Page p) throws Exception {
        long limite = System.nanoTime() + java.time.Duration.ofSeconds(12).toNanos();
        while (edt(p::ocupado) && System.nanoTime() < limite) {
            Thread.sleep(25);
        }
        assertFalse(edt(p::ocupado), "A tela excedeu o tempo de atualização.");
    }

    private boolean visivelNaArvore(Component c, Container raiz) {
        for (Component atual = c; atual != raiz; atual = atual.getParent()) {
            if (atual == null || !atual.isVisible()) {
                return false;
            }
        }
        return true;
    }

    private <T extends Component> List<T> filhos(Container root, Class<T> tipo) {
        List<T> resultados = new ArrayList<>();
        for (Component c : root.getComponents()) {
            if (tipo.isInstance(c)) {
                resultados.add(tipo.cast(c));
            }
            if (c instanceof Container container) {
                resultados.addAll(filhos(container, tipo));
            }
        }
        return resultados;
    }

    private void capturar(JFrame frame, String arquivo) throws Exception {
        frame.addNotify();
        frame.validate();
        var layout = frame.getContentPane();
        var imagem = new BufferedImage(layout.getWidth(), layout.getHeight(), BufferedImage.TYPE_INT_RGB);
        var graphics = imagem.createGraphics();
        try {
            layout.printAll(graphics);
        } finally {
            graphics.dispose();
        }
        ImageIO.write(imagem, "png", Path.of("target", arquivo + "-qa.png").toFile());
    }

    private <T> T edt(Callable<T> trabalho) throws Exception {
        FutureTask<T> tarefa = new FutureTask<>(trabalho);
        SwingUtilities.invokeAndWait(tarefa);
        return tarefa.get();
    }
}
