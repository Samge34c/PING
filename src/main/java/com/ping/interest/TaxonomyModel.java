package com.ping.interest;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Profile;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
@ControllerAdvice @Profile("!bootstrap")
public class TaxonomyModel{
 private final InterestTaxonomy taxonomy;
 public TaxonomyModel(InterestTaxonomy t){taxonomy=t;}
 @ModelAttribute public void model(HttpServletRequest r,Model m){String p=r.getRequestURI();if(p.equals("/join/interests")||p.matches("/participants/[0-9]+/interests")){m.addAttribute("taxonomyNodes",taxonomy.nodes());m.addAttribute("taxonomyMemberships",taxonomy.memberships());}}
}
