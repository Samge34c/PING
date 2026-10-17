package com.ping.interest;
import java.sql.*;import javax.sql.DataSource;import java.util.*;
import org.flywaydb.core.Flyway;import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
public class InterestCategoryMigrationTests {
 @Test void upgradesV16WithoutChangingIdsSelectionsOrEncounter()throws Exception{
  try(var source=new org.springframework.jdbc.datasource.SingleConnectionDataSource("jdbc:h2:mem:category-upgrade;MODE=MySQL;DB_CLOSE_DELAY=-1","sa","",true)){verify(source);}
 }
 public static void verify(DataSource source)throws Exception{
  Flyway.configure().dataSource(source).target("16").load().migrate();
  try(var c=source.getConnection();var s=c.createStatement()){
   s.executeUpdate("INSERT INTO events(id,name,description,status,created_at,updated_at,version,meeting_point,entry_code) VALUES(42,'Anterior','Conservar','OPEN',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,7,'Plaza','PING42')");
   s.executeUpdate("INSERT INTO participants(id,alias,event_id,available,created_at,points) VALUES(1,'Uno',42,false,CURRENT_TIMESTAMP,21),(2,'Dos',42,false,CURRENT_TIMESTAMP,34)");
   s.executeUpdate("INSERT INTO participant_interests(participant_id,interest_id) SELECT 1,id FROM interests WHERE name IN ('Videojuegos','Tecnología')");
   s.executeUpdate("INSERT INTO interests(id,name,demo) VALUES(91,'Interés anterior sin categoría',false)");
   s.executeUpdate("INSERT INTO ping_matches(id,event_id,first_id,second_id,status,first_accepted,second_accepted,first_wait,second_wait,common_interests,first_interests,second_interests,meeting_point,word,activity,created_at,reward_points) VALUES(5,42,1,2,'ACTIVE',true,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'Videojuegos','Videojuegos','Videojuegos','Plaza original','PRESERVAR-5','Actividad original',CURRENT_TIMESTAMP,37)");
   Map<Long,String> before=new TreeMap<>();try(var r=s.executeQuery("select id,name from interests")){while(r.next())before.put(r.getLong(1),r.getString(2));}
   var next=Flyway.configure().dataSource(source).load();assertThat(next.migrate().migrationsExecuted).isEqualTo(1);
   Map<Long,String> after=new TreeMap<>();try(var r=s.executeQuery("select id,name from interests")){while(r.next())after.put(r.getLong(1),r.getString(2));}assertThat(after).isEqualTo(before);
   try(var r=s.executeQuery("select count(*) from participant_interests where participant_id=1")){r.next();assertThat(r.getInt(1)).isEqualTo(2);}
   try(var r=s.executeQuery("select category_id from interests where id=91")){r.next();assertThat(r.getObject(1)).isNull();}
   try(var r=s.executeQuery("select category_id from interests where name='Videojuegos'")){r.next();assertThat(r.getLong(1)).isEqualTo(1);}
   try(var r=s.executeQuery("select status,word,activity,reward_points from ping_matches where id=5")){r.next();assertThat(r.getString(1)).isEqualTo("ACTIVE");assertThat(r.getString(2)).isEqualTo("PRESERVAR-5");assertThat(r.getString(3)).isEqualTo("Actividad original");assertThat(r.getInt(4)).isEqualTo(37);}
   try(var r=s.executeQuery("select points from participants order by id")){r.next();assertThat(r.getLong(1)).isEqualTo(21);r.next();assertThat(r.getLong(1)).isEqualTo(34);}
   assertThat(next.migrate().migrationsExecuted).isZero();System.out.println(c.getMetaData().getDatabaseProductName()+" "+c.getMetaData().getDatabaseProductVersion()+" V16->V17: IDs/names, previous selections, uncategorized interest, ACTIVE snapshot and balances preserved; rerun0.");
  }
 }
}
