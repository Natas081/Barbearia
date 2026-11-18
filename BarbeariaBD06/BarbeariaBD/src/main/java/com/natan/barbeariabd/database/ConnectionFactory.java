package com.natan.barbeariabd.database;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class ConnectionFactory {
    private ConnectionFactory() {}

    public static Connection getConnection() throws SQLException {
        Properties local = new Properties();
        Path path = Path.of("barbearia.local.properties");
        if (Files.exists(path)) {
            try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                local.load(reader);
            } catch (IOException | IllegalArgumentException e) {
                throw new SQLException("Não foi possível ler barbearia.local.properties.", "BDCFG", e);
            }
        }
        Properties jdbc = new Properties();
        jdbc.setProperty("user", setting(local, "db.user", "BARBEARIA_DB_USER", "barbearia_app"));
        jdbc.setProperty("password", setting(local, "db.password", "BARBEARIA_DB_PASSWORD", ""));
        jdbc.setProperty("connectTimeout", "5000");
        jdbc.setProperty("socketTimeout", "30000");
        jdbc.setProperty("noAccessToProcedureBodies", "true");
        return DriverManager.getConnection(
                setting(local, "db.url", "BARBEARIA_DB_URL", "jdbc:mysql://127.0.0.1:3306/barbearia"), jdbc);
    }

    private static String setting(Properties local, String key, String env, String fallback) {
        String value = System.getenv(env);
        return value != null ? value : local.getProperty(key, fallback);
    }
}
