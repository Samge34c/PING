package com.ping.auth;
import java.util.*;
import java.time.*;
import java.sql.Timestamp;
import java.security.*;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.*;
import com.ping.web.BusinessRuleException;
@Service @Profile("!bootstrap") @Transactional(readOnly=true)
public class AccountService implements UserDetailsService {
 private final JdbcTemplate jdbc;private final PasswordEncoder encoder;private final PrivateMail mail;
 public AccountService(JdbcTemplate j,PasswordEncoder p,PrivateMail m){jdbc=j;encoder=p;mail=m;}
 public static String email(String value){String e=value==null?"":value.strip().toLowerCase(Locale.ROOT);if(e.length()>254||!e.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))throw new BusinessRuleException("Escribe un correo válido.");return e;}
 public static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 public static String secret(){byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
 public static void password(String p){if(p==null||p.length()<12||p.length()>128)throw new BusinessRuleException("Usa una contraseña de12 a128 caracteres.");}
 public Optional<Long> find(String e){return jdbc.query("select id from ping_accounts where email=?",(r,n)->r.getLong(1),e).stream().findFirst();}
 public PingUser user(long id){return jdbc.queryForObject("select a.*,exists(select 1 from event_administrators e where e.account_id=a.id) organizer from ping_accounts a where a.id=?",(r,n)->new PingUser(r.getLong("id"),r.getString("email"),r.getString("password_hash"),r.getBoolean("email_verified"),r.getBoolean("general_admin"),r.getBoolean("organizer"),r.getLong("auth_epoch")),id);}
 @Override public UserDetails loadUserByUsername(String name){try{return user(find(email(name)).orElseThrow(()->new UsernameNotFoundException("Acceso no válido")));}catch(BusinessRuleException ex){throw new UsernameNotFoundException("Acceso no válido");}}
 private long create(String email,String password,boolean verified){jdbc.update("insert into ping_accounts(email,password_hash,email_verified) values(?,?,?)",email,encoder.encode(password),verified);return find(email).orElseThrow();}
 private void token(long id,String purpose){String value=secret();jdbc.update("update account_tokens set consumed=true where account_id=? and purpose=? and consumed=false",id,purpose);jdbc.update("insert into account_tokens(token_hash,account_id,purpose,expires_at) values(?,?,?,?)",hash(value),id,purpose,Timestamp.from(Instant.now().plusSeconds(purpose.equals("RESET")?1800:86400)));mail.send(user(id).email(),purpose,value);}
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public void register(String value,String password){String e=email(value);password(password);if(!mail.available())throw new BusinessRuleException("El registro por correo requiere que la organización configure el envío.");var old=find(e);if(old.isPresent()){if(!user(old.get()).verified())token(old.get(),"VERIFY");return;}try{long id=create(e,password,false);token(id,"VERIFY");audit(id,"REGISTER",id,null);}catch(org.springframework.dao.DuplicateKeyException race){/* Same public response; retry through recovery. */}}
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public void recover(String value){String e=email(value);var id=find(e);if(id.isPresent()&&mail.available())token(id.get(),user(id.get()).verified()?"RESET":"VERIFY");}
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public void consume(String value,String purpose,String password){
  if(value==null||!value.matches("[A-Za-z0-9_-]{43}"))throw new BusinessRuleException("El enlace no es válido o ha caducado. Solicita otro.");
  var rows=jdbc.queryForList("select * from account_tokens where token_hash=? for update",hash(value));
  if(rows.isEmpty()||!purpose.equals(rows.getFirst().get("purpose"))||Boolean.TRUE.equals(rows.getFirst().get("consumed"))||((Timestamp)rows.getFirst().get("expires_at")).toInstant().isBefore(Instant.now()))throw new BusinessRuleException("El enlace no es válido o ha caducado. Solicita otro.");
  long id=((Number)rows.getFirst().get("account_id")).longValue();if(purpose.equals("RESET")){password(password);jdbc.update("update ping_accounts set password_hash=?,auth_epoch=auth_epoch+1 where id=?",encoder.encode(password),id);}else jdbc.update("update ping_accounts set email_verified=true where id=?",id);
  jdbc.update("update account_tokens set consumed=true where account_id=? and purpose=?",id,purpose);audit(id,purpose,id,null);
 }
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public void bootstrap(String value,String password){if(value.isBlank()&&password.isBlank())return;if(value.isBlank()||password.isBlank())throw new IllegalStateException("Both PING_BOOTSTRAP_EMAIL and PING_BOOTSTRAP_PASSWORD are required for first setup");boolean used=Boolean.TRUE.equals(jdbc.queryForObject("select used from auth_setup_state where id=1 for update",Boolean.class));if(used)return;String e=email(value);password(password);if(find(e).isPresent())throw new IllegalStateException("Bootstrap requires a new dedicated account; no existing account is promoted");long id=create(e,password,true);jdbc.update("update ping_accounts set general_admin=true where id=?",id);jdbc.update("update auth_setup_state set used=true where id=1");audit(id,"BOOTSTRAP_GENERAL",id,null);}
 public List<Long> managed(long account){return jdbc.query("select event_id from event_administrators where account_id=?",(r,n)->r.getLong(1),account);}
 public record Participation(long eventId,long participantId,String eventName,String alias){}
 public List<Participation> participations(long account){return jdbc.query("select x.event_id,x.participant_id,e.name,p.alias from account_participations x join events e on e.id=x.event_id join participants p on p.id=x.participant_id where x.account_id=? order by x.participant_id desc",(r,n)->new Participation(r.getLong(1),r.getLong(2),r.getString(3),r.getString(4)),account);}
 @Transactional(propagation=Propagation.MANDATORY) public void lock(long id){jdbc.queryForObject("select id from ping_accounts where id=? for update",Long.class,id);}
 public Optional<Long> participant(long account,long event){return jdbc.query("select participant_id from account_participations where account_id=? and event_id=?",(r,n)->r.getLong(1),account,event).stream().findFirst();}
 public boolean unlinked(long participant){return jdbc.queryForObject("select count(*) from account_participations where participant_id=?",Integer.class,participant)==0;}
 public boolean owns(long account,long participant){return jdbc.queryForObject("select count(*) from account_participations where account_id=? and participant_id=?",Integer.class,account,participant)>0;}
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public void bind(long account,long participant,long event){
  jdbc.queryForObject("select id from ping_accounts where id=? for update",Long.class,account);
  if(owns(account,participant))return;
  if(jdbc.queryForObject("select count(*) from participants where id=? and event_id=?",Integer.class,participant,event)==0)throw new BusinessRuleException("La participación no pertenece a este evento.");
  if(jdbc.queryForObject("select count(*) from account_participations where participant_id=? or(account_id=? and event_id=?)",Integer.class,participant,account,event)>0)throw new BusinessRuleException("La cuenta o participación ya está vinculada. No se fusionaron identidades ni puntos.");
  jdbc.update("insert into account_participations(account_id,event_id,participant_id) values(?,?,?)",account,event,participant);audit(account,"BIND_PARTICIPATION",participant,event);
 }
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public void assign(long actor,long target,long event,boolean grant){if(!user(actor).general())throw new org.springframework.security.access.AccessDeniedException("Solo administración general");jdbc.queryForObject("select id from ping_accounts where id=? for update",Long.class,target);jdbc.queryForObject("select id from events where id=? for update",Long.class,event);if(grant){if(!managed(target).contains(event))jdbc.update("insert into event_administrators(account_id,event_id) values(?,?)",target,event);}else jdbc.update("delete from event_administrators where account_id=? and event_id=?",target,event);audit(actor,grant?"GRANT_EVENT_ADMIN":"REVOKE_EVENT_ADMIN",target,event);}
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public long external(String provider,String subject,String claimedEmail,boolean verified,Long linking){
  if(!Set.of("google","facebook").contains(provider)||subject==null||subject.length()>255)throw new BusinessRuleException("No pudimos comprobar la identidad del proveedor.");
  var old=jdbc.query("select account_id from external_identities where provider=? and subject=? for update",(r,n)->r.getLong(1),provider,subject);
  if(!old.isEmpty()){if(linking!=null&&old.getFirst().longValue()!=linking)throw new BusinessRuleException("Esta identidad ya está vinculada a otra cuenta.");return old.getFirst();}
  long id;if(linking!=null){id=linking;if(!user(id).verified())throw new BusinessRuleException("Verifica primero tu correo.");}else{String e=email(claimedEmail);if(find(e).isPresent())throw new BusinessRuleException("Entra con tu cuenta y vincula el proveedor desde Mi cuenta.");if(!verified&&!mail.available())throw new BusinessRuleException("Este proveedor requiere verificar el correo; el envío aún no está configurado.");id=create(e,secret(),verified);if(!verified)token(id,"VERIFY");}
  jdbc.update("insert into external_identities(provider,subject,account_id) values(?,?,?)",provider,subject,id);audit(id,"LINK_"+provider.toUpperCase(Locale.ROOT),id,null);return id;
 }
 @Transactional
 public void audit(long actor,String action,Long subject,Long event){jdbc.update("insert into auth_audit(actor_id,action,subject_id,event_id) values(?,?,?,?)",actor,action,subject,event);}
}
