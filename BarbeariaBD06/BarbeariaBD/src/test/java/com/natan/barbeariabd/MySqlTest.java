package com.natan.barbeariabd;

import com.natan.barbeariabd.dao.*;
import com.natan.barbeariabd.database.Jdbc;
import com.natan.barbeariabd.model.*;
import com.natan.barbeariabd.model.Dashboard.*;
import com.natan.barbeariabd.service.Estatistica;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "BARBEARIA_RUN_IT", matches = "true")
class MySqlTest {
    private static final Periodo TODAS = new Periodo(null, null);
    private static final LocalDate PRIMEIRO = LocalDate.of(2026, 9, 1);
    private static final String CLIENTE = "91000000001";
    private static final String PROFISSIONAL = "92000000001";

    @BeforeAll
    static void protegerBase() throws SQLException {
        assertEquals("barbearia_etapa06_teste", Jdbc.list("SELECT DATABASE()", r -> r.getString(1)).getFirst(),
                "Use uma cópia isolada dos scripts. O fechamento modifica a tabela auxiliar.");
    }

    @Test
    void oitoConsultasDuasViewsEParametros() throws Exception {
        var dao = new ConsultaDAO();
        var consultas = ConsultaDAO.Consulta.values();
        int[] esperados = {30, 30, 30, 30, 0, 90, 15, 5, 30, 30};
        for (int i = 0; i < consultas.length; i++) {
            var consulta = consultas[i];
            var resultado = dao.executar(consulta, "", TODAS, null);
            assertEquals(esperados[i], resultado.linhas().size(), consulta.toString());
            var semCorrespondencia = dao.executar(consulta, "%_? ausência", TODAS, null);
            assertTrue(semCorrespondencia.linhas().isEmpty(), consulta.toString());
            assertFalse(resultado.colunas().isEmpty());
        }
        Periodo doisDias = new Periodo(PRIMEIRO, PRIMEIRO.plusDays(1));
        for (var consulta : consultas) {
            if (consulta.temPeriodo) {
                assertEquals(1, dao.executar(consulta, "", doisDias, "Agendado").linhas().size(), consulta.toString());
            }
        }
        var view = dao.executar(ConsultaDAO.Consulta.VW_FATURAMENTO, "Cliente 1", doisDias, null);
        assertEquals(LocalDateTime.class, view.tipos().get(1));
        assertEquals(1, view.linhas().size());
        assertEquals(new BigDecimal("24.00"), view.linhas().getFirst()[6]);
    }

    @Test
    void dashboardHistoricoFiltrosEEsvaziamento() throws Exception {
        var dao = new DashboardDAO();
        Dados total = dao.consultar(new Filtro(TODAS, null, null, null, Intervalo.SEMANA));
        assertEquals(30, total.clientes());
        assertEquals(60, total.profissionais());
        assertEquals(30, total.agenda().size());
        assertEquals(7, total.agenda().stream().filter(a -> a.status().equals("Concluido")).count());
        assertEquals(30, total.visitas().size());
        assertEquals(30, total.visitas().stream().mapToLong(Visita::itens).sum());
        assertEquals(new BigDecimal("1120.00"), receita(total));
        assertEquals(7, total.servicos().size());
        assertTrue(Estatistica.pearson(total.visitas()).isPresent());
        var recorte = new Filtro(new Periodo(PRIMEIRO, PRIMEIRO.plusDays(2)), "30000000001", 1, "Concluido", Intervalo.DIA);
        Dados filtrados = dao.consultar(recorte);
        assertEquals(1, filtrados.agenda().size());
        assertEquals(1, filtrados.visitas().size());
        assertEquals(1, filtrados.clientes());
        assertEquals(1, filtrados.profissionais());
        assertEquals(new BigDecimal("24.00"), receita(filtrados));
        assertEquals(1, filtrados.servicos().size());
        assertEquals("Basico", filtrados.visitas().getFirst().classificacao());
        var vazio = dao.consultar(new Filtro(new Periodo(null, PRIMEIRO.minusYears(10)), null, null, null, Intervalo.MES));
        assertTrue(vazio.agenda().isEmpty());
        assertTrue(vazio.visitas().isEmpty());
        assertTrue(vazio.servicos().isEmpty());
    }

