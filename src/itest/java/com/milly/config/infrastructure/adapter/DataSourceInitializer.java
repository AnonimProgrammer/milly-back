package com.milly.config.infrastructure.adapter;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DataSourceInitializer {

    private static final Object LOCK = new Object();

    public static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withReuse(true);

    private DataSourceInitializer() {
    }

    static void startContainerIfNeeded() {
        synchronized (LOCK) {
            if (!POSTGRES.isRunning()) {
                POSTGRES.start();
            }
        }
    }

    static void releaseStaleConnections() {
        if (!POSTGRES.isRunning()) {
            return;
        }

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                Statement statement = connection.createStatement()) {
            statement.execute("""
                    SELECT pg_terminate_backend(pid)
                    FROM pg_stat_activity
                    WHERE datname = current_database()
                      AND pid <> pg_backend_pid()
                    """);
        } catch (SQLException ignored) {
            // Best-effort cleanup when reusing the container across test contexts.
        }
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext context) {
            startContainerIfNeeded();
            releaseStaleConnections();

            TestPropertyValues.of(
                            "spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                            "spring.datasource.username=" + POSTGRES.getUsername(),
                            "spring.datasource.password=" + POSTGRES.getPassword(),
                            "spring.datasource.driver-class-name=org.postgresql.Driver")
                    .applyTo(context.getEnvironment());
        }
    }
}
