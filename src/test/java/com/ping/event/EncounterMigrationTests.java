package com.ping.event;

import java.sql.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class EncounterMigrationTests {
    @Test void v14PreservesExistingEventActiveAndPendingSagaConditions() throws Exception {
        String url="jdbc:h2:mem:encounter-upgrade;MODE=MySQL;DB_CLOSE_DELAY=-1";
        try(var source=new org.springframework.jdbc.datasource.SingleConnectionDataSource(url,"sa","",true)) {
            Flyway.configure().dataSource(source).target("13").load().migrate();
            try(var connection=source.getConnection();var sql=connection.createStatement()) {
                sql.executeUpdate("INSERT INTO events(id,name,description,status,created_at,updated_at,version,meeting_point,entry_code) VALUES (42,'Antes','Conservar','PAUSED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,7,'Lugar anterior','PING42')");
                sql.executeUpdate("INSERT INTO participants(id,alias,event_id,available,created_at,points) VALUES (1,'Uno',42,false,CURRENT_TIMESTAMP,30),(2,'Dos',42,false,CURRENT_TIMESTAMP,20)");
                sql.executeUpdate("INSERT INTO ping_matches(id,event_id,first_id,second_id,status,first_accepted,second_accepted,first_confirmed,first_wait,second_wait,common_interests,first_interests,second_interests,meeting_point,word,activity,created_at,completion_pending) VALUES (5,42,1,2,'ACTIVE',true,true,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'','','','Lugar original','ANTES-5','Actividad original',CURRENT_TIMESTAMP,false),(6,42,1,2,'ACTIVE',true,true,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'','','','Lugar pendiente','ANTES-6','Actividad pendiente',CURRENT_TIMESTAMP,true)");
                sql.executeUpdate("INSERT INTO match_completion_sagas(match_id,status,attempt,trigger_participant_id,first_granted,second_granted) VALUES(6,'FIRST_REWARD_GRANTED',1,2,true,false)");
                Flyway current=Flyway.configure().dataSource(source).load(); assertThat(current.migrate().migrationsExecuted).isEqualTo(1);
                try(var rows=sql.executeQuery("SELECT * FROM events WHERE id=42")) {
                    rows.next(); assertThat(rows.getString("entry_code")).isEqualTo("PING42"); assertThat(rows.getString("meeting_point")).isEqualTo("Lugar anterior"); assertThat(rows.getLong("version")).isEqualTo(7);
                    assertThat(rows.getString("meeting_points")).isEmpty(); assertThat(rows.getString("encounter_words")).isEmpty(); assertThat(rows.getString("encounter_activities")).isEmpty();
                }
                try(var rows=sql.executeQuery("SELECT * FROM ping_matches ORDER BY id")) {
                    while(rows.next()) {assertThat(rows.getBoolean("arrival_required")).isFalse(); assertThat(rows.getBoolean("first_arrived")).isFalse(); assertThat(rows.getBoolean("second_arrived")).isFalse(); assertThat(rows.getBoolean("first_confirmed")).isTrue();assertThat(rows.getString("word")).isEqualTo("ANTES-"+rows.getLong("id"));}
                }
                try(var rows=sql.executeQuery("SELECT points FROM participants ORDER BY id")) {rows.next();assertThat(rows.getLong(1)).isEqualTo(30);rows.next();assertThat(rows.getLong(1)).isEqualTo(20);}
                try(var rows=sql.executeQuery("SELECT status, first_granted FROM match_completion_sagas WHERE match_id=6")) {rows.next();assertThat(rows.getString(1)).isEqualTo("FIRST_REWARD_GRANTED");assertThat(rows.getBoolean(2)).isTrue();}
                assertThat(current.migrate().migrationsExecuted).isZero();
            }
        }
    }
}
