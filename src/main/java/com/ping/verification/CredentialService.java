package com.ping.verification;
import java.util.*;
import java.time.*;
import java.sql.Timestamp;
import java.security.SecureRandom;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.*;
import com.ping.auth.*;
import com.ping.event.EventService;
import com.ping.match.*;
import com.ping.participant.ParticipantRepository;
import com.ping.reward.*;
import com.ping.web.*;
@Service @Profile("!bootstrap") @Transactional(readOnly=true)
public class CredentialService {
 private final JdbcTemplate jdbc;private final EventService events;private final MatchRepository matches;private final ParticipantRepository people;private final RewardClaimRepository claims;private final AuthThrottle throttle;private final AuthSupport auth;private final AccountService accounts;
 public CredentialService(JdbcTemplate j,EventService e,MatchRepository m,ParticipantRepository p,RewardClaimRepository c,AuthThrottle t,AuthSupport a,AccountService accounts){jdbc=j;events=e;matches=m;people=p;claims=c;throttle=t;auth=a;this.accounts=accounts;}
 public record Issued(String id,String issueKey,String purpose,long context,long issuer,String manual,String token,Instant expiresAt) implements java.io.Serializable{}
 private BusinessRuleException invalid(){return new BusinessRuleException("El código no es válido para esta acción, caducó o fue sustituido. Pide uno nuevo y reintenta.");}
 private Match lockedMatch(long id,long self){events.lock(matches.eventId(id).orElseThrow(this::invalid));var m=matches.findById(id).orElseThrow(this::invalid);if(!m.owns(self))throw invalid();return m;}
 private RewardClaim lockedClaim(long id){events.lock(claims.eventId(id).orElseThrow(this::invalid));return claims.findById(id).orElseThrow(this::invalid);}
 private long other(Match m,long self){return m.getFirst().getId()==self?m.getSecond().getId():m.getFirst().getId();}
 public boolean valid(Issued value){return value!=null&&value.expiresAt().isAfter(Instant.now())&&jdbc.queryForObject("select count(*) from purpose_credentials where id=? and revoked=false and consumed=false and expires_at>?",Integer.class,value.id(),Timestamp.from(Instant.now()))>0;}
 private Issued issue(String purpose,long context,long event,long issuer,Long recipient,String key,int minutes){
  if(key==null||!key.matches("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"))throw invalid();
  if(jdbc.queryForObject("select count(*) from purpose_credentials where issue_key=?",Integer.class,key)>0)throw new BusinessRuleException("Esta emisión ya se procesó. Revisa tu código o renueva explícitamente.");
  jdbc.update("update purpose_credentials set revoked=true where purpose=? and context_id=? and issuer_id=? and consumed=false",purpose,context,issuer);
  String alphabet="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";var random=new SecureRandom();var suffix=new StringBuilder();for(int i=0;i<20;i++)suffix.append(alphabet.charAt(random.nextInt(alphabet.length())));
  String manual=(purpose.equals("ENCOUNTER")?people.findById(issuer).orElseThrow().getAvatar().animal().toUpperCase(Locale.ROOT):purpose.equals("ACTIVITY")?"ACTIVIDAD":"CANJE")+"-"+suffix;
  String token=AccountService.secret(),id=UUID.randomUUID().toString();Instant expiry=Instant.now().plusSeconds(minutes*60L);
  jdbc.update("insert into purpose_credentials(id,issue_key,purpose,context_id,event_id,issuer_id,recipient_id,token_hash,manual_hash,expires_at) values(?,?,?,?,?,?,?,?,?,?)",id,key,purpose,context,event,issuer,recipient,AccountService.hash(token),AccountService.hash(manual),Timestamp.from(expiry));
  return new Issued(id,key,purpose,context,issuer,manual,token,expiry);
 }
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public Issued encounter(long match,long self,String key){var m=lockedMatch(match,self);if(m.getVerificationPolicy()<2||m.getStatus()!=MatchStatus.ACTIVE||!m.bothArrived()||m.verified(other(m,self))||m.encounterVerified())throw new BusinessRuleException(m.getVerificationPolicy()>=3?"Primero deben llegar ambos. Si el encuentro ya está verificado, no necesitas otro código.":"Primero deben llegar ambos. Si la otra persona ya te verificó, no necesitas otro código.");throttle.check("issue-encounter",self+":"+match,10,10);return issue("ENCOUNTER",match,m.getEvent().getId(),self,other(m,self),key,10);}
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public Issued redemption(long claim,long self,String key){var c=lockedClaim(claim);if(c.getParticipant().getId()!=self||c.getStatus()!=ClaimStatus.REQUESTED)throw invalid();throttle.check("issue-redemption",self+":"+claim,10,10);return issue("REDEMPTION",claim,c.getParticipant().getEvent().getId(),self,null,key,1440);}
 private Map<String,Object> credential(String purpose,String submitted){
  String input=submitted==null?"":submitted.strip();if(input.length()>160)throw invalid();String digest,column;
  if(input.startsWith("PING:")){String prefix="PING:"+purpose+":";if(!input.startsWith(prefix)||!input.substring(prefix.length()).matches("[A-Za-z0-9_-]{43}"))throw invalid();digest=AccountService.hash(input.substring(prefix.length()));column="token_hash";}
  else{input=input.replaceAll("\\s+","").toUpperCase(Locale.ROOT);if(!input.matches("[A-Z]{3,9}-[A-Z2-9]{20}"))throw invalid();digest=AccountService.hash(input);column="manual_hash";}
  var rows=jdbc.queryForList("select * from purpose_credentials where "+column+"=? and purpose=?",digest,purpose);if(rows.isEmpty())throw invalid();return rows.getFirst();
 }
 private long number(Map<String,Object> row,String key){return ((Number)row.get(key)).longValue();}
 private boolean flag(Map<String,Object> row,String key){return Boolean.TRUE.equals(row.get(key));}
 private void pending(Map<String,Object> row){if(flag(row,"revoked")||((Timestamp)row.get("expires_at")).toInstant().isBefore(Instant.now()))throw invalid();}
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public void verify(long match,long self,String submitted,String ip){
  throttle.check("verify-account",self+":"+match,8,5);throttle.check("verify-ip",ip,40,5);var m=lockedMatch(match,self);var row=credential("ENCOUNTER",submitted);
  if(number(row,"context_id")!=match||number(row,"issuer_id")!=other(m,self)||row.get("recipient_id")==null||number(row,"recipient_id")!=self||number(row,"event_id")!=m.getEvent().getId())throw invalid();
  if(flag(row,"consumed")&&m.verified(self)&&(m.getStatus()==MatchStatus.ACTIVE||m.getStatus()==MatchStatus.COMPLETED))return;
  // DEC-073 · policy 3: once one real scan verified the encounter, a repeated or simultaneous scan by this member gets the same
  // result (already verified) with no further effect. Checked after identity/context, so a third party never reaches it.
  if(m.getVerificationPolicy()>=3&&m.encounterVerified()&&(m.getStatus()==MatchStatus.ACTIVE||m.getStatus()==MatchStatus.COMPLETED))return;
  if(m.getVerificationPolicy()<2||m.getStatus()!=MatchStatus.ACTIVE||!m.bothArrived())throw new BusinessRuleException("Ambos deben registrar su llegada antes de verificar.");pending(row);m.verifiedBy(self);jdbc.update("update purpose_credentials set consumed=true where id=?",row.get("id"));
  // Policy 3: the encounter is verified now, so no other pending code of this encounter may be used or shown again.
  if(m.getVerificationPolicy()>=3)jdbc.update("update purpose_credentials set revoked=true where purpose='ENCOUNTER' and context_id=? and consumed=false",match);
 }
 // ---- DEC-081 · Activity QR: shown by a participant after «Completar actividad», scanned by authorised staff of the event. ----
 private static final int ACTIVITY_MINUTES=20;
 /** Issues (or replaces) this participant's activity QR. Only for a QR-validated encounter this person already completed. */
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public Issued activity(long match,long self,String key){var m=lockedMatch(match,self);
  if(!m.qrValidation()||m.getStatus()!=MatchStatus.ACTIVE||m.isCompletionPending())throw new BusinessRuleException("Esta actividad no usa validación con QR o ya se está acreditando.");
  if(!m.confirmed(self))throw new BusinessRuleException("Primero pulsa «Completar actividad».");
  throttle.check("issue-activity",self+":"+match,10,10);return issue("ACTIVITY",match,m.getEvent().getId(),self,null,key,ACTIVITY_MINUTES);}
 public enum ScanResult{CREDITED,WAITING,ALREADY}
 /** missing: aliases that still have to press «Completar actividad» (only for WAITING). */
 public record ActivityScan(ScanResult result,long match,String first,String second,int points,List<String> missing){}
 /** A scanned code of another purpose gets a precise answer; anything else is the generic invalid code. */
 private Map<String,Object> activityCredential(String submitted){
  String input=submitted==null?"":submitted.strip(),upper=input.replaceAll("\\s+","").toUpperCase(Locale.ROOT);
  if(input.startsWith("PING:REDEMPTION:")||upper.matches("CANJE-[A-Z2-9]{20}"))throw new BusinessRuleException("Ese es el QR de un recibo de canje. Para validar la actividad, escanea el QR que aparece al pulsar «Completar actividad».");
  if(input.startsWith("PING:ENCOUNTER:")||(!upper.startsWith("ACTIVIDAD-")&&upper.matches("[A-Z]{3,8}-[A-Z2-9]{20}")))throw new BusinessRuleException("Ese es el QR personal del encuentro, no el de la actividad. Pide el QR que aparece al pulsar «Completar actividad».");
  return credential("ACTIVITY",submitted);
 }
 /**
  * One staff scan per encounter. With both confirmations it registers the validation (unique per encounter) and the caller
  * starts the existing Saga; before that it only says who is missing and leaves the code usable. Never grants points itself.
  */
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public ActivityScan validateActivity(String submitted,String ip){
  long staffId=auth.require().id();throttle.check("activity-scan-account",Long.toString(staffId),30,5);throttle.check("activity-scan-ip",ip,60,5);
  var found=activityCredential(submitted);long matchId=number(found,"context_id"),event=number(found,"event_id");
  events.lock(event);var m=matches.findById(matchId).orElseThrow(this::invalid);if(m.getEvent().getId()!=event||!m.qrValidation())throw invalid();
  auth.manage(event);
  if(accounts.owns(staffId,m.getFirst().getId())||accounts.owns(staffId,m.getSecond().getId()))throw new org.springframework.security.access.AccessDeniedException("Participas en este encuentro: otra persona autorizada del evento debe validar la actividad.");
  long first=m.getFirst().getId(),second=m.getSecond().getId();String a=m.getFirst().getAlias(),b=m.getSecond().getAlias();
  boolean validated=jdbc.queryForObject("select count(*) from activity_qr_validations where match_id=?",Integer.class,matchId)>0;
  if(validated||m.getStatus()==MatchStatus.COMPLETED)return new ActivityScan(ScanResult.ALREADY,matchId,a,b,m.getRewardPoints(),List.of());
  var row=jdbc.queryForMap("select * from purpose_credentials where id=? for update",found.get("id"));pending(row);
  if(m.getStatus()!=MatchStatus.ACTIVE)throw new BusinessRuleException("Este encuentro ya no está activo. No se acreditaron puntos.");
  var missing=new ArrayList<String>();if(!m.confirmed(first))missing.add(a);if(!m.confirmed(second))missing.add(b);
  if(!missing.isEmpty())return new ActivityScan(ScanResult.WAITING,matchId,a,b,m.getRewardPoints(),List.copyOf(missing));
  jdbc.update("insert into activity_qr_validations(match_id,event_id,validated_by,validated_at) values(?,?,?,?)",matchId,event,staffId,Timestamp.from(Instant.now()));
  jdbc.update("update purpose_credentials set consumed=true where id=?",row.get("id"));
  jdbc.update("update purpose_credentials set revoked=true where purpose='ACTIVITY' and context_id=? and consumed=false",matchId);
  accounts.audit(staffId,"VALIDATE_ACTIVITY_QR",matchId,event);
  return new ActivityScan(ScanResult.CREDITED,matchId,a,b,m.getRewardPoints(),List.of());
 }
 /** Organisation summary: QR encounters with both confirmations that still wait for a scan. */
 public long awaitingActivityScan(long event){return jdbc.queryForObject("select count(*) from ping_matches m where m.event_id=? and m.status='ACTIVE' and m.activity_check='QR_VALIDATION' and m.first_confirmed=true and m.second_confirmed=true and not exists(select 1 from activity_qr_validations v where v.match_id=m.id)",Long.class,event);}
 public record Delivery(long claim,String benefit,long cost,String status,String location,String directions,String alias,java.util.List<EvidenceLedger.Requirement> evidence,EvidenceLedger.Note note){
  /** DEC-068: the receipt used points still to check; delivery stays blocked until every linked encounter is validated. */
  public boolean evidenceMissing(){return evidence.stream().anyMatch(r->!r.validated());}
 }
 private EvidenceLedger ledger;
 @org.springframework.beans.factory.annotation.Autowired void ledger(EvidenceLedger ledger){this.ledger=ledger;}
 private Delivery delivery(RewardClaim c){return new Delivery(c.getId(),c.getBenefitName(),c.getCost(),c.getStatus().name(),c.getClaimLocation(),c.getClaimInstructions(),c.getParticipant().getAlias(),ledger.requirements(c.getId()),ledger.lastNote(c.getId()).orElse(null));}
 private void staff(RewardClaim claim){long event=claim.getParticipant().getEvent().getId();auth.manage(event);if(accounts.owns(auth.require().id(),claim.getParticipant().getId()))throw new org.springframework.security.access.AccessDeniedException("Otra persona autorizada debe registrar la entrega.");}
 public Delivery lookup(String submitted,String ip){throttle.check("delivery-account",Long.toString(auth.require().id()),30,5);throttle.check("delivery-ip",ip,60,5);var row=credential("REDEMPTION",submitted);var c=claims.findById(number(row,"context_id")).orElseThrow(this::invalid);staff(c);if(c.getStatus()==ClaimStatus.REQUESTED)pending(row);return delivery(c);}
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public long deliver(String submitted,ClaimStatus target,String ip){
  throttle.check("delivery-account",Long.toString(auth.require().id()),30,5);var row=credential("REDEMPTION",submitted);var c=lockedClaim(number(row,"context_id"));row=jdbc.queryForMap("select * from purpose_credentials where id=? for update",row.get("id"));staff(c);
  if(target==null||target==ClaimStatus.REQUESTED)throw invalid();if(flag(row,"consumed")&&c.getStatus()==target)return c.getId();
  if(c.getStatus()!=ClaimStatus.REQUESTED)throw new BusinessRuleException("Este recibo ya tiene un resultado registrado. No se modificaron puntos.");pending(row);
  if(target==ClaimStatus.DELIVERED&&ledger.missing(c.getId()))throw new BusinessRuleException("Comprueba primero la evidencia de la actividad. No se registró la entrega ni se cobraron puntos.");
  c.finishVerified(target);jdbc.update("update purpose_credentials set consumed=true where id=?",row.get("id"));accounts.audit(auth.require().id(),target==ClaimStatus.DELIVERED?"DELIVER_CLAIM":"INCIDENT_CLAIM",c.getId(),c.getParticipant().getEvent().getId());return c.getId();
 }
 /** DEC-068: the staff member checked the evidence in person. One validation covers both participants of that encounter; it grants no points. */
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public boolean validateEvidence(String submitted,long match,String ip){
  throttle.check("delivery-account",Long.toString(auth.require().id()),30,5);var row=credential("REDEMPTION",submitted);var c=lockedClaim(number(row,"context_id"));staff(c);
  if(c.getStatus()!=ClaimStatus.REQUESTED)throw new BusinessRuleException("Este recibo ya tiene un resultado registrado.");pending(row);
  var m=matches.findById(match).orElseThrow(this::invalid);long staffId=auth.require().id();
  if(accounts.owns(staffId,m.getFirst().getId())||accounts.owns(staffId,m.getSecond().getId()))throw new org.springframework.security.access.AccessDeniedException("Otra persona autorizada debe comprobar la evidencia de un encuentro tuyo.");
  boolean created=ledger.validate(c.getId(),match,c.getParticipant().getEvent().getId(),staffId);if(created)accounts.audit(staffId,"VALIDATE_EVIDENCE",match,c.getParticipant().getEvent().getId());return created;
 }
 /** DEC-068: evidence missing or not matching. The receipt stays pending: nothing is delivered and nothing is charged again. */
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public void evidenceMissing(String submitted,String note,String ip){
  throttle.check("delivery-account",Long.toString(auth.require().id()),30,5);var row=credential("REDEMPTION",submitted);var c=lockedClaim(number(row,"context_id"));staff(c);
  if(c.getStatus()!=ClaimStatus.REQUESTED)throw new BusinessRuleException("Este recibo ya tiene un resultado registrado.");pending(row);
  if(!ledger.missing(c.getId()))throw new BusinessRuleException("Este recibo no tiene evidencia pendiente.");
  String text=note==null?"":note.strip();if(text.isEmpty())text="Vuelve con la foto de la actividad en tu celular.";if(text.length()>300)throw new BusinessRuleException("La indicación admite hasta 300 caracteres.");
  long staffId=auth.require().id();ledger.note(c.getId(),staffId,text);accounts.audit(staffId,"EVIDENCE_MISSING",c.getId(),c.getParticipant().getEvent().getId());
 }
 public RewardService.ClaimView personal(long claim,long self){return claims.findByParticipantIdOrderByIdDesc(self).stream().filter(c->c.getId()==claim).findFirst().map(c->new RewardService.ClaimView(c.getId(),c.getParticipant().getAlias(),c.getBenefitName(),c.getCost(),c.getStatus(),c.getCreatedAt(),c.getBenefitDescription(),c.getClaimLocation(),c.getClaimInstructions())).orElseThrow(this::invalid);}
}
