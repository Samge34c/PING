package com.ping.interest;
import jakarta.validation.Valid;import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;import org.springframework.stereotype.Controller;import org.springframework.ui.Model;import org.springframework.validation.BindingResult;import org.springframework.web.bind.annotation.*;
import com.ping.web.BusinessRuleException;
@Controller @Profile("!bootstrap") @RequestMapping("/interests/categories")
public class InterestCategoryController {
 private final InterestCategoryAdminService service;private final InterestService interests;
 public InterestCategoryController(InterestCategoryAdminService service,InterestService interests){this.service=service;this.interests=interests;}
 @GetMapping public String list(Model m){m.addAttribute("categories",service.list());return "interests/categories";}
 @GetMapping("/new") public String create(Model m){m.addAttribute("categoryForm",new InterestCategoryForm());populate(m,null);return "interests/category-form";}
 @GetMapping("/{id}/edit") public String edit(@PathVariable long id,Model m){m.addAttribute("categoryForm",service.form(id));populate(m,id);return "interests/category-form";}
 @PostMapping({"","/{id}/edit"}) public String save(@PathVariable(required=false) Long id,@Valid @ModelAttribute("categoryForm") InterestCategoryForm f,BindingResult errors,Model m,HttpServletResponse response){
  if(!errors.hasErrors())try{service.save(id,f);return "redirect:/interests/categories";}catch(BusinessRuleException ex){response.setStatus(409);errors.reject("category",ex.getMessage());}
  populate(m,id);return "interests/category-form";
 }
 private void populate(Model m,Long id){m.addAttribute("itemId",id);m.addAttribute("interests",interests.catalog());}
}
