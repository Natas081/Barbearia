package com.natan.barbeariabd.model;

import java.time.LocalDate;
import java.util.List;

public record Periodo(LocalDate inicio, LocalDate fim) {
    public Periodo {
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw new IllegalArgumentException("A data final deve ser igual ou posterior à inicial.");
        }
    }

    public boolean ativo() {
        return inicio != null || fim != null;
    }

    public void aplicar(StringBuilder sql, List<Object> parametros, String coluna) {
        if (inicio != null) {
            sql.append(" AND ").append(coluna).append(" >= ?");
            parametros.add(inicio.atStartOfDay());
        }
        if (fim != null) {
            sql.append(" AND ").append(coluna).append(" < ?");
            parametros.add(fim.plusDays(1).atStartOfDay());
        }
    }
}
