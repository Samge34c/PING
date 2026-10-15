package com.ping.participant;

import com.ping.match.DemoSession;
import com.ping.web.BusinessRuleException;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @Profile("!bootstrap")
public class RecognitionHintController {
    private final RecognitionHintService hints;
    public RecognitionHintController(RecognitionHintService hints) { this.hints = hints; }
    @PostMapping("/me/recognition-hint") public String save(@Valid @ModelAttribute("recognitionHintForm") RecognitionHintForm form,
            BindingResult errors, HttpSession session, Model model, HttpServletResponse response, RedirectAttributes flash) {
        long self = DemoSession.participantId(session); // Never bind an owner from client-supplied participantId.
        response.setHeader("Cache-Control", "no-store, private");
        if (errors.hasErrors()) { response.setStatus(400); model.addAttribute("hintError", "Revisa la pista. No se guardaron cambios."); return "recognition/edit"; }
        try { hints.save(self, form); }
        catch (BusinessRuleException error) {
            response.setStatus(409); model.addAttribute("hintError", error.getMessage());
            form.setVersion(hints.ownForm(self).getVersion()); return "recognition/edit";
        }
        flash.addFlashAttribute("message", "Cómo reconocerme guardado. Solo lo verá tu conexión mientras el encuentro esté activo.");
        return "redirect:/me";
    }
    @GetMapping("/matches/{id}/recognition-hint") @ResponseBody
    public java.util.Map<String, Object> other(@PathVariable long id, HttpSession session, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, private");
        var hint = hints.other(id, DemoSession.participantId(session));
        if (hint == null) return java.util.Map.of("active", false);
        return java.util.Map.of("active", true, "text", hint.text() == null ? "" : hint.text(), "revision", hint.revision());
    }
}
