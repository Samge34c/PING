package com.ping.interest;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.ping.participant.ParticipantService;

@Controller @Profile("!bootstrap")
public class InterestController {
    private final InterestService service;
    private final ParticipantService participants;
    public InterestController(InterestService service, ParticipantService participants) { this.service = service; this.participants = participants; }
    @GetMapping("/interests") public String catalog(Model model) { model.addAttribute("interests", service.catalog());model.addAttribute("interestGroups",service.groups(java.util.List.of()));return "interests/catalog"; }
    @GetMapping("/participants/{id}/interests") public String form(@PathVariable long id, Model model) {
        model.addAttribute("interestSelectionForm", service.selection(id)); populate(model, id); return "interests/form";
    }
    @PostMapping("/participants/{id}/interests") public String save(@PathVariable long id, @Valid @ModelAttribute InterestSelectionForm interestSelectionForm, BindingResult errors, Model model, RedirectAttributes flash, jakarta.servlet.http.HttpSession session, jakarta.servlet.http.HttpServletResponse response) {
        participants.get(id);
        if (errors.hasErrors()) { populate(model, id); return "interests/form"; }
        try {service.save(id, interestSelectionForm);}catch(com.ping.web.BusinessRuleException error){response.setStatus(409);errors.reject("selection",error.getMessage());populate(model,id);return "interests/form";}
        flash.addFlashAttribute("message", "Intereses guardados."); return "participant".equals(session.getAttribute("pingEntryMode")) && Long.valueOf(id).equals(session.getAttribute("pingDemoParticipantId")) ? "redirect:/me" : "redirect:/participants/" + id;
    }
    private void populate(Model model, long id) { model.addAttribute("participant", participants.get(id)); model.addAttribute("interests", service.catalog());
        var form=(InterestSelectionForm)model.getAttribute("interestSelectionForm");
        var selected=form.getInterestIds()==null?java.util.List.<Long>of():form.getInterestIds().stream().filter(java.util.Objects::nonNull).toList();
        var groups=service.groups(selected);model.addAttribute("interestGroups",groups);model.addAttribute("selectedIds",selected);model.addAttribute("interestOptionCount",groups.stream().mapToInt(g->g.choices().size()).sum());
        model.addAttribute("selectedNames",groups.stream().flatMap(g->g.choices().stream()).filter(c->selected.contains(c.id())).map(InterestCatalogService.Choice::name).toList());model.addAttribute("selectionContext","edit");
    }
}
