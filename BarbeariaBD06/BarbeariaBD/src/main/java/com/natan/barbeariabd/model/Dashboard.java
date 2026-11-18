package com.natan.barbeariabd.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class Dashboard {
    private Dashboard() {}

    public enum Intervalo {
        DIA("Dia"), SEMANA("Semana"), MES("Mês");

        private final String label;

        Intervalo(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public record Filtro(Periodo periodo, String barbeiro, Integer servico, String status, Intervalo intervalo) {
        public boolean ativo() {
            return periodo.ativo() || barbeiro != null || servico != null || status != null;
        }
    }

    public record Agenda(String cliente, String barbeiro, String nomeBarbeiro, String status) {}
    public record Visita(String cliente, String barbeiro, LocalDate dia, BigDecimal total,
                         long itens, long segundos, String classificacao) {}
    public record ServicoMedido(String nome, long quantidade, BigDecimal receita) {}
    public record Dados(long clientes, long profissionais, List<Agenda> agenda, List<Visita> visitas,
                        List<ServicoMedido> servicos) {}
}
