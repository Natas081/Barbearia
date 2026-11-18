package com.natan.barbeariabd.view;

import com.natan.barbeariabd.dao.AgendamentoDAO;
import com.natan.barbeariabd.dao.ProfissionalDAO;
import com.natan.barbeariabd.model.Agendamento;
import com.natan.barbeariabd.model.Escolha;
import javax.swing.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import static com.natan.barbeariabd.view.Theme.campo;

final class AgendamentoPanel extends ListaPage<Agendamento> {
    private final AgendamentoDAO dao = new AgendamentoDAO();

    private record Opcoes(List<Escolha<String>> clientes, List<Escolha<String>> barbeiros) {}

    AgendamentoPanel(Consumer<Banco> banco, IntConsumer atendimento) {
        super(banco, "Buscar cliente, barbeiro ou status", Agendamento::id,
                new TableView.Col<>("Código", Integer.class, Agendamento::id),
                new TableView.Col<>("Horário marcado", LocalDateTime.class, Agendamento::horario),
                new TableView.Col<>("Cliente", String.class, Agendamento::cliente),
                new TableView.Col<>("Barbeiro", String.class, Agendamento::barbeiro),
                new TableView.Col<>("Status", String.class, Agendamento::status),
                new TableView.Col<>("Atendimento", Integer.class, Agendamento::atendimento));
        botao("Atualizar", false, this::atualizar);
        botao("Atendimento", false, () -> atendimento.accept(selecionado().id()));
        botao("Concluir", false, this::concluir);
        botao("Editar", false, () -> abrir(selecionado()));
        botao("Excluir", false, this::excluir);
        botao("＋ Agendar", true, () -> abrir(null));
    }

    @Override
    List<Agendamento> listar() throws Exception {
        return dao.listar();
    }

    private void abrir(Agendamento atual) {
        executar("Carregando clientes e barbeiros…",
                () -> new Opcoes(dao.clientes(), new ProfissionalDAO().barbeiros()), op -> editar(atual, op));
    }

    private void editar(Agendamento atual, Opcoes opcoes) {
        boolean novo = atual == null;
        var clientes = new JComboBox<Escolha<String>>();
        var barbeiros = new JComboBox<Escolha<String>>();
        Form.opcoes(clientes, opcoes.clientes(), "Selecione o cliente", novo ? null : atual.clienteCpf());
        Form.opcoes(barbeiros, opcoes.barbeiros(), "Selecione o barbeiro", novo ? null : atual.barbeiroCpf());
        var horario = Form.horario(novo ? null : atual.horario());
        var status = new JComboBox<Escolha<String>>();
        var situacoes = Agendamento.STATUS.stream().filter(s -> !novo || !Agendamento.CONCLUIDO.equals(s))
                .map(s -> new Escolha<>(s, Form.status(s))).toList();
        Form.opcoes(status, situacoes, null, novo ? "Agendado" : atual.status());
        var agenda = Form.editar(this, novo ? "Novo agendamento" : "Editar agendamento", () -> new Agendamento(
                        novo ? null : atual.id(), Form.idObrigatorio(clientes, "o cliente"), null,
                        Form.idObrigatorio(barbeiros, "o barbeiro"), null, Form.horario(horario, "Horário marcado", novo),
                        Form.id(status), null),
                campo("Cliente", clientes), campo("Barbeiro", barbeiros), campo("Horário marcado", horario),
                campo("Status", status));
        if (agenda != null) {
            gravar(novo ? "Agendamento criado." : "Agendamento atualizado.", () -> dao.salvar(agenda));
        }
    }

    private void concluir() {
        var agenda = selecionado();
        if (Agendamento.CONCLUIDO.equals(agenda.status())) {
            mensagem("Este agendamento já está concluído.", Theme.MUTED);
            return;
        }
        gravar("Agendamento concluído.", () -> dao.concluir(agenda.id()));
    }

    private void excluir() {
        var agenda = selecionado();
        if (Form.confirmar(this, "Excluir agendamento", "Excluir o agendamento #" + agenda.id() + "?")) {
            gravar("Agendamento excluído.", () -> dao.excluir(agenda.id()));
        }
    }
}
