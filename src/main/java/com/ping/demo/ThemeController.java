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
            new Theme("A", "Actual · identidad oficial", "Negro, morado Ping y amarillo. Predeterminado: conserva la marca."),
            new Theme("B", "Dark moderno", "Grafito, violeta eléctrico y dorado. Superficies más separadas para escanear el flujo."),
            new Theme("C", "Morado dominante", "Morado profundo, morado brillante, amarillo suave y blanco cálido."),
            new Theme("D", "Claro opcional", "Neutros claros, morado Ping y amarillo como detalle. Enlaces morados para mantener contraste.")));
        return "demo/themes";
    }
}
