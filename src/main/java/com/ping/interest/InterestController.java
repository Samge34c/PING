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
    @GetMapping("/interests") public String catalog(Model model) { model.addAttribute("interests", service.catalog()); return "interests/catalog"; }
    @GetMapping("/participants/{id}/interests") public String form(@PathVariable long id, Model model) {
        model.addAttribute("interestSelectionForm", service.selection(id)); populate(model, id); return "interests/form";
    }
    @PostMapping("/participants/{id}/interests") public String save(@PathVariable long id, @Valid @ModelAttribute InterestSelectionForm interestSelectionForm, BindingResult errors, Model model, RedirectAttributes flash, jakarta.servlet.http.HttpSession session) {
        participants.get(id);
        if (errors.hasErrors()) { populate(model, id); return "interests/form"; }
        service.save(id, interestSelectionForm); flash.addFlashAttribute("message", "Intereses guardados."); return "participant".equals(session.getAttribute("pingEntryMode")) && Long.valueOf(id).equals(session.getAttribute("pingDemoParticipantId")) ? "redirect:/me" : "redirect:/participants/" + id;
    }
    private void populate(Model model, long id) { model.addAttribute("participant", participants.get(id)); model.addAttribute("interests", service.catalog()); }
}
