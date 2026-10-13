package com.ping.event;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @Profile("!bootstrap") @RequestMapping("/events")
public class EventController {
    private final EventService service;
    public EventController(EventService service) { this.service = service; }
    @GetMapping public String list(Model model) { model.addAttribute("events", service.list()); return "events/list"; }
    @GetMapping("/new") public String createForm(Model model) {
        model.addAttribute("eventForm", new EventForm()); model.addAttribute("editing", false); return "events/form";
    }
    @PostMapping public String create(@Valid @ModelAttribute EventForm eventForm, BindingResult errors, Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) { model.addAttribute("editing", false); return "events/form"; }
        Event event = service.create(eventForm); flash.addFlashAttribute("message", "Evento creado en borrador.");
        return "redirect:/events/" + event.getId();
    }
    @GetMapping("/{id}") public String detail(@PathVariable long id, Model model) {
        model.addAttribute("event", service.get(id)); return "events/detail";
    }
    @GetMapping("/{id}/edit") public String editForm(@PathVariable long id, Model model) {
        model.addAttribute("eventForm", EventForm.from(service.get(id))); editing(model, id); return "events/form";
    }
    @PostMapping("/{id}/edit") public String edit(@PathVariable long id, @Valid @ModelAttribute EventForm eventForm, BindingResult errors, Model model, RedirectAttributes flash) {
        service.get(id);
        if (errors.hasErrors()) { editing(model, id); return "events/form"; }
        service.update(id, eventForm); flash.addFlashAttribute("message", "Datos del evento actualizados.");
        return "redirect:/events/" + id;
    }
    @PostMapping("/{id}/open") public String open(@PathVariable long id, RedirectAttributes flash) { return change(id, EventStatus.OPEN, flash); }
    @PostMapping("/{id}/pause") public String pause(@PathVariable long id, RedirectAttributes flash) { return change(id, EventStatus.PAUSED, flash); }
    @PostMapping("/{id}/resume") public String resume(@PathVariable long id, RedirectAttributes flash) { return change(id, EventStatus.OPEN, flash); }
    @PostMapping("/{id}/close") public String close(@PathVariable long id, RedirectAttributes flash) { return change(id, EventStatus.CLOSED, flash); }
    private String change(long id, EventStatus target, RedirectAttributes flash) {
        service.transition(id, target); flash.addFlashAttribute("message", "Estado del evento actualizado."); return "redirect:/events/" + id;
    }
    private void editing(Model model, long id) { model.addAttribute("editing", true); model.addAttribute("eventId", id); }
}
