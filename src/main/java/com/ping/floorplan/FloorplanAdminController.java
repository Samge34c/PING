package com.ping.floorplan;

import com.ping.event.EventService;
import com.ping.web.BusinessRuleException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @Profile("!bootstrap")
public class FloorplanAdminController {
    private final FloorplanService plans;
    private final EventService events;
    public FloorplanAdminController(FloorplanService plans,EventService events){this.plans=plans;this.events=events;}
    private String page(long id,Model model){
        var plan=plans.current(id);model.addAttribute("event",events.get(id));model.addAttribute("plan",plan);
        var points=plans.points(id);model.addAttribute("planPoints",points);
        model.addAttribute("missingPoints",points.stream().filter(p->!p.placed()).count());
        if(!model.containsAttribute("positionForm")){
            var form=new FloorplanPositionForm();if(plan!=null){form.setPlanId(plan.id());form.setRevision(plan.revision());}model.addAttribute("positionForm",form);
        }
        var form=(FloorplanPositionForm)model.getAttribute("positionForm");
        model.addAttribute("fpX",form.getX());model.addAttribute("fpY",form.getY());model.addAttribute("fpLabel","Punto seleccionado");
        return "floorplan/admin";
    }
    @GetMapping("/events/{id}/floorplan")
    public String edit(@PathVariable long id,@RequestParam(required=false) Long pointId,Model model){
        if(pointId!=null){var plan=plans.current(id);if(plan!=null){var selected=plans.points(id).stream().filter(p->p.id()==pointId).findFirst().orElse(null);
            if(selected!=null){var f=new FloorplanPositionForm();f.setPlanId(plan.id());f.setRevision(plan.revision());f.setPointId(pointId);f.setX(selected.x());f.setY(selected.y());model.addAttribute("positionForm",f);}
        }}
        return page(id,model);
    }
    @PostMapping("/events/{id}/floorplan/image")
    public String upload(@PathVariable long id,@RequestParam(required=false) String expectedPlanId,@RequestParam(defaultValue="false") boolean replace,
                         @RequestParam MultipartFile image,Model model,HttpServletResponse response,RedirectAttributes flash){
        try{plans.upload(id,expectedPlanId,replace,image);flash.addFlashAttribute("message","Plano guardado. Coloca los puntos sobre esta imagen; las posiciones anteriores no se copian.");return "redirect:/events/"+id+"/floorplan";}
        catch(BusinessRuleException error){response.setStatus(409);model.addAttribute("uploadError",error.getMessage());return page(id,model);}
    }
    @PostMapping("/events/{id}/floorplan/position")
    public String place(@PathVariable long id,@Valid @ModelAttribute("positionForm") FloorplanPositionForm form,BindingResult errors,
                        Model model,HttpServletResponse response,RedirectAttributes flash){
        if(form.getX()==null&&!errors.hasFieldErrors("x"))errors.rejectValue("x","required","Indica una posición horizontal entre 0 y 1.");
        if(form.getY()==null&&!errors.hasFieldErrors("y"))errors.rejectValue("y","required","Indica una posición vertical entre 0 y 1.");
        if(!errors.hasErrors())try{plans.place(id,form.getPlanId(),form.getRevision(),form.getPointId(),form.getX(),form.getY());flash.addFlashAttribute("message","Posición guardada para próximos encuentros.");return "redirect:/events/"+id+"/floorplan?pointId="+form.getPointId();}
        catch(BusinessRuleException error){errors.reject("position",error.getMessage());}
        response.setStatus(409);return page(id,model);
    }
    @PostMapping("/events/{id}/floorplan/remove")
    public String remove(@PathVariable long id,@RequestParam String planId,@RequestParam long revision,@RequestParam long pointId,RedirectAttributes flash){
        plans.remove(id,planId,revision,pointId);flash.addFlashAttribute("message","Marcador retirado. El punto y los encuentros activos se conservan.");return "redirect:/events/"+id+"/floorplan?pointId="+pointId;
    }
    @GetMapping("/events/{id}/floorplan/images/{planId}") @ResponseBody
    public ResponseEntity<byte[]> image(@PathVariable long id,@PathVariable String planId,@RequestHeader(value="If-None-Match",required=false) String etag){
        var plan=plans.plan(id,planId);String tag='"'+plan.id()+'"';
        var builder=ResponseEntity.ok().eTag(tag).cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(365)).cachePrivate().immutable())
            .header("X-Content-Type-Options","nosniff").header("Content-Disposition","inline; filename=\"plan-"+plan.id()+(plan.contentType().equals("image/png")?".png\"":".jpg\""));
        if(tag.equals(etag))return ResponseEntity.status(304).eTag(tag).build();
        return builder.contentType(MediaType.parseMediaType(plan.contentType())).body(plans.image(id,planId));
    }
}
