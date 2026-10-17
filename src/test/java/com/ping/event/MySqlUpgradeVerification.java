package com.ping.event;
import java.sql.*;
import org.flywaydb.core.Flyway;
public class MySqlUpgradeVerification {
 static void require(boolean b,String message){if(!b)throw new IllegalStateException(message);}
 public static void main(String[] args)throws Exception{
  String url=System.getenv("DB_URL"),password=System.getenv("DB_PASSWORD");
  if(url==null || !url.contains("exclusive"))throw new IllegalArgumentException("Usa una base NUEVA exclusiva cuyo nombre incluya exclusive; nunca la demo.");
  var old=Flyway.configure().dataSource(url,"root",password).target("13").load();old.migrate();
  try(var c=DriverManager.getConnection(url,"root",password);var s=c.createStatement()){
   s.executeUpdate("INSERT INTO events(id,name,description,status,created_at,updated_at,version,meeting_point,entry_code) VALUES(42,'Anterior','Conservar','PAUSED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,7,'Plaza anterior','PING42')");
   s.executeUpdate("INSERT INTO participants(id,alias,event_id,available,created_at,points) VALUES(1,'Uno',42,false,CURRENT_TIMESTAMP,30),(2,'Dos',42,false,CURRENT_TIMESTAMP,20)");
   s.executeUpdate("INSERT INTO ping_matches(id,event_id,first_id,second_id,status,first_accepted,second_accepted,first_confirmed,first_wait,second_wait,common_interests,first_interests,second_interests,meeting_point,word,activity,created_at,completion_pending) VALUES(5,42,1,2,'ACTIVE',true,true,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'','','','Plaza guardada','ANTES-5','Actividad guardada',CURRENT_TIMESTAMP,false),(6,42,1,2,'ACTIVE',true,true,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'','','','Lugar pendiente','ANTES-6','Actividad pendiente',CURRENT_TIMESTAMP,true)");
   s.executeUpdate("INSERT INTO match_completion_sagas(match_id,status,attempt,trigger_participant_id,first_granted,second_granted) VALUES(6,'FIRST_REWARD_GRANTED',1,2,true,false)");
   var f=Flyway.configure().dataSource(url,"root",password).target("14").load();require(f.migrate().migrationsExecuted==1,"V14 count");
   try(var r=s.executeQuery("SELECT arrival_required,first_arrived,second_arrived,first_confirmed,word FROM ping_matches WHERE id=5")){r.next();require(!r.getBoolean(1)&&!r.getBoolean(2)&&!r.getBoolean(3)&&r.getBoolean(4)&&r.getString(5).equals("ANTES-5"),"V14 match preservation");}
   s.executeUpdate("UPDATE events SET meeting_points='Plaza\\nEntrada',encounter_activities='Conversen.',encounter_words='LUNA' WHERE id=42");
   f=Flyway.configure().dataSource(url,"root",password).load();f.migrate();require(f.migrate().migrationsExecuted==0,"idempotent");
   try(var r=s.executeQuery("SELECT name,version,meeting_point,meeting_points,structured_encounter FROM events WHERE id=42")){r.next();require(r.getString(1).equals("Anterior")&&r.getLong(2)==7&&r.getString(3).equals("Plaza anterior")&&r.getString(4).contains("Entrada")&&!r.getBoolean(5),"event preservation");}
   try(var r=s.executeQuery("SELECT points FROM participants ORDER BY id")){r.next();require(r.getLong(1)==30,"balance1");r.next();require(r.getLong(1)==20,"balance2");}
   try(var r=s.executeQuery("SELECT status,first_granted FROM match_completion_sagas WHERE match_id=6")){r.next();require(r.getString(1).equals("FIRST_REWARD_GRANTED")&&r.getBoolean(2),"pending receipt");}
   System.out.println("MYSQL "+c.getMetaData().getDatabaseProductVersion()+" Flyway13->14->15: preserved event, lists, ACTIVE/partial confirmation, pending Saga/credit receipts, balances; rerun0.");
  }
 }
}
