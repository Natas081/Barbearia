package com.natan.barbeariabd.view;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract class Page extends JPanel {
    enum Banco { CONECTADO, INDISPONIVEL }

    private final Consumer<Banco> banco;
    private final JLabel feedback = Theme.muted(" ");
    private final List<JComponent> bloqueados = new ArrayList<>();
    private boolean busy;

    Page(Consumer<Banco> banco) {
        super(new BorderLayout(0, 10));
        this.banco = banco;
        setOpaque(false);
        feedback.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        add(feedback, BorderLayout.SOUTH);
    }

    abstract void atualizar();

    final boolean ocupado() {
        return busy;
    }

    final <T extends JComponent> T bloquear(T c) {
        bloqueados.add(c);
        return c;
    }

    final void mensagem(String texto, Color cor) {
        feedback.setForeground(cor);
        feedback.setText(texto);
    }

    final void validar(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException error) {
            mensagem(error.getMessage(), Theme.RED);
        }
    }

    final <T> void executar(String carregando, Callable<T> tarefa, Consumer<T> aplicar) {
        if (busy) {
            return;
        }
        busy = true;
        var estados = bloqueados.stream().map(JComponent::isEnabled).toList();
        bloqueados.forEach(c -> c.setEnabled(false));
        mensagem(carregando, Theme.MUTED);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return tarefa.call();
            }

            @Override
            protected void done() {
                busy = false;
                for (int i = 0; i < estados.size(); i++) {
                    bloqueados.get(i).setEnabled(estados.get(i));
                }
                try {
                    T dados = get();
                    banco.accept(Banco.CONECTADO);
                    mensagem(" ", Theme.MUTED);
                    aplicar.accept(dados);
                } catch (ExecutionException e) {
                    falha(e.getCause());
                } catch (RuntimeException e) {
                    falha(e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mensagem("Operação interrompida.", Theme.RED);
                }
            }
        }.execute();
    }

    private void falha(Throwable causa) {
        if (causa instanceof SQLException sql) {
            String estado = java.util.Objects.toString(sql.getSQLState(), "");
            if (estado.startsWith("08") || estado.startsWith("28") || estado.equals("BDCFG")
                    || sql.getErrorCode() == 1049) {
                banco.accept(Banco.INDISPONIVEL);
            } else {
                banco.accept(Banco.CONECTADO);
            }
            String texto = switch (sql.getErrorCode()) {
                case 1044, 1045, 1142, 1370 -> "Acesso negado pelo MySQL. Confira usuário, senha e permissões.";
                case 1049, 1054, 1146, 1305 -> "Estrutura do banco incompleta. Confira os scripts SQL no README.";
                case 1062 -> "Já existe um registro com essa chave (CPF, nome ou vínculo).";
                case 1213, 1205 -> "Outro usuário está atualizando estes dados. Atualize a lista e tente novamente.";
                case 1451 -> "O registro possui vínculos no banco e não pode ser excluído ou desvinculado.";
                case 1452 -> "Um registro relacionado foi removido. Atualize a lista e selecione novamente.";
                case 3819 -> "Um dos valores viola as regras do banco. Confira os dados informados.";
                default -> {
                    if ("45000".equals(estado)) {
                        yield "Conclusao exige atendimento finalizado com servico registrado".equals(sql.getMessage())
                                ? "Conclusão bloqueada pelo MySQL. Finalize o atendimento com pelo menos um serviço."
                                : sql.getMessage();
                    }
                    if ("BDCFG".equals(estado)) {
                        yield sql.getMessage();
                    }
                    if (estado.startsWith("08")) {
                        yield "MySQL indisponível. Confira a conexão e tente atualizar.";
                    }
                    yield "O MySQL recusou a operação (código " + sql.getErrorCode() + ").";
                }
            };
            mensagem(texto, Theme.RED);
            return;
        }
        Logger.getLogger(Page.class.getName()).log(Level.WARNING, "Falha inesperada na interface", causa);
        mensagem("Não foi possível completar a operação. Tente novamente.", Theme.RED);
    }
}
