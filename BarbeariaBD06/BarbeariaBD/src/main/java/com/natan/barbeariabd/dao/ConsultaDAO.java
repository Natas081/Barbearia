package com.natan.barbeariabd.dao;

import com.natan.barbeariabd.model.Periodo;
import com.natan.barbeariabd.model.Resultado;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ConsultaDAO {
    public enum Consulta {
        CLIENTES("Clientes e agendamentos", "INNER JOIN", "Confira cliente, barbeiro e situação de cada reserva.", true, true),
        POR_BARBEIRO("Agendamentos por barbeiro", "LEFT JOIN", "Compara a quantidade de reservas e inclui barbeiros sem agendamentos."),
        TOTAIS("Valores dos atendimentos", "Agregação", "Soma os preços praticados por atendimento, inclusive totais zerados.", true, true),
        SERVICOS("Serviços mais realizados", "Agregação", "Mostra quantidade, preço médio praticado e faturamento por serviço."),
        SEM_AGENDA("Clientes sem agendamentos", "Anti join", "Localiza clientes que ainda não possuem reservas."),
        PESSOAS("Clientes e profissionais", "FULL OUTER JOIN simulado", "Cruza os cadastros pelo CPF e mantém os registros sem par."),
        PRECO_MEDIO("Serviços acima do preço médio", "Subconsulta", "Compara cada preço atual com a média do catálogo."),
        BARBEIROS_MEDIA("Barbeiros acima da média", "Subconsulta", "Compara com a média dos barbeiros que têm ao menos um agendamento."),
        VW_AGENDA("Agenda detalhada", "View · vw_agenda_detalhada", "Consulte os vínculos e a situação das reservas na view do MySQL.", true, true),
        VW_FATURAMENTO("Faturamento de atendimentos", "View · vw_faturamento_atendimento", "Consulte horários e totais históricos na view do MySQL.", true, true);

        public final String titulo;
        public final String tipo;
        public final String descricao;
        public final boolean temPeriodo;
        public final boolean temStatus;

        Consulta(String titulo, String tipo, String descricao) {
            this(titulo, tipo, descricao, false, false);
        }

        Consulta(String titulo, String tipo, String descricao, boolean temPeriodo, boolean temStatus) {
            this.titulo = titulo;
            this.tipo = tipo;
            this.descricao = descricao;
            this.temPeriodo = temPeriodo;
            this.temStatus = temStatus;
        }

        @Override
        public String toString() {
            return titulo;
        }
    }

    private record ConsultaSql(String corpo, String busca, String ordem) {}

    public Resultado executar(Consulta consulta, String texto, Periodo periodo, String status) throws SQLException {
        ConsultaSql fonte = sql(consulta);
        StringBuilder sql = new StringBuilder("SELECT q.* FROM (" + fonte.corpo + ") q WHERE 1=1");
        List<Object> valores = new ArrayList<>();
        if (!texto.isBlank()) {
            sql.append(" AND LOCATE(?, ").append(fonte.busca).append(") > 0");
            valores.add(texto);
        }
        if (consulta.temPeriodo && periodo.ativo()) {
            String data = switch (consulta) {
                case CLIENTES, VW_AGENDA -> """
                        (SELECT COALESCE(a.horario_marcado, t.horario_inicio)
                         FROM agendamento a LEFT JOIN atendimento t ON t.id_agendamento = a.id_agendamento
                         WHERE a.id_agendamento = q.`Agendamento`)
                        """;
                case TOTAIS -> "(SELECT t.horario_inicio FROM atendimento t WHERE t.id_atendimento = q.`Atendimento`)";
                case VW_FATURAMENTO -> "q.`Início`";
                default -> throw new IllegalArgumentException("Consulta sem filtro temporal");
            };
            periodo.aplicar(sql, valores, data);
        }
        if (consulta.temStatus && status != null) {
            if (consulta == Consulta.TOTAIS) {
                sql.append("""
                         AND EXISTS (SELECT 1 FROM atendimento t JOIN agendamento a ON a.id_agendamento = t.id_agendamento
                             WHERE t.id_atendimento = q.`Atendimento` AND a.status = ?)
                        """);
            } else {
                sql.append(" AND q.`Status` = ?");
            }
            valores.add(status);
        }
        sql.append(" ORDER BY ").append(fonte.ordem);
        return ResultadosSQL.ler(sql.toString(), valores.toArray());
    }

    private ConsultaSql sql(Consulta consulta) {
        return switch (consulta) {
            case CLIENTES -> new ConsultaSql("""
                    SELECT c.nome AS Cliente, a.id_agendamento AS Agendamento, a.status AS Status, p.nome AS Barbeiro
                    FROM cliente c JOIN agendamento a ON a.cpf_cliente = c.cpf
                    JOIN barbeiro b ON b.cpf_profissional = a.cpf_barbeiro
                    JOIN profissional p ON p.cpf = b.cpf_profissional
                    """, "CONCAT_WS(' ', Cliente, Agendamento, Barbeiro)", "Agendamento");
            case POR_BARBEIRO -> new ConsultaSql("""
                    SELECT p.nome AS Barbeiro, COUNT(a.id_agendamento) AS Agendamentos
                    FROM barbeiro b JOIN profissional p ON p.cpf = b.cpf_profissional
                    LEFT JOIN agendamento a ON a.cpf_barbeiro = b.cpf_profissional
                    GROUP BY p.cpf, p.nome
                    """, "Barbeiro", "Agendamentos DESC, Barbeiro");
            case TOTAIS -> new ConsultaSql("""
                    SELECT t.id_atendimento AS Atendimento, c.nome AS Cliente,
                        COALESCE(SUM(i.preco_praticado), 0) AS Total
                    FROM atendimento t LEFT JOIN agendamento a ON a.id_agendamento = t.id_agendamento
                    LEFT JOIN cliente c ON c.cpf = a.cpf_cliente
                    LEFT JOIN item_atendimento i ON i.id_atendimento = t.id_atendimento
                    GROUP BY t.id_atendimento, c.nome
                    """, "CONCAT_WS(' ', Atendimento, Cliente)", "Atendimento");
            case SERVICOS -> new ConsultaSql("""
                    SELECT s.nome AS Serviço, COUNT(i.id_servico) AS Execuções,
                        COALESCE(ROUND(AVG(i.preco_praticado), 2), 0) AS `Preço médio`,
                        COALESCE(SUM(i.preco_praticado), 0) AS Faturamento
                    FROM servico s LEFT JOIN item_atendimento i ON i.id_servico = s.id_servico
                    GROUP BY s.id_servico, s.nome
                    """, "`Serviço`", "`Execuções` DESC, Faturamento DESC, `Serviço`");
            case SEM_AGENDA -> new ConsultaSql("""
                    SELECT c.cpf AS CPF, c.nome AS Cliente
                    FROM cliente c LEFT JOIN agendamento a ON a.cpf_cliente = c.cpf
                    WHERE a.id_agendamento IS NULL
                    """, "CONCAT_WS(' ', CPF, Cliente)", "Cliente, CPF");
            case PESSOAS -> new ConsultaSql("""
                    SELECT c.cpf AS `CPF do cliente`, c.nome AS Cliente,
                           p.cpf AS `CPF do profissional`, p.nome AS Profissional
                    FROM cliente c LEFT JOIN profissional p ON p.cpf = c.cpf
                    UNION
                    SELECT c.cpf, c.nome, p.cpf, p.nome
                    FROM profissional p LEFT JOIN cliente c ON c.cpf = p.cpf
                    """, "CONCAT_WS(' ', `CPF do cliente`, Cliente, `CPF do profissional`, Profissional)",
                    "COALESCE(Cliente, Profissional), `CPF do cliente`, `CPF do profissional`");
            case PRECO_MEDIO -> new ConsultaSql("""
                    SELECT s.id_servico AS Código, s.nome AS Serviço, s.preco AS Preço
                    FROM servico s WHERE s.preco > (SELECT AVG(preco) FROM servico)
                    """, "CONCAT_WS(' ', `Código`, `Serviço`)", "`Preço` DESC, `Serviço`");
            case BARBEIROS_MEDIA -> new ConsultaSql("""
                    SELECT p.nome AS Barbeiro, COUNT(a.id_agendamento) AS Agendamentos
                    FROM barbeiro b JOIN profissional p ON p.cpf = b.cpf_profissional
                    LEFT JOIN agendamento a ON a.cpf_barbeiro = b.cpf_profissional
                    GROUP BY b.cpf_profissional, p.nome
                    HAVING COUNT(a.id_agendamento) > (
                        SELECT AVG(t.total) FROM (
                            SELECT COUNT(*) AS total FROM agendamento GROUP BY cpf_barbeiro
                        ) t
                    )
                    """, "Barbeiro", "Agendamentos DESC, Barbeiro");
            case VW_AGENDA -> new ConsultaSql("""
                    SELECT id_agendamento AS Agendamento, status AS Status, cpf_cliente AS `CPF do cliente`,
                           cliente AS Cliente, cpf_barbeiro AS `CPF do barbeiro`, barbeiro AS Barbeiro
                    FROM vw_agenda_detalhada
                    """, "CONCAT_WS(' ', Agendamento, `CPF do cliente`, Cliente, `CPF do barbeiro`, Barbeiro)", "Agendamento");
            case VW_FATURAMENTO -> new ConsultaSql("""
                    SELECT id_atendimento AS Atendimento, horario_inicio AS Início, horario_final AS Fim,
                           id_agendamento AS Agendamento, status AS Status, cliente AS Cliente, valor_total AS Total
                    FROM vw_faturamento_atendimento
                    """, "CONCAT_WS(' ', Atendimento, Agendamento, Cliente)", "Atendimento");
        };
    }
}
