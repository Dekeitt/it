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
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void migrationsRunAndEnforceMarketplaceAndNotificationInvariants() throws Exception {
        Flyway flyway=Flyway.configure().dataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword()).locations("classpath:db/migration").load();
        assertThat(flyway.migrate().migrationsExecuted).isGreaterThanOrEqualTo(6);
        try(Connection connection=DriverManager.getConnection(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword())){
            assertThat(count(connection,"SELECT count(*) FROM service_types WHERE active = true")).isEqualTo(3);
            long clientId=insertUser(connection,"https://issuer.example/","client-sub","client@example.com");
            long cleanerId=insertUser(connection,"https://issuer.example/","cleaner-sub","cleaner@example.com");
            insertNotification(connection,clientId,"reservation:1:created:email");
            insertNotification(connection,clientId,"reservation:1:created:email");
            assertThat(count(connection,"SELECT count(*) FROM notification_outbox WHERE event_key='reservation:1:created:email'" )).isEqualTo(1);
            assertThat(count(connection,"SELECT count(*) FROM notification_preferences")).isZero();
            assertThat(count(connection,"SELECT count(*) FROM web_push_subscriptions")).isZero();

            insertCleaner(connection,cleanerId,"cleaner@example.com");
            long serviceTypeId=serviceTypeId(connection,"STANDARD");insertOffering(connection,cleanerId,serviceTypeId,1800);
            assertThatThrownBy(()->insertOffering(connection,cleanerId,serviceTypeId,2000)).isInstanceOf(SQLException.class).hasMessageContaining("uk_cleaner_service_offering");
            insertServiceArea(connection,cleanerId,"ES","28");
            assertThatThrownBy(()->insertServiceArea(connection,cleanerId,"ES","28")).isInstanceOf(SQLException.class).hasMessageContaining("uk_cleaner_service_area");
            assertThat(insertAddress(connection,clientId)).isPositive();
            long jobId=insertJob(connection,clientId);
            long reservationId=insertReservation(connection,jobId,clientId,cleanerId,"cleaner@example.com","SCHEDULED","2030-01-01 10:00:00+00","2030-01-01 12:00:00+00");
            updateUserEmail(connection,cleanerId,"cleaner+new@example.com");
            assertThatThrownBy(()->insertReservation(connection,jobId,clientId,cleanerId,"cleaner+new@example.com","SCHEDULED","2030-01-01 11:00:00+00","2030-01-01 13:00:00+00")).isInstanceOf(SQLException.class).hasMessageContaining("reservations_no_cleaner_overlap");
            insertReservation(connection,jobId,clientId,cleanerId,"cleaner+new@example.com","CANCELLED","2030-01-01 11:00:00+00","2030-01-01 13:00:00+00");
            insertPayment(connection,reservationId,"pi_test_1");
            assertThatThrownBy(()->insertPayment(connection,reservationId,"pi_test_2")).isInstanceOf(SQLException.class).hasMessageContaining("uk_payments_reservation");
        }
    }

    private void insertNotification(Connection c,long userId,String eventKey)throws SQLException{try(PreparedStatement s=c.prepareStatement("""
        INSERT INTO notification_outbox(event_key,event_type,recipient_user_id,channel,subject,body,status,available_at)
        VALUES (?, 'RESERVATION_CREATED', ?, 'EMAIL', 'Reserva', 'Confirmada', 'PENDING', NOW())
        ON CONFLICT (event_key) DO NOTHING
        """)){s.setString(1,eventKey);s.setLong(2,userId);s.executeUpdate();}}
    private long count(Connection c,String sql)throws SQLException{try(var s=c.prepareStatement(sql);var r=s.executeQuery()){r.next();return r.getLong(1);}}
    private long serviceTypeId(Connection c,String code)throws SQLException{try(PreparedStatement s=c.prepareStatement("SELECT id FROM service_types WHERE code=?")){s.setString(1,code);try(var r=s.executeQuery()){r.next();return r.getLong(1);}}}
    private void insertOffering(Connection c,long cleanerId,long serviceTypeId,long rate)throws SQLException{try(PreparedStatement s=c.prepareStatement("INSERT INTO cleaner_service_offerings(cleaner_id,service_type_id,hourly_rate_cents,active) VALUES (?,?,?,true)")){s.setLong(1,cleanerId);s.setLong(2,serviceTypeId);s.setLong(3,rate);s.executeUpdate();}}
    private void insertServiceArea(Connection c,long cleanerId,String country,String prefix)throws SQLException{try(PreparedStatement s=c.prepareStatement("INSERT INTO cleaner_service_areas(cleaner_id,country_code,postal_code_prefix) VALUES (?,?,?)")){s.setLong(1,cleanerId);s.setString(2,country);s.setString(3,prefix);s.executeUpdate();}}
    private long insertAddress(Connection c,long userId)throws SQLException{try(PreparedStatement s=c.prepareStatement("INSERT INTO user_addresses(user_id,label,line1,postal_code,city,country_code) VALUES (?, 'Casa','Calle Mayor 1','28001','Madrid','ES') RETURNING id")){s.setLong(1,userId);try(var r=s.executeQuery()){r.next();return r.getLong(1);}}}
    private long insertUser(Connection c,String issuer,String subject,String email)throws SQLException{try(PreparedStatement s=c.prepareStatement("INSERT INTO user_accounts(issuer,subject,email,roles) VALUES (?,?,?,'') RETURNING id")){s.setString(1,issuer);s.setString(2,subject);s.setString(3,email);try(var r=s.executeQuery()){r.next();return r.getLong(1);}}}
    private void updateUserEmail(Connection c,long userId,String email)throws SQLException{try(PreparedStatement s=c.prepareStatement("UPDATE user_accounts SET email=?,updated_at=NOW() WHERE id=?")){s.setString(1,email);s.setLong(2,userId);s.executeUpdate();}}
    private void insertCleaner(Connection c,long userId,String email)throws SQLException{try(PreparedStatement s=c.prepareStatement("INSERT INTO cleaners(user_id,email,name,rating) VALUES (?,?,'Cleaner',0)")){s.setLong(1,userId);s.setString(2,email);s.executeUpdate();}}
    private long insertJob(Connection c,long clientId)throws SQLException{try(PreparedStatement s=c.prepareStatement("INSERT INTO jobs(client_id,client_email,title,status,price_cents) VALUES (?,'client@example.com','Test job','OPEN',5000) RETURNING id")){s.setLong(1,clientId);try(var r=s.executeQuery()){r.next();return r.getLong(1);}}}
    private long insertReservation(Connection c,long jobId,long clientId,long cleanerId,String cleanerEmail,String status,String start,String end)throws SQLException{try(PreparedStatement s=c.prepareStatement("""
        INSERT INTO reservations(job_id,client_id,client_email,cleaner_id,cleaner_email,start_at,end_at,duration_minutes,agreed_amount_cents,currency,status,version)
        VALUES (?,?,'client@example.com',?,?,?::timestamptz,?::timestamptz,120,5000,'eur',?,0) RETURNING id
        """)){s.setLong(1,jobId);s.setLong(2,clientId);s.setLong(3,cleanerId);s.setString(4,cleanerEmail);s.setString(5,start);s.setString(6,end);s.setString(7,status);try(var r=s.executeQuery()){r.next();return r.getLong(1);}}}
    private void insertPayment(Connection c,long reservationId,String intentId)throws SQLException{try(PreparedStatement s=c.prepareStatement("INSERT INTO payments(reservation_id,amount_cents,currency,stripe_payment_intent_id,status,version,created_at,updated_at) VALUES (?,5000,'eur',?,'requires_payment_method',0,NOW(),NOW())")){s.setLong(1,reservationId);s.setString(2,intentId);s.executeUpdate();}}
}
