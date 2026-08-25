package com.clean.it;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class MigrationPostgresTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void migrationsRunAndEnforceStableIdentityReservationAndPaymentInvariants() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load();

        assertThat(flyway.migrate().migrationsExecuted).isGreaterThanOrEqualTo(4);

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            long clientId = insertUser(connection, "https://issuer.example/", "client-sub", "client@example.com");
            long cleanerId = insertUser(connection, "https://issuer.example/", "cleaner-sub", "cleaner@example.com");
            insertCleaner(connection, cleanerId, "cleaner@example.com");
            long jobId = insertJob(connection, clientId);
            long reservationId = insertReservation(connection, jobId, clientId, cleanerId, "cleaner@example.com",
                    "SCHEDULED", "2030-01-01 10:00:00+00", "2030-01-01 12:00:00+00");

            // Changing the cleaner email does not change identity or overlap enforcement.
            updateUserEmail(connection, cleanerId, "cleaner+new@example.com");
            assertThatThrownBy(() -> insertReservation(connection, jobId, clientId, cleanerId,
                    "cleaner+new@example.com", "SCHEDULED",
                    "2030-01-01 11:00:00+00", "2030-01-01 13:00:00+00"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("reservations_no_cleaner_overlap");

            insertReservation(connection, jobId, clientId, cleanerId, "cleaner+new@example.com",
                    "CANCELLED", "2030-01-01 11:00:00+00", "2030-01-01 13:00:00+00");

            insertPayment(connection, reservationId, "pi_test_1");
            assertThatThrownBy(() -> insertPayment(connection, reservationId, "pi_test_2"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("uk_payments_reservation");
        }
    }

    private long insertUser(Connection connection, String issuer, String subject, String email) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO user_accounts(issuer, subject, email, roles)
                VALUES (?, ?, ?, '')
                RETURNING id
                """)) {
            statement.setString(1, issuer);
            statement.setString(2, subject);
            statement.setString(3, email);
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        }
    }

    private void updateUserEmail(Connection connection, long userId, String email) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE user_accounts SET email = ?, updated_at = NOW() WHERE id = ?")) {
            statement.setString(1, email);
            statement.setLong(2, userId);
            statement.executeUpdate();
        }
    }

    private void insertCleaner(Connection connection, long userId, String email) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO cleaners(user_id, email, name, rating)
                VALUES (?, ?, 'Cleaner', 0)
                """)) {
            statement.setLong(1, userId);
            statement.setString(2, email);
            statement.executeUpdate();
        }
    }

    private long insertJob(Connection connection, long clientId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO jobs(client_id, client_email, title, status, price_cents)
                VALUES (?, 'client@example.com', 'Test job', 'OPEN', 5000)
                RETURNING id
                """)) {
            statement.setLong(1, clientId);
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        }
    }

    private long insertReservation(Connection connection, long jobId, long clientId, long cleanerId,
                                   String cleanerEmail, String status, String start, String end) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO reservations(
                    job_id, client_id, client_email, cleaner_id, cleaner_email, start_at, end_at,
                    duration_minutes, agreed_amount_cents, currency, status, version
                ) VALUES (?, ?, 'client@example.com', ?, ?, ?::timestamptz, ?::timestamptz,
                          120, 5000, 'eur', ?, 0)
                RETURNING id
                """)) {
            statement.setLong(1, jobId);
            statement.setLong(2, clientId);
            statement.setLong(3, cleanerId);
            statement.setString(4, cleanerEmail);
            statement.setString(5, start);
            statement.setString(6, end);
            statement.setString(7, status);
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        }
    }

    private void insertPayment(Connection connection, long reservationId, String intentId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO payments(
                    reservation_id, amount_cents, currency, stripe_payment_intent_id,
                    status, version, created_at, updated_at
                ) VALUES (?, 5000, 'eur', ?, 'requires_payment_method', 0, NOW(), NOW())
                """)) {
            statement.setLong(1, reservationId);
            statement.setString(2, intentId);
            statement.executeUpdate();
        }
    }
}
