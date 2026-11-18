package com.natan.barbeariabd.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class Jdbc {
    private Jdbc() {}

    @FunctionalInterface
    public interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    public interface Row<T> {
        T read(ResultSet row) throws SQLException;
    }

    public static <T> T connect(Work<T> work) throws SQLException {
        try (Connection connection = ConnectionFactory.getConnection()) {
            return work.run(connection);
        }
    }

    public static <T> T transaction(Work<T> work) throws SQLException {
        return connect(connection -> {
            connection.setAutoCommit(false);
            try {
                T value = work.run(connection);
                connection.commit();
                return value;
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        });
    }

    public static <T> List<T> list(String sql, Row<T> read, Object... values) throws SQLException {
        return connect(c -> list(c, sql, read, values));
    }

    public static <T> List<T> list(Connection c, String sql, Row<T> read, Object... values) throws SQLException {
        try (var statement = c.prepareStatement(sql)) {
            bind(statement, values);
            try (var rows = statement.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rows.next()) {
                    result.add(read.read(rows));
                }
                return result;
            }
        }
    }

    public static int update(String sql, Object... values) throws SQLException {
        return connect(c -> update(c, sql, values));
    }

    public static int update(Connection c, String sql, Object... values) throws SQLException {
        try (var statement = c.prepareStatement(sql)) {
            bind(statement, values);
            return statement.executeUpdate();
        }
    }

    public static void bind(PreparedStatement statement, Object... values) throws SQLException {
        for (int i = 0; i < values.length; i++) {
            statement.setObject(i + 1, values[i]);
        }
    }

    public static void requireRow(int count) throws SQLException {
        if (count == 0) {
            throw rule("Registro indisponível. Atualize a lista e tente novamente.");
        }
    }

    public static SQLException rule(String message) {
        return new SQLException(message, "45000");
    }
}
