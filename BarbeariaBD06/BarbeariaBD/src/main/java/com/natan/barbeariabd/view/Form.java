package com.natan.barbeariabd.view;

import com.natan.barbeariabd.model.Escolha;
import com.natan.barbeariabd.model.Periodo;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

final class Form {
    static final Locale BR = Locale.forLanguageTag("pt-BR");
    static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);
    static final DateTimeFormatter DATA_HORA_SEGUNDOS = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss")
            .withResolverStyle(ResolverStyle.STRICT);

    private Form() {}

    static String moeda(BigDecimal valor) {
        return valor == null ? "—" : NumberFormat.getCurrencyInstance(BR).format(valor);
    }

    static String numero(Number valor, int decimais) {
        NumberFormat format = NumberFormat.getNumberInstance(BR);
        format.setMinimumFractionDigits(decimais);
        format.setMaximumFractionDigits(decimais);
        return format.format(valor);
    }

    static JTextField campo(String texto, int largura) {
        return new JTextField(texto == null ? "" : texto, largura);
    }

    static JTextField data() {
        var field = campo("", 10);
        field.putClientProperty("JTextField.placeholderText", "dd/mm/aaaa");
        field.setToolTipText("dd/mm/aaaa");
        return field;
    }

    static JTextField horario(LocalDateTime value) {
        var field = campo(value == null ? "" : value.format(DATA_HORA_SEGUNDOS), 19);
        field.putClientProperty("JTextField.placeholderText", "dd/mm/aaaa hh:mm");
        field.setToolTipText("dd/mm/aaaa hh:mm ou dd/mm/aaaa hh:mm:ss");
        return field;
    }

    static Periodo periodo(JTextField inicio, JTextField fim) {
        return new Periodo(data(inicio, "Data inicial"), data(fim, "Data final"));
    }

    private static LocalDate data(JTextField field, String label) {
        String text = field.getText().strip();
        if (text.isEmpty()) {
            return null;
        }
        try {
            LocalDate data = LocalDate.parse(text, DATA);
            if (data.getYear() < 1000 || data.getYear() > 9998) {
                throw new IllegalArgumentException(label + ": informe um ano entre 1000 e 9998.");
            }
            return data;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(label + ": use uma data válida no formato dd/mm/aaaa.");
        }
    }

    static LocalDateTime horario(JTextField field, String label, boolean obrigatorio) {
        String text = field.getText().strip();
        if (text.isEmpty() && !obrigatorio) {
            return null;
        }
        try {
            return LocalDateTime.parse(text, text.length() == 19 ? DATA_HORA_SEGUNDOS : DATA_HORA);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(label + ": use dd/mm/aaaa hh:mm.");
        }
    }

    static String texto(JTextField field, String label, int limite, boolean obrigatorio) {
        String text = field.getText().strip();
        if ((obrigatorio && text.isEmpty()) || text.codePointCount(0, text.length()) > limite) {
            throw new IllegalArgumentException(label + ": " + (obrigatorio ? "informe de 1 a " : "use até ")
                    + limite + " caracteres.");
        }
        return text.isEmpty() ? null : text;
    }

    static String cpf(JTextField field) {
        String cpf = field.getText().strip().replaceAll("[.\\-]", "");
        if (!cpf.matches("[0-9]{11}")) {
            throw new IllegalArgumentException("Informe um CPF com 11 dígitos.");
        }
        return cpf;
    }

    static int inteiro(JTextField field, String label) {
        try {
            int value = Integer.parseInt(field.getText().strip());
            if (value > 0) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // A mensagem abaixo vale tanto para formato inválido quanto para valor fora do intervalo.
        }
        throw new IllegalArgumentException(label + ": informe um número inteiro positivo.");
    }

    static BigDecimal dinheiro(JTextField field, String label, int inteiros, boolean positivo) {
        String text = field.getText().strip();
        if (text.matches("[0-9]{1," + inteiros + "}([,.][0-9]{1,2})?")) {
            BigDecimal value = new BigDecimal(text.replace(',', '.'));
            if (!positivo || value.signum() > 0) {
                return value;
            }
        }
        throw new IllegalArgumentException(label + ": informe um valor " + (positivo ? "positivo" : "não negativo")
                + " com até " + inteiros + " dígitos inteiros e duas casas decimais.");
    }

    static <T> void opcoes(JComboBox<Escolha<T>> box, List<Escolha<T>> valores, String todos, T definido) {
        var model = new DefaultComboBoxModel<Escolha<T>>();
        if (todos != null) {
            model.addElement(new Escolha<>(null, todos));
        }
        valores.forEach(model::addElement);
        box.setModel(model);
        box.setMaximumRowCount(15);
        for (int i = 0; i < model.getSize(); i++) {
            if (Objects.equals(model.getElementAt(i).id(), definido)) {
                box.setSelectedIndex(i);
                break;
            }
        }
    }

    static <T> T id(JComboBox<Escolha<T>> box) {
        int i = box.getSelectedIndex();
        return i < 0 ? null : box.getItemAt(i).id();
    }

    static <T> T idObrigatorio(JComboBox<Escolha<T>> box, String label) {
        T id = id(box);
        if (id == null) {
            throw new IllegalArgumentException("Selecione " + label + ".");
        }
        return id;
    }

    static boolean confirmar(Component parent, String titulo, String texto) {
        return JOptionPane.showConfirmDialog(parent, texto, titulo, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
    }

    static <T> T editar(Component parent, String titulo, Supplier<T> ler, JComponent... componentes) {
        var panel = new JPanel(new GridLayout(0, 1, 0, 8));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        for (JComponent c : componentes) {
            panel.add(c);
        }
        while (JOptionPane.showConfirmDialog(parent, panel, titulo, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                return ler.get();
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(parent, e.getMessage(), "Confira os dados", JOptionPane.WARNING_MESSAGE);
            }
        }
        return null;
    }

    static String status(String status) {
        return "Concluido".equals(status) ? "Concluído" : status;
    }

    static String classe(String classe) {
        return switch (classe) {
            case "Sem servicos" -> "Sem serviços";
            case "Basico" -> "Básico";
            case "Intermediario" -> "Intermediário";
            default -> classe;
        };
    }
}
