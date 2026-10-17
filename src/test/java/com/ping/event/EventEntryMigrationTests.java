package com.ping.event;

import java.sql.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class EventEntryMigrationTests {
    @Test void upgradesExistingEventsWithoutChangingDataAndEnforcesStableUniqueCodes() throws Exception {
        String url="jdbc:h2:mem:entry-upgrade;MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (var source=new org.springframework.jdbc.datasource.SingleConnectionDataSource(url,"sa","",true)) {
        Flyway.configure().dataSource(source).target("12").load().migrate();
        try(var connection=source.getConnection();var sql=connection.createStatement()) {
            sql.executeUpdate("INSERT INTO events(id,name,description,status,created_at,updated_at,version,meeting_point) VALUES (42,'Antes','Conservar','PAUSED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,7,'Lugar anterior')");
            Flyway current=Flyway.configure().dataSource(source).load(); current.migrate();
            try(var rows=sql.executeQuery("SELECT * FROM events WHERE id=42")) {
                assertThat(rows.next()).isTrue();assertThat(rows.getString("entry_code")).isEqualTo("PING42");
                assertThat(rows.getString("name")).isEqualTo("Antes");assertThat(rows.getString("description")).isEqualTo("Conservar");
                assertThat(rows.getString("status")).isEqualTo("PAUSED");assertThat(rows.getLong("version")).isEqualTo(7);assertThat(rows.getString("meeting_point")).isEqualTo("Lugar anterior");
            }
            assertThat(current.migrate().migrationsExecuted).isZero();
            assertThatThrownBy(()->sql.executeUpdate("INSERT INTO events(name,description,status,created_at,updated_at,meeting_point,entry_code) VALUES ('Duplicado','','DRAFT',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'Punto','PING42')")).isInstanceOf(SQLException.class);
            assertThatThrownBy(()->sql.executeUpdate("INSERT INTO events(name,description,status,created_at,updated_at,meeting_point) VALUES ('Sin código','','DRAFT',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'Punto')")).isInstanceOf(SQLException.class);
            try(var rows=sql.executeQuery("SELECT COUNT(*) FROM events")) {rows.next();assertThat(rows.getInt(1)).isEqualTo(1);}
        }
        }
    }
}
