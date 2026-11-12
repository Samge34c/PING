package com.ping.verification;
import java.util.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;
import com.ping.match.*;
import com.ping.reward.ClaimStatus;
import com.ping.web.BusinessRuleException;
@Controller @Profile("!bootstrap")
public class CredentialController {
 private final CredentialService codes;private final MatchService matches;private final com.ping.participant.ParticipantService people;
 public CredentialController(CredentialService c,MatchService m,com.ping.participant.ParticipantService people){codes=c;matches=m;this.people=people;}
 @PostMapping("/matches/{id}/code") public String issue(@PathVariable long id,@RequestParam String requestKey,@RequestParam(defaultValue="false") boolean renew,HttpSession s,RedirectAttributes flash){long self=DemoSession.participantId(s);synchronized(s){var old=CredentialSession.get(s,"ENCOUNTER",id,self);if(old==null||!codes.valid(old)||renew&&!old.issueKey().equals(requestKey))CredentialSession.put(s,codes.encounter(id,self,requestKey));}return "redirect:/matches/"+id+"?view=mine#am-step";}
 @PostMapping("/matches/{id}/verify") public String verify(@PathVariable long id,@RequestParam String credential,HttpSession s,HttpServletRequest r,RedirectAttributes flash){try{codes.verify(id,DemoSession.participantId(s),credential,r.getRemoteAddr());}catch(BusinessRuleException e){flash.addFlashAttribute("verificationError",e.getMessage());return "redirect:/matches/"+id+"?view=theirs#am-step";}return "redirect:/matches/"+id+"#am-step";}
 @GetMapping(value="/matches/{id}/my-code.png",produces="image/png") @ResponseBody public ResponseEntity<byte[]> qr(@PathVariable long id,HttpSession s){long self=DemoSession.participantId(s);var m=matches.get(id,self);var code=CredentialSession.get(s,"ENCOUNTER",id,self);if(m.status()!=MatchStatus.ACTIVE||!codes.valid(code))throw new BusinessRuleException("Genera un código personal vigente.");return image(code);}
 private com.ping.reward.EvidenceLedger ledger;
 @org.springframework.beans.factory.annotation.Autowired void ledger(com.ping.reward.EvidenceLedger ledger){this.ledger=ledger;}
 @GetMapping("/me/claims/{id}") public String receipt(@PathVariable long id,HttpSession s,Model m){long self=DemoSession.participantId(s);m.addAttribute("claim",codes.personal(id,self));
  // DEC-068: evidence this receipt requires (only after ownership was checked above).
  m.addAttribute("evidence",ledger.requirements(id));m.addAttribute("evidenceNote",ledger.lastNote(id).orElse(null));m.addAttribute("participant",people.get(self));var code=CredentialSession.get(s,"REDEMPTION",id,self);m.addAttribute("myCredential",codes.valid(code)?code:null);m.addAttribute("requestKey",UUID.randomUUID().toString());return "rewards/receipt";}
 @PostMapping("/me/claims/{id}/code") public String receiptCode(@PathVariable long id,@RequestParam String requestKey,@RequestParam(defaultValue="false") boolean renew,HttpSession s){long self=DemoSession.participantId(s);synchronized(s){var old=CredentialSession.get(s,"REDEMPTION",id,self);if(old==null||!codes.valid(old)||renew&&!old.issueKey().equals(requestKey))CredentialSession.put(s,codes.redemption(id,self,requestKey));}return "redirect:/me/claims/"+id;}
 @GetMapping(value="/me/claims/{id}/code.png",produces="image/png") @ResponseBody public ResponseEntity<byte[]> receiptQr(@PathVariable long id,HttpSession s){long self=DemoSession.participantId(s);var c=codes.personal(id,self);var code=CredentialSession.get(s,"REDEMPTION",id,self);if(c.status()!=ClaimStatus.REQUESTED||!codes.valid(code))throw new BusinessRuleException("El recibo ya tiene resultado o requiere renovar su código.");return image(code);}
 @GetMapping("/organizer/redemptions") public String delivery(){return "rewards/delivery";}
 @PostMapping("/organizer/redemptions/lookup") public String lookup(@RequestParam String credential,HttpServletRequest r,Model m){try{m.addAttribute("delivery",codes.lookup(credential,r.getRemoteAddr()));m.addAttribute("credential",credential);}catch(BusinessRuleException e){m.addAttribute("deliveryError",e.getMessage());}
  // Receipt of an event this account does not manage (or its own claim): explain on the same page; nothing about the receipt is shown.
  catch(org.springframework.security.access.AccessDeniedException e){m.addAttribute("deliveryError",e.getMessage()!=null&&e.getMessage().startsWith("Otra persona")?e.getMessage():"Este recibo es de un evento que no administras con esta cuenta. Lo entrega el personal de ese evento.");m.addAttribute("credential",credential);}
  return "rewards/delivery";}
 /** DEC-068: in-person evidence check. Scanning the receipt only identifies it; this registers that the photo was seen. */
 @PostMapping("/organizer/redemptions/evidence") public String evidence(@RequestParam String credential,@RequestParam long matchId,HttpServletRequest r,Model m){
  try{boolean created=codes.validateEvidence(credential,matchId,r.getRemoteAddr());m.addAttribute("deliveryMessage",created?"Evidencia comprobada y registrada a tu nombre. Ya puedes confirmar la entrega. No se sumaron puntos.":"Esa evidencia ya estaba comprobada. Ya puedes confirmar la entrega.");}
  catch(BusinessRuleException e){m.addAttribute("deliveryError",e.getMessage());}
  catch(org.springframework.security.access.AccessDeniedException e){m.addAttribute("deliveryError",e.getMessage());}
  return lookup(credential,r,m);}
 @PostMapping("/organizer/redemptions/evidence-missing") public String evidenceMissing(@RequestParam String credential,@RequestParam(defaultValue="") String note,HttpServletRequest r,Model m){
  try{codes.evidenceMissing(credential,note,r.getRemoteAddr());m.addAttribute("deliveryMessage","Incidencia registrada. El recibo sigue pendiente: no se entregó ni se cobraron puntos. La persona verá tu indicación en su recibo.");}
  catch(BusinessRuleException e){m.addAttribute("deliveryError",e.getMessage());}
  return lookup(credential,r,m);}
 @PostMapping("/organizer/redemptions/deliver") public String deliver(@RequestParam String credential,@RequestParam ClaimStatus status,HttpServletRequest r,Model m,RedirectAttributes flash){
  try{codes.deliver(credential,status,r.getRemoteAddr());}catch(BusinessRuleException e){m.addAttribute("deliveryError",e.getMessage());return lookup(credential,r,m);}flash.addFlashAttribute("deliveryMessage",status==com.ping.reward.ClaimStatus.DELIVERED?"Entrega confirmada. No se cobraron puntos adicionales.":"Incidencia registrada. El recibo sigue sin entregar y no se cobraron puntos adicionales.");return "redirect:/organizer/redemptions";}
 private ResponseEntity<byte[]> image(CredentialService.Issued c){try{var matrix=new com.google.zxing.qrcode.QRCodeWriter().encode("PING:"+c.purpose()+":"+c.token(),com.google.zxing.BarcodeFormat.QR_CODE,320,320);var img=new java.awt.image.BufferedImage(320,320,java.awt.image.BufferedImage.TYPE_INT_RGB);for(int x=0;x<320;x++)for(int y=0;y<320;y++)img.setRGB(x,y,matrix.get(x,y)?0x000000:0xffffff);var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(img,"png",bytes);return ResponseEntity.ok().header("Cache-Control","no-store, private").header("X-Content-Type-Options","nosniff").body(bytes.toByteArray());}catch(com.google.zxing.WriterException|java.io.IOException ex){throw new IllegalStateException("Could not encode local QR",ex);}}
}
