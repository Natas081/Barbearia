package com.natan.barbeariabd.view;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

abstract class ListaPage<R> extends Page {
    @FunctionalInterface
    interface Gravacao {
        void executar() throws Exception;
    }

    final TableView<R> tabela;
    final JPanel acoes = Theme.row(7);
    final JPanel corpo = new JPanel(new BorderLayout(0, 10));
    final JTextField busca = Form.campo("", 19);
    private final Function<R, ?> chave;

    @SafeVarargs
    ListaPage(Consumer<Banco> banco, String placeholder, Function<R, ?> chave, TableView.Col<R>... colunas) {
        super(banco);
        this.chave = chave;
        tabela = new TableView<>(colunas);
        bloquear(tabela.table());
        bloquear(busca);
        busca.putClientProperty("JTextField.placeholderText", placeholder);
        busca.getAccessibleContext().setAccessibleName(placeholder);
        busca.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filtrar(); }
            @Override public void removeUpdate(DocumentEvent e) { filtrar(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrar(); }
            private void filtrar() { tabela.buscar(busca.getText()); }
        });

        JPanel barra = Theme.card(new BorderLayout(10, 0));
        barra.add(busca, BorderLayout.WEST);
        barra.add(acoes, BorderLayout.EAST);
        corpo.setOpaque(false);
        corpo.add(barra, BorderLayout.NORTH);
        JPanel resultados = Theme.card(new BorderLayout());
        resultados.add(tabela);
        corpo.add(resultados, BorderLayout.CENTER);
        add(corpo, BorderLayout.CENTER);
    }

    final void botao(String nome, boolean principal, Runnable acao) {
        acoes.add(bloquear(Theme.button(nome, principal, () -> validar(acao))));
    }

    final R selecionado() {
        R value = tabela.selecionado();
        if (value == null) {
            throw new IllegalArgumentException("Selecione um registro na tabela.");
        }
        return value;
    }

    abstract List<R> listar() throws Exception;

    @Override
    void atualizar() {
        carregar(null);
    }

    void carregar(String mensagem) {
        R anterior = tabela.selecionado();
        Object id = anterior == null ? null : chave.apply(anterior);
        executar("Carregando registros…", this::listar, dados -> {
            tabela.dados(dados);
            if (id != null) {
                tabela.selecionar(r -> id.equals(chave.apply(r)));
            }
            if (mensagem != null) {
                mensagem(mensagem, Theme.GREEN);
            }
        });
    }

    final void gravar(String sucesso, Gravacao tarefa) {
        executar("Salvando no MySQL…", () -> {
            tarefa.executar();
            return null;
        }, ignored -> carregar(sucesso));
    }
}
