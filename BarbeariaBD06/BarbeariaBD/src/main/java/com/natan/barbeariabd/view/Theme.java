package com.natan.barbeariabd.view;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class Theme {
    static final Color BG = Color.decode("#141719");
    static final Color SIDE = Color.decode("#101214");
    static final Color CARD = Color.decode("#1D2225");
    static final Color FIELD = Color.decode("#181C1E");
    static final Color BORDER = Color.decode("#30373B");
    static final Color GOLD = Color.decode("#DCB780");
    static final Color TEXT = Color.decode("#F2EFEA");
    static final Color MUTED = Color.decode("#A9B0B3");
    static final Color GREEN = Color.decode("#9CC3AA");
    static final Color RED = Color.decode("#F0A89E");

    private Theme() {}

    public static void install() {
        FlatDarkLaf.setup();
        String font = java.util.Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames()).contains("Inter") ? "Inter" : "Segoe UI";
        UIManager.put("defaultFont", new Font(font, Font.PLAIN, 13));
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.focusColor", GOLD);
        UIManager.put("Component.borderColor", BORDER);
        UIManager.put("Panel.background", BG);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("TextField.background", FIELD);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", GOLD);
        UIManager.put("TextField.margin", new Insets(7, 10, 7, 10));
        UIManager.put("ComboBox.background", FIELD);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ComboBox.buttonBackground", FIELD);
        UIManager.put("ComboBox.buttonArrowColor", MUTED);
        UIManager.put("ComboBox.padding", new Insets(7, 8, 7, 8));
        UIManager.put("Button.background", Color.decode("#292F32"));
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("Button.margin", new Insets(8, 13, 8, 13));
        UIManager.put("ScrollPane.background", CARD);
        UIManager.put("Table.background", CARD);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.alternateRowColor", Color.decode("#202629"));
        UIManager.put("Table.selectionBackground", Color.decode("#4A3C2A"));
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("TableHeader.background", Color.decode("#262D30"));
        UIManager.put("TableHeader.foreground", MUTED);
        UIManager.put("TableHeader.height", 38);
        UIManager.put("Table.rowHeight", 37);
        UIManager.put("Table.showHorizontalLines", false);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("ToolTip.background", CARD);
        UIManager.put("ToolTip.foreground", TEXT);
    }

    static JLabel label(String text, float size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(style, size));
        label.setForeground(color);
        return label;
    }

    static JLabel muted(String text) {
        return label(text, 13, Font.PLAIN, MUTED);
    }

    static JButton button(String text, boolean primary, Runnable action) {
        JButton b = new JButton(text);
        if (primary) {
            b.setBackground(GOLD);
            b.setForeground(SIDE);
            b.setFont(b.getFont().deriveFont(Font.BOLD));
        }
        b.addActionListener(e -> action.run());
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JPanel card(LayoutManager layout) {
        JPanel card = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D surface = (Graphics2D) g.create();
                surface.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                surface.setColor(CARD);
                surface.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                surface.setColor(BORDER);
                surface.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                surface.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        return card;
    }

    static JPanel row(int gap) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING, gap, 0));
        panel.setOpaque(false);
        return panel;
    }

    static JPanel campo(String label, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        JLabel caption = muted(label);
        caption.setLabelFor(field);
        panel.add(caption, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    static JScrollPane scroll(Component contents) {
        var pane = new JScrollPane(contents);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.getVerticalScrollBar().setUnitIncrement(19);
        pane.getViewport().setBackground(BG);
        return pane;
    }
}
