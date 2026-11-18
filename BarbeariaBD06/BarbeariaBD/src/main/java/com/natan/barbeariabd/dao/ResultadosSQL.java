package com.natan.barbeariabd.dao;

import com.natan.barbeariabd.database.Jdbc;
import com.natan.barbeariabd.model.Resultado;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

final class ResultadosSQL {
    private ResultadosSQL() {}

    static Resultado ler(String sql, Object... valores) throws SQLException {
        return Jdbc.connect(c -> ler(c, sql, valores));
    }

    static Resultado ler(Connection c, String sql, Object... valores) throws SQLException {
        try (var statement = c.prepareStatement(sql)) {
            Jdbc.bind(statement, valores);
            try (var rs = statement.executeQuery()) {
                var meta = rs.getMetaData();
                List<String> colunas = new ArrayList<>();
                List<Class<?>> tipos = new ArrayList<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    colunas.add(meta.getColumnLabel(i));
                    tipos.add(meta.getColumnType(i) == Types.TIMESTAMP
                            ? LocalDateTime.class : resolverClasse(meta.getColumnClassName(i)));
                }
                List<Object[]> linhas = new ArrayList<>();
                while (rs.next()) {
                    Object[] linha = new Object[colunas.size()];
                    for (int i = 0; i < linha.length; i++) {
                        linha[i] = tipos.get(i) == LocalDateTime.class
                                ? rs.getObject(i + 1, LocalDateTime.class) : rs.getObject(i + 1);
                    }
                    linhas.add(linha);
                }
                return new Resultado(colunas, tipos, linhas);
            }
        }
    }

    private static Class<?> resolverClasse(String tipo) {
        return switch (tipo) {
            case "java.lang.Integer" -> Integer.class;
            case "java.lang.Long" -> Long.class;
            case "java.math.BigDecimal" -> java.math.BigDecimal.class;
            case "java.lang.String" -> String.class;
            default -> Object.class;
        };
    }
}
