package com.ping.web;

import com.ping.event.EventEntryLinks;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/** Public metadata never derives an address or personal content from the request. */
@ControllerAdvice @Profile("!bootstrap")
public class PagePresentation {
    public record PublicMetadata(boolean publicPage, String canonical, String image) { }
    private final EventEntryLinks links;
    public PagePresentation(EventEntryLinks links) { this.links = links; }
    @ModelAttribute public void present(HttpServletRequest request, Model model) {
        String path = request.getRequestURI();
        boolean publicPage = request.getMethod().equals("GET") && java.util.Set.of("/", "/join", "/join/event", "/events").contains(path);
        String base = links.baseUrl();
        model.addAttribute("publicMetadata", new PublicMetadata(publicPage, publicPage && base != null ? base + path : null,
                publicPage && base != null ? base + "/img/ping-social.png" : null));
        model.addAttribute("interestSelectorScript", path.equals("/join/interests") || path.matches("/participants/[0-9]+/interests") || path.equals("/interests"));
        model.addAttribute("recognitionEditorScript", path.equals("/me") || path.equals("/me/recognition-hint") || path.matches("/matches/[0-9]+"));
    }
}