    @Test
    void crudFluxoDeConclusaoFuncoesEReajusteAuditado() throws Exception {
        var clientes = new ClienteDAO();
        var profissionais = new ProfissionalDAO();
        var servicos = new ServicoDAO();
        var agenda = new AgendamentoDAO();
        var visitas = new AtendimentoDAO();
        var ops = new OperacoesDAO();
        var preco = new BigDecimal("79.00");
        var precoHistorico = new BigDecimal("65.50");
        var novoPreco = new BigDecimal("125.00");
        var servico = new Servico(null, "Serviço temporário Etapa 06", null, 40, preco);
        Integer agendamento = null;
        Integer atendimento = null;

        clientes.inserir(new Cliente(CLIENTE, "Cliente Etapa 06", "Rua A", "1"));
        try {
            clientes.atualizar(new Cliente(CLIENTE, "Cliente revisado", "Rua A", "2"));
            assertEquals("Cliente revisado", clientes.listar().stream().filter(c -> CLIENTE.equals(c.getCpf()))
                    .findFirst().orElseThrow().getNome());
            var b = new Profissional.Barbeiro("Especialidade", "10:00-18:00", "30000000001");
            var p = new Profissional(PROFISSIONAL, "Profissional Etapa 06", "Rua B", "3", b, null);
            profissionais.salvar(p, true);
            assertTrue(profissionais.barbeiros().stream().anyMatch(escolha -> PROFISSIONAL.equals(escolha.id())));
            profissionais.salvar(new Profissional(PROFISSIONAL, "Profissional revisado", p.rua(), p.numero(), b, preco), false);
            assertEquals("Barbeiro e gerente", profissionais.listar().stream().filter(q -> PROFISSIONAL.equals(q.cpf()))
                    .findFirst().orElseThrow().funcao());

            servicos.inserir(servico);
            servico.setDuracao(50);
            servicos.atualizar(servico);
            assertEquals(50, servicos.listar().stream().filter(s -> servico.getIdServico().equals(s.getIdServico()))
                    .findFirst().orElseThrow().getDuracao());
            LocalDateTime inicio = PRIMEIRO.plusMonths(1).atTime(12, 0);
            LocalDateTime fim = inicio.plusMinutes(45);
            agenda.salvar(new Agendamento(null, CLIENTE, null, PROFISSIONAL, null, inicio, "Agendado", null));
            agendamento = agenda.listar().stream().filter(a -> CLIENTE.equals(a.clienteCpf()))
                    .findFirst().orElseThrow().id();
            int agendaId = agendamento;
            agenda.salvar(new Agendamento(agendamento, CLIENTE, null, PROFISSIONAL, null, inicio, "Confirmado", null));
            SQLException rejeicao = assertThrows(SQLException.class, () -> agenda.concluir(agendaId));
            assertEquals("45000", rejeicao.getSQLState());
            assertTrue(rejeicao.getMessage().contains("servico registrado"));
            visitas.salvar(null, agendamento, inicio, null);
            atendimento = visitas.listar().stream().filter(t -> Integer.valueOf(agendaId).equals(t.agendamento()))
                    .findFirst().orElseThrow().id();
            int visitaId = atendimento;
            assertEquals("Sem servicos", visitas.demonstrarFuncoes(atendimento).classificacao());
            visitas.salvar(atendimento, agendamento, inicio, fim);
            assertThrows(SQLException.class, () -> agenda.concluir(agendaId));
            visitas.salvar(atendimento, agendamento, inicio, null);
            visitas.salvarItem(atendimento, servico.getIdServico(), precoHistorico, true);
            assertThrows(SQLException.class, () -> agenda.concluir(agendaId));
            visitas.salvar(atendimento, agendamento, inicio, fim);
            visitas.salvarItem(atendimento, 1, new BigDecimal("50.00"), true);
            agenda.concluir(agendamento);
            assertEquals("Concluido", agenda.listar().stream().filter(a -> a.id() == agendaId).findFirst().orElseThrow().status());
            assertEquals("Intermediario", visitas.demonstrarFuncoes(atendimento).classificacao());
            assertThrows(SQLException.class, () -> agenda.excluir(agendaId));
            assertThrows(SQLException.class, () -> clientes.excluir(CLIENTE));
            assertThrows(SQLException.class, () -> servicos.excluir(servico.getIdServico()));
            assertThrows(SQLException.class, () -> visitas.excluirItem(visitaId, 1));

            servicos.reajustar(servico.getIdServico(), preco, novoPreco);
            assertEquals(new BigDecimal("115.50"), visitas.demonstrarFuncoes(atendimento).total());
            var recorte = new Filtro(new Periodo(inicio.toLocalDate(), inicio.toLocalDate()), PROFISSIONAL,
                    servico.getIdServico(), "Concluido", Intervalo.DIA);
            var dados = new DashboardDAO().consultar(recorte);
            assertEquals(new BigDecimal("115.50"), receita(dados));
            assertEquals(1, dados.visitas().size());
            assertEquals(2, dados.visitas().getFirst().itens());
            assertEquals(2, dados.servicos().size());
            var log = ops.auditoria(TODAS, servico.getIdServico()).linhas();
            assertEquals(1, log.size());
            assertEquals(preco, log.getFirst()[3]);
            assertEquals(novoPreco, log.getFirst()[4]);
            assertInstanceOf(LocalDateTime.class, log.getFirst()[5]);
            assertFalse(((String) log.getFirst()[6]).isBlank());

            agenda.salvar(new Agendamento(agendamento, CLIENTE, null, PROFISSIONAL, null, inicio, "Confirmado", null));
            visitas.salvarItem(atendimento, servico.getIdServico(), BigDecimal.ONE, false);
            assertEquals(new BigDecimal("51.00"), visitas.demonstrarFuncoes(atendimento).total());
            visitas.excluirItem(atendimento, servico.getIdServico());
            visitas.excluirItem(atendimento, 1);
            visitas.excluir(atendimento);
            atendimento = null;
            agenda.excluir(agendamento);
            agendamento = null;
        } finally {
            // Limpeza de dados exclusivos da base de teste, inclusive após uma asserção que falhe.
            if (atendimento != null) {
                Jdbc.update("DELETE FROM atendimento WHERE id_atendimento = ?", atendimento);
            }
            if (agendamento != null) {
                Jdbc.update("DELETE FROM agendamento WHERE id_agendamento = ?", agendamento);
            }
            if (servico.getIdServico() != null) {
                Jdbc.update("DELETE FROM log_preco_servico WHERE id_servico = ?", servico.getIdServico());
                servicos.excluir(servico.getIdServico());
            }
            if (profissionais.listar().stream().anyMatch(p -> PROFISSIONAL.equals(p.cpf()))) {
                profissionais.excluir(PROFISSIONAL);
            }
            clientes.excluir(CLIENTE);
        }
        assertTrue(clientes.listar().stream().noneMatch(c -> CLIENTE.equals(c.getCpf())));
        assertTrue(profissionais.listar().stream().noneMatch(p -> PROFISSIONAL.equals(p.cpf())));
    }

