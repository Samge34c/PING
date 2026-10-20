package com.ping.interest;
import jakarta.validation.Valid;import org.springframework.stereotype.Controller;import org.springframework.context.annotation.Profile;import org.springframework.ui.Model;import org.springframework.validation.BindingResult;import org.springframework.web.bind.annotation.*;
@Controller @Profile("!bootstrap") @RequestMapping("/interests/taxonomy")
public class TaxonomyController {
 private final TaxonomyAdminService service;private final InterestService interests;
 public TaxonomyController(TaxonomyAdminService s,InterestService i){service=s;interests=i;}
 private void context(Model m,String key){m.addAttribute("nodes",service.list());m.addAttribute("interests",interests.catalog());m.addAttribute("editing",key!=null);}
 @GetMapping public String list(Model m){m.addAttribute("nodes",service.list());return "interests/taxonomy";}
 @GetMapping({"/new","/{key}/edit"})public String edit(@PathVariable(required=false)String key,Model m){m.addAttribute("taxonomyForm",key==null?new TaxonomyForm():service.form(key));context(m,key);return "interests/taxonomy-form";}
 @PostMapping({"","/{key}/edit"})public String save(@PathVariable(required=false)String key,@Valid @ModelAttribute("taxonomyForm")TaxonomyForm f,BindingResult errors,Model m){if(!errors.hasErrors())try{service.save(key,f);return "redirect:/interests/taxonomy";}catch(com.ping.web.BusinessRuleException ex){errors.reject("taxonomy",ex.getMessage());}context(m,key);return "interests/taxonomy-form";}
}
