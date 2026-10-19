package com.ping.auth;
import java.time.*;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.*;
import com.ping.web.BusinessRuleException;
@Service @Profile("!bootstrap")
public class AuthThrottle {
 private final JdbcTemplate jdbc;
 public AuthThrottle(JdbcTemplate j){jdbc=j;}
 @Transactional(propagation=Propagation.REQUIRES_NEW,isolation=Isolation.READ_COMMITTED)
 public void check(String scope,String identity,int max,int minutes){
  String key=AccountService.hash(scope+":"+identity);Instant now=Instant.now();
  try{jdbc.update("insert into auth_throttle(bucket_key,hits,expires_at) select ?,0,? where not exists(select 1 from auth_throttle where bucket_key=?)",key,Timestamp.from(now.plusSeconds(minutes*60L)),key);}catch(org.springframework.dao.DuplicateKeyException concurrent){}
  var row=jdbc.queryForMap("select hits,expires_at from auth_throttle where bucket_key=? for update",key);
  boolean expired=((Timestamp)row.get("expires_at")).toInstant().isBefore(now);int hits=expired?0:((Number)row.get("hits")).intValue();
  if(hits>=max)throw new BusinessRuleException("Demasiados intentos. Espera "+minutes+" minutos y vuelve a intentarlo.");
  jdbc.update("update auth_throttle set hits=?,expires_at=? where bucket_key=?",hits+1,expired?Timestamp.from(now.plusSeconds(minutes*60L)):row.get("expires_at"),key);
 }
}
