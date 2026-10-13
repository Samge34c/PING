package com.ping.demo;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
@Controller @Profile("!bootstrap")
public class ThemeController {
    public record Theme(String key, String name, String description) { }
    @GetMapping("/demo/themes") public String themes(Model model) {
        model.addAttribute("themes", List.of(
            new Theme("B", "DARK OFICIAL", "Oscuro predeterminado: grafito, violeta y dorado. Superficies diferenciadas y foco visible."),
            new Theme("D", "LIGHT OFICIAL", "Claro oficial: fondos neutros, morado Ping y amarillo como acento. Texto y enlaces oscuros para mantener contraste.")));
        return "demo/themes";
    }
}
