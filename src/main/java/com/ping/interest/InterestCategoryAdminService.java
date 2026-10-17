package com.ping.interest;
import java.util.*;
import jakarta.persistence.*;import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.validation.annotation.Validated;
import com.ping.web.*;
@Service @Profile("!bootstrap") @Validated @Transactional(readOnly=true)
public class InterestCategoryAdminService {
 private final InterestCategoryRepository categories;private final InterestRepository interests;private final InterestCatalogService catalog;
 @PersistenceContext private EntityManager entityManager;
 public InterestCategoryAdminService(InterestCategoryRepository categories,InterestRepository interests,InterestCatalogService catalog){this.categories=categories;this.interests=interests;this.catalog=catalog;}
 public record CategoryRow(long id,String name,int presentationOrder,boolean active,List<String> members){}
 public List<CategoryRow> list(){var all=interests.findAllByOrderByNameAsc();return categories.findAllByOrderByPresentationOrderAscNameAscIdAsc().stream().map(c->new CategoryRow(c.getId(),c.getName(),c.getPresentationOrder(),c.isActive(),all.stream().filter(i->i.getCategory()!=null&&i.getCategory().getId().equals(c.getId())).map(Interest::getName).toList())).toList();}
 private InterestCategory get(long id){return categories.findById(id).orElseThrow(()->new NotFoundException("No encontramos esa categoría."));}
 public InterestCategoryForm form(long id){var c=get(id);var f=new InterestCategoryForm();f.setName(c.getName());f.setPresentationOrder(c.getPresentationOrder());f.setActive(c.isActive());f.setVersion(c.getVersion());f.setInterestIds(interests.findAllByOrderByNameAsc().stream().filter(i->i.getCategory()!=null&&i.getCategory().getId().equals(id)).map(Interest::getId).toList());return f;}
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
 public long save(Long id,@Valid InterestCategoryForm form){
  catalog.lock();var category=id==null?new InterestCategory():get(id);
  if(id!=null&&(form.getVersion()==null||form.getVersion()!=category.getVersion()))throw new BusinessRuleException("La categoría o sus asociaciones cambiaron. Recarga el formulario antes de guardar.");
  String name=form.getName().strip();if(name.isEmpty())throw new BusinessRuleException("Escribe un nombre para la categoría.");
  if(categories.existsByNameIgnoreCaseAndIdNot(name,id==null?-1:id))throw new BusinessRuleException("Ya existe una categoría con ese nombre.");
  Set<Long> selected=new LinkedHashSet<>(form.getInterestIds());var all=interests.findAllByOrderByNameAsc();
  if(!all.stream().map(Interest::getId).toList().containsAll(selected))throw new BusinessRuleException("Elige intereses del catálogo existente.");
  category.change(name,form.getPresentationOrder(),form.isActive());categories.saveAndFlush(category);
  Set<InterestCategory> touched=new HashSet<>();
  for(var interest:all){var previous=interest.getCategory();if(selected.contains(interest.getId())){
    if(previous==null||!previous.getId().equals(category.getId())){if(previous!=null)touched.add(previous);interest.changeCategory(category);touched.add(category);}
   }else if(previous!=null&&previous.getId().equals(category.getId())){interest.changeCategory(null);touched.add(category);}
  }
  // Membership edits invalidate forms of every affected category, even if its name/state did not change.
  touched.forEach(c->entityManager.lock(c,LockModeType.OPTIMISTIC_FORCE_INCREMENT));
  return category.getId();
 }
}
