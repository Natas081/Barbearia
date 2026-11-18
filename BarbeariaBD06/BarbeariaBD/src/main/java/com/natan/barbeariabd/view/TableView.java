package com.natan.barbeariabd.view;

import com.natan.barbeariabd.model.Resultado;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

final class TableView<R> extends JPanel {
    record Col<R>(String nome, Class<?> tipo, Function<R, ?> valor) {}

    private final JTable table = new JTable() {
        @Override
        public boolean getScrollableTracksViewportWidth() {
            if (getParent() instanceof JViewport viewport) {
                int min = 0;
                for (int i = 0; i < getColumnModel().getColumnCount(); i++) {
                    min += getColumnModel().getColumn(i).getMinWidth();
                }
                return min <= viewport.getWidth();
            }
            return super.getScrollableTracksViewportWidth();
        }
    };
    private final Model model;
    private final TableRowSorter<Model> sorter;
    private final JLabel estado = Theme.muted(" ");
    private List<R> rows = List.of();
    private List<Col<R>> colunas;
    private String vazio = "Nenhum registro encontrado.";

    @SafeVarargs
    TableView(Col<R>... colunas) {
        super(new BorderLayout(0, 8));
        this.colunas = List.of(colunas);
        model = new Model();
        sorter = new TableRowSorter<>(model);
        setOpaque(false);
        table.setModel(model);
        table.setRowSorter(sorter);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setReorderingAllowed(false);
        if (table.getTableHeader().getDefaultRenderer() instanceof JLabel header) {
            header.setHorizontalAlignment(SwingConstants.LEADING);
        }
        ajustarColunas();
        table.setDefaultRenderer(Object.class, new Renderer());
        table.setDefaultRenderer(Number.class, new Renderer());
        table.setDefaultRenderer(BigDecimal.class, new Renderer());
        table.setDefaultRenderer(LocalDateTime.class, new Renderer());
        sorter.addRowSorterListener(e -> contar());
        var scroll = Theme.scroll(table);
        scroll.getViewport().setBackground(Theme.CARD);
        scroll.setPreferredSize(new Dimension(540, 280));
        add(scroll, BorderLayout.CENTER);
        add(estado, BorderLayout.SOUTH);
        contar();
    }

    void dados(List<R> values) {
        rows = values;
        table.clearSelection();
        model.fireTableDataChanged();
        contar();
    }

    void vazio(String texto) {
        vazio = texto;
        contar();
    }

    static void resultado(TableView<Object[]> view, Resultado resultado) {
        view.colunas = java.util.stream.IntStream.range(0, resultado.colunas().size())
                .mapToObj(i -> new Col<Object[]>(resultado.colunas().get(i), resultado.tipos().get(i), r -> r[i]))
                .toList();
        view.rows = resultado.linhas();
        view.table.clearSelection();
        view.model.fireTableStructureChanged();
        view.ajustarColunas();
        view.contar();
    }

    private void ajustarColunas() {
        for (int i = 0; i < colunas.size(); i++) {
            Col<R> coluna = colunas.get(i);
            var c = table.getColumnModel().getColumn(i);
            int largura = Math.max(switch (coluna.tipo().getSimpleName()) {
                case "LocalDateTime" -> 138;
                case "BigDecimal" -> 108;
                case "Integer", "Long" -> 78;
                default -> 104;
            }, table.getFontMetrics(table.getFont()).stringWidth(coluna.nome()) + 25);
            c.setMinWidth(largura);
            c.setPreferredWidth(coluna.tipo() == String.class ? largura + 45 : largura);
        }
    }

    JTable table() {
        return table;
    }

    R selecionado() {
        int i = table.getSelectedRow();
        return i < 0 ? null : rows.get(table.convertRowIndexToModel(i));
    }

    void selecionar(java.util.function.Predicate<R> match) {
        for (int i = 0; i < rows.size(); i++) {
            if (match.test(rows.get(i))) {
                int visible = table.convertRowIndexToView(i);
                if (visible >= 0) {
                    table.setRowSelectionInterval(visible, visible);
                    table.scrollRectToVisible(table.getCellRect(visible, 0, true));
                }
                return;
            }
        }
    }

    void buscar(String texto) {
        String filtro = normalizar(texto.strip());
        sorter.setRowFilter(filtro.isEmpty() ? null : new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends Model, ? extends Integer> entry) {
                for (int i = 0; i < entry.getValueCount(); i++) {
                    if (normalizar(formatar(entry.getValue(i))).contains(filtro)) {
                        return true;
                    }
                }
                return false;
            }
        });
        contar();
    }

    void filtrar(java.util.function.Predicate<R> match) {
        sorter.setRowFilter(match == null ? null : new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends Model, ? extends Integer> entry) {
                return match.test(rows.get(entry.getIdentifier()));
            }
        });
        contar();
    }

    private void contar() {
        int n = table.getRowCount();
        estado.setText(n == 0 ? vazio : Form.numero(n, 0) + (n == 1 ? " registro" : " registros"));
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private static String formatar(Object valor) {
        return switch (valor) {
            case null -> "—";
            case BigDecimal v -> Form.moeda(v);
            case LocalDateTime v -> v.format(Form.DATA_HORA);
            case Number v -> Form.numero(v, 0);
            default -> Form.status(valor.toString());
        };
    }

    private final class Model extends AbstractTableModel {
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return colunas.size(); }
        @Override public String getColumnName(int col) { return colunas.get(col).nome(); }
        @Override public Class<?> getColumnClass(int col) { return colunas.get(col).tipo(); }
        @Override public Object getValueAt(int row, int col) { return colunas.get(col).valor().apply(rows.get(row)); }
    }

    private static final class Renderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, formatar(value), selected, focus, row, column);
            setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 9));
            setHorizontalAlignment(value instanceof Number ? RIGHT : LEFT);
            setToolTipText(value == null ? null :
                    value instanceof LocalDateTime data ? data.format(Form.DATA_HORA_SEGUNDOS) : formatar(value));
            if (!selected) {
                setForeground(switch (String.valueOf(value)) {
                    case "Concluido", "Confirmado" -> Theme.GREEN;
                    case "Cancelado" -> Theme.RED;
                    case "Agendado", "Premium" -> Theme.GOLD;
                    case "null" -> Theme.MUTED;
                    default -> Theme.TEXT;
                });
            }
            return this;
        }
    }
}
