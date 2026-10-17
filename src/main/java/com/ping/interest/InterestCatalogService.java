package com.ping.interest;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import com.ping.web.BusinessRuleException;
@Service @Profile("!bootstrap") @Transactional(readOnly=true)
public class InterestCatalogService {
 private final InterestRepository interests;private final InterestCategoryRepository categories;private final InterestCatalogStateRepository state;
 public InterestCatalogService(InterestRepository interests,InterestCategoryRepository categories,InterestCatalogStateRepository state){this.interests=interests;this.categories=categories;this.state=state;}
 @Transactional(propagation=Propagation.MANDATORY) public void lock(){if(state.lockCatalog()==null)throw new IllegalStateException("Interest catalog lock missing");}
 @Transactional(propagation=Propagation.MANDATORY) public void validateSelection(Collection<Long> selected,Collection<Long> previous){
  lock();var available=new HashSet<>(interests.selectableIds());var retained=new HashSet<>(previous);
  if(selected.stream().anyMatch(id->!available.contains(id)&&!retained.contains(id)))throw new BusinessRuleException("Una categoría ya no está disponible. Quita las opciones no disponibles o conserva únicamente tus intereses ya guardados.");
 }
 public record Choice(long id,String name,boolean available){}
 public record Group(String id,String name,List<Choice> choices){}
 public List<Group> groups(Collection<Long> selected){
  var all=interests.findAllByOrderByNameAsc();var result=new ArrayList<Group>();
  for(var category:categories.findAllByOrderByPresentationOrderAscNameAscIdAsc()){
   if(!category.isActive())continue;
   var choices=all.stream().filter(i->i.getCategory()!=null&&i.getCategory().getId().equals(category.getId())).map(i->new Choice(i.getId(),i.getName(),true)).toList();
   if(!choices.isEmpty())result.add(new Group("category-"+category.getId(),category.getName(),choices));
  }
  var other=all.stream().filter(i->i.getCategory()==null).map(i->new Choice(i.getId(),i.getName(),true)).toList();
  if(!other.isEmpty())result.add(new Group("other","Otros intereses",other));
  var unavailable=all.stream().filter(i->!i.isSelectable()&&selected.contains(i.getId())).map(i->new Choice(i.getId(),i.getName(),false)).toList();
  if(!unavailable.isEmpty())result.add(new Group("retained","Conservados",unavailable));
  return List.copyOf(result);
 }
}