    @Test
    void fechamentoReconstroiLotesEHonraAtendimentosIndivisiveis() throws Exception {
        var dao = new OperacoesDAO();
        var unico = dao.gerarFechamento(new BigDecimal("2000.00"));
        assertEquals(1, unico.lotes().linhas().size());
        assertEquals(new BigDecimal("1120.00"), unico.lotes().linhas().getFirst()[2]);
        var dividido = dao.gerarFechamento(new BigDecimal("50.00"));
        assertEquals(30, dividido.atendimentos().linhas().size());
        assertTrue(dividido.lotes().linhas().size() > 1);
        for (Object[] lote : dividido.lotes().linhas()) {
            BigDecimal valor = (BigDecimal) lote[2];
            if (valor.compareTo(new BigDecimal("50.00")) > 0) {
                assertEquals(1L, lote[1], "Atendimento acima do limite fica sozinho no lote.");
            }
        }
        assertEquals(dividido.lotes().linhas().size(), dao.fechamento().lotes().linhas().size());
        assertThrows(SQLException.class, () -> dao.gerarFechamento(BigDecimal.ZERO));
        assertEquals(30, dao.fechamento().atendimentos().linhas().size());
    }

    private BigDecimal receita(Dados dados) {
        return dados.visitas().stream().map(Visita::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
