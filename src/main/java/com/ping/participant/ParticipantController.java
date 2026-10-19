package com.ping.participant;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @Profile("!bootstrap") @RequestMapping("/participants")
public class ParticipantController {
    private final com.ping.auth.AuthSupport auth;
    private final ParticipantService service;
    public ParticipantController(ParticipantService service,com.ping.auth.AuthSupport auth) { this.auth=auth; this.service = service; }
    @GetMapping public String list(Model model) { model.addAttribute("participants", service.list().stream().filter(p->auth.legacyFixture()||auth.canManage(p.eventId())).toList()); return "participants/list"; }
    @GetMapping("/new") public String form(Model model) {
        model.addAttribute("participantForm", new ParticipantForm()); model.addAttribute("events", service.registrationEvents()); return "participants/form";
    }
    @PostMapping public String create(@Valid @ModelAttribute ParticipantForm participantForm, BindingResult errors, Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) { model.addAttribute("events", service.registrationEvents()); return "participants/form"; }
        ParticipantView participant = service.create(participantForm); flash.addFlashAttribute("message", "Participante registrado.");
        return "redirect:/participants/" + participant.id();
    }
    @GetMapping("/{id}") public String detail(@PathVariable long id, Model model) { model.addAttribute("participant", service.get(id)); return "participants/detail"; }
    @PostMapping("/{id}/availability") public String availability(@PathVariable long id, @Valid @ModelAttribute AvailabilityForm availabilityForm, BindingResult errors, Model model, RedirectAttributes flash) {
        service.get(id);
        if (errors.hasErrors()) {
            model.addAttribute("participant", service.get(id)); model.addAttribute("formError", "Recarga la página y selecciona una disponibilidad válida."); return "participants/detail";
        }
        service.setAvailability(id, availabilityForm); flash.addFlashAttribute("message", "Disponibilidad actualizada."); return "redirect:/participants/" + id;
    }
}
