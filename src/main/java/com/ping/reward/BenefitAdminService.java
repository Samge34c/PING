package com.ping.reward;
import java.util.List;
import com.ping.event.EventService;
import com.ping.web.*;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
@Service @Profile("!bootstrap") @Validated @Transactional(readOnly=true)
public class BenefitAdminService {
 private final BenefitRepository repository;private final EventService events;
 public BenefitAdminService(BenefitRepository repository,EventService events){this.repository=repository;this.events=events;}
 public List<Benefit> list(long event){events.get(event);return repository.findByEventIdOrderByIdAsc(event);}
 private Benefit owned(long event,long id){var b=repository.findById(id).orElseThrow(()->new NotFoundException("No encontramos esa recompensa."));if(b.getEventId()==null || b.getEventId()!=event)throw new NotFoundException("La recompensa no pertenece a este evento.");return b;}
 public BenefitForm form(long event,long id){return BenefitForm.from(owned(event,id));}
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
 public long save(long event,Long id,@Valid BenefitForm f){events.lock(event);var b=id==null?new Benefit(event):owned(event,id);if(id!=null && (f.getVersion()==null || f.getVersion()!=b.getVersion()))throw new BusinessRuleException("La recompensa cambió. Recarga el editor antes de guardar.");b.change(f);return repository.save(b).getId();}
}
