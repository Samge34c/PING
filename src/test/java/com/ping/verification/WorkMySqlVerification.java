package com.ping.verification;
import java.sql.*;
import org.flywaydb.core.Flyway;
import org.springframework.boot.SpringApplication;
import com.ping.PingApplication;
import com.ping.match.*;
import com.ping.participant.ParticipantService;
/** Optional destructive-free upgrade check: refuses any nonempty database; fixtures are explicitly fictitious. */
public class WorkMySqlVerification {
 static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
 public static void main(String[] args)throws Exception{
  String url=System.getenv("DB_URL"),user=System.getenv("DB_USERNAME"),password=System.getenv("DB_PASSWORD");
  if(url==null||!url.contains("exclusive"))throw new IllegalArgumentException("Use a NEW exclusive database, never demo or usage data.");
  try(var c=DriverManager.getConnection(url,user,password);var s=c.createStatement();var r=s.executeQuery("select count(*) from information_schema.tables where table_schema=database()")){r.next();require(c.getCatalog().contains("exclusive")&&r.getInt(1)==0,"Database must be new and empty; no clean/drop is performed.");}
  Flyway.configure().dataSource(url,user,password).target("20").load().migrate();
  try(var c=DriverManager.getConnection(url,user,password);var s=c.createStatement()){
   s.executeUpdate("insert into events(id,name,description,status,created_at,updated_at,entry_code,encounter_words) values(501,'Fixture anterior ficticia','Prueba exclusiva','PAUSED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'WORKLEGACY501','')");
   s.executeUpdate("insert into participants(id,alias,event_id,available,created_at,points) values(501,'Anterior A',501,false,CURRENT_TIMESTAMP,30),(502,'Anterior B',501,false,CURRENT_TIMESTAMP,20),(503,'Saga A',501,false,CURRENT_TIMESTAMP,30),(504,'Saga B',501,false,CURRENT_TIMESTAMP,20)");
   s.executeUpdate("insert into ping_matches(id,event_id,first_id,second_id,status,first_accepted,second_accepted,first_confirmed,second_confirmed,first_wait,second_wait,common_interests,first_interests,second_interests,meeting_point,word,activity,created_at,completion_pending,reward_points) values(501,501,501,502,'ACTIVE',true,true,true,false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'','','','Plaza anterior','ANTERIOR-FICTICIO-501','Actividad anterior',CURRENT_TIMESTAMP,false,37),(502,501,503,504,'ACTIVE',true,true,true,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'','','','Lugar anterior','ANTERIOR-FICTICIO-502','Actividad Saga anterior',CURRENT_TIMESTAMP,true,22)");
   s.executeUpdate("insert into match_reservations(participant_id,match_id) values(501,501),(502,501),(503,502),(504,502)");
   s.executeUpdate("insert into match_completion_sagas(match_id,status,attempt,trigger_participant_id,first_granted,second_granted,reward_points) values(502,'FIRST_REWARD_GRANTED',1,504,true,false,22)");
   s.executeUpdate("insert into reward_claims(participant_id,benefit_id,request_key,cost,status,created_at,benefit_name,claim_location,claim_instructions) values(501,1,'legacy-paid-fixture',20,'REQUESTED',CURRENT_TIMESTAMP,'Beneficio anterior','Mesa anterior','Indicaciones anteriores')");
  }
  var current=Flyway.configure().dataSource(url,user,password).load();require(current.migrate().migrationsExecuted==1,"Expected only V21");require(current.migrate().migrationsExecuted==0,"Migration rerun must be zero");
  try(var c=DriverManager.getConnection(url,user,password);var s=c.createStatement();var r=s.executeQuery("select verification_policy,first_arrived,second_arrived,first_verified,second_verified,first_confirmed,word,reward_points from ping_matches where id=501")){r.next();require(r.getInt(1)==1&&!r.getBoolean(2)&&!r.getBoolean(3)&&!r.getBoolean(4)&&!r.getBoolean(5)&&r.getBoolean(6)&&r.getString(7).equals("ANTERIOR-FICTICIO-501")&&r.getInt(8)==37,"Historical policy/data changed");}
  try(var context=SpringApplication.run(PingApplication.class,"--spring.profiles.active=mysql","--server.port=0")){
   var matches=context.getBean(MatchService.class);var people=context.getBean(ParticipantService.class);
   require(matches.get(502,503).status()==MatchStatus.COMPLETED,"Pending Saga must recover");require(people.get(503).points()==30&&people.get(504).points()==42,"Pending Saga amount changed or duplicated");
   matches.confirm(501,502);matches.confirm(501,502);require(people.get(501).points()==67&&people.get(502).points()==57,"Legacy optional completion/replay changed amount");
   var jdbc=context.getBean(org.springframework.jdbc.core.JdbcTemplate.class);require(jdbc.queryForObject("select receipt_policy from reward_claims where request_key='legacy-paid-fixture'",Integer.class)==1,"Old receipt policy changed");require(jdbc.queryForObject("select claim_location from reward_claims where request_key='legacy-paid-fixture'",String.class).equals("Mesa anterior"),"Old delivery snapshot changed");
   System.out.println("MYSQL UPGRADE OK: V20->21/rerun0, historical ACTIVE policy1/no invented arrivals/verifications, reward37/replay, Saga22 recovered, old paid receipt/place preserved.");
  }
 }
}
