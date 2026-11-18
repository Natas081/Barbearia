package com.natan.barbeariabd.view;

import com.natan.barbeariabd.dao.ProfissionalDAO;
import com.natan.barbeariabd.model.Escolha;
import com.natan.barbeariabd.model.Profissional;
import javax.swing.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import static com.natan.barbeariabd.view.Theme.campo;

final class ProfissionalPanel extends ListaPage<Profissional> {
    private final ProfissionalDAO dao = new ProfissionalDAO();

    ProfissionalPanel(Consumer<Banco> banco) {
        super(banco, "Buscar profissional ou CPF", Profissional::cpf,
                new TableView.Col<>("Nome", String.class, Profissional::nome),
                new TableView.Col<>("CPF", String.class, Profissional::cpf),
                new TableView.Col<>("Função", String.class, Profissional::funcao),
                new TableView.Col<>("Especialidade", String.class, p -> p.barbeiro() == null ? null : p.barbeiro().especialidade()),
                new TableView.Col<>("Expediente", String.class, p -> p.barbeiro() == null ? null : p.barbeiro().horario()),
                new TableView.Col<>("Bonificação", BigDecimal.class, Profissional::bonificacao));
        botao("Atualizar", false, this::atualizar);
        botao("Editar", false, () -> abrir(selecionado()));
        botao("Excluir", false, this::excluir);
        botao("＋ Novo profissional", true, () -> abrir(null));
    }

    @Override
    List<Profissional> listar() throws Exception {
        return dao.listar();
    }

    private void abrir(Profissional atual) {
        executar("Carregando supervisores…", dao::barbeiros, opcoes -> editar(atual, opcoes));
    }

    private void editar(Profissional atual, List<Escolha<String>> opcoes) {
        boolean novo = atual == null;
        var cpf = Form.campo(novo ? "" : atual.cpf(), 29);
        cpf.setEditable(novo);
        var nome = Form.campo(novo ? "" : atual.nome(), 29);
        var rua = Form.campo(novo ? "" : atual.rua(), 29);
        var numero = Form.campo(novo ? "" : atual.numero(), 29);
        var b = novo ? null : atual.barbeiro();
        var cortar = new JCheckBox("Barbeiro", b != null);
        var gerenciar = new JCheckBox("Gerente", !novo && atual.bonificacao() != null);
        var funcoes = Theme.row(5);
        funcoes.add(cortar);
        funcoes.add(gerenciar);
        var especialidade = Form.campo(b == null ? "Corte masculino" : b.especialidade(), 29);
        var horario = Form.campo(b == null ? "08:00-18:00" : b.horario(), 29);
        var supervisor = new JComboBox<Escolha<String>>();
        Form.opcoes(supervisor, opcoes.stream().filter(s -> novo || !atual.cpf().equals(s.id())).toList(),
                "Sem supervisor", b == null ? null : b.supervisor());
        var bonus = Form.campo(gerenciar.isSelected() ? atual.bonificacao().toPlainString() : "0,00", 29);
        Runnable habilitar = () -> {
            especialidade.setEnabled(cortar.isSelected());
            horario.setEnabled(cortar.isSelected());
            supervisor.setEnabled(cortar.isSelected());
            bonus.setEnabled(gerenciar.isSelected());
        };
        cortar.addActionListener(e -> habilitar.run());
        gerenciar.addActionListener(e -> habilitar.run());
        habilitar.run();
        var valor = Form.editar(this, novo ? "Novo profissional" : "Editar profissional", () -> new Profissional(
                        Form.cpf(cpf), Form.texto(nome, "Nome", 100, true), Form.texto(rua, "Rua", 100, true),
                        Form.texto(numero, "Número", 10, true),
                        cortar.isSelected() ? new Profissional.Barbeiro(Form.texto(especialidade, "Especialidade", 50, true),
                                Form.texto(horario, "Expediente", 50, true), Form.id(supervisor)) : null,
                        gerenciar.isSelected() ? Form.dinheiro(bonus, "Bonificação", 8, false) : null),
                campo("CPF", cpf), campo("Nome completo", nome), campo("Rua", rua), campo("Número", numero),
                campo("Especializações", funcoes), campo("Especialidade de barbeiro", especialidade),
                campo("Expediente", horario), campo("Supervisor", supervisor), campo("Bonificação (R$)", bonus));
        if (valor != null) {
            gravar(novo ? "Profissional cadastrado." : "Profissional atualizado.", () -> dao.salvar(valor, novo));
        }
    }

    private void excluir() {
        var p = selecionado();
        String texto = "Excluir " + p.nome() + ", suas especializações e seus telefones?";
        if (Form.confirmar(this, "Excluir profissional", texto)) {
            gravar("Profissional excluído.", () -> dao.excluir(p.cpf()));
        }
    }
}
