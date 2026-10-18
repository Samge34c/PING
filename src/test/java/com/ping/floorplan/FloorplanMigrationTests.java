package com.ping.floorplan;

import java.sql.*;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

public class FloorplanMigrationTests {
    @Test void v18KeepsOldEncounterAndSelectionsWithoutInventingCoordinates() throws Exception {
        try(var source=new org.springframework.jdbc.datasource.SingleConnectionDataSource("jdbc:h2:mem:floorplan-upgrade;MODE=MySQL;DB_CLOSE_DELAY=-1","sa","",true)){verify(source);}
    }
    public static void verify(DataSource source) throws Exception {
        Flyway.configure().dataSource(source).target("17").load().migrate();
        try(var connection=source.getConnection();var sql=connection.createStatement()){
            sql.executeUpdate("insert into events(id,name,description,status,created_at,updated_at,version,meeting_point,entry_code) values(42,'Anterior','Conservar','OPEN',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,7,'Plaza','PLANO42')");
            sql.executeUpdate("insert into participants(id,alias,event_id,available,created_at,points) values(1,'Uno',42,false,CURRENT_TIMESTAMP,21),(2,'Dos',42,false,CURRENT_TIMESTAMP,34)");
            sql.executeUpdate("insert into participant_interests(participant_id,interest_id) select 1,id from interests where name in ('Videojuegos','Música')");
            sql.executeUpdate("insert into ping_matches(id,event_id,first_id,second_id,status,first_accepted,second_accepted,first_wait,second_wait,common_interests,first_interests,second_interests,meeting_point,word,activity,created_at,reward_points,meeting_directions) values(5,42,1,2,'ACTIVE',true,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'Videojuegos','Videojuegos','Videojuegos','Plaza original','PLANO-5','Actividad original',CURRENT_TIMESTAMP,37,'Junto al letrero original')");
            var flyway=Flyway.configure().dataSource(source).target("18").load();assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
            try(var rows=sql.executeQuery("select floorplan_id,meeting_point_id,floorplan_x,floorplan_y,word,reward_points,meeting_directions from ping_matches where id=5")){
                rows.next();for(int n=1;n<=4;n++)assertThat(rows.getObject(n)).isNull();assertThat(rows.getString(5)).isEqualTo("PLANO-5");assertThat(rows.getInt(6)).isEqualTo(37);assertThat(rows.getString(7)).isEqualTo("Junto al letrero original");
            }
            try(var rows=sql.executeQuery("select points from participants order by id")){rows.next();assertThat(rows.getInt(1)).isEqualTo(21);rows.next();assertThat(rows.getInt(1)).isEqualTo(34);}
            try(var rows=sql.executeQuery("select count(*) from participant_interests")){rows.next();assertThat(rows.getInt(1)).isEqualTo(2);}
            try(var rows=sql.executeQuery("select count(*) from interest_categories")){rows.next();assertThat(rows.getInt(1)).isEqualTo(3);}
            try(var rows=sql.executeQuery("select floorplan_id from events where id=42")){rows.next();assertThat(rows.getObject(1)).isNull();}
            assertThat(flyway.migrate().migrationsExecuted).isZero();
            System.out.println(connection.getMetaData().getDatabaseProductName()+" "+connection.getMetaData().getDatabaseProductVersion()+" V17->V18 preserves ACTIVE snapshot, selections, categories and balances; no historical coordinates; rerun0.");
        }
    }
}
