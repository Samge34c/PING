package com.ping.event;
import java.util.*;
import java.text.Normalizer;
import com.ping.web.BusinessRuleException;
public final class EncounterOptions {
 private EncounterOptions(){}
 public static List<String> lines(String v){return v==null?List.of():v.lines().map(String::strip).filter(s->!s.isEmpty()).toList();}
 public static String key(String s){return Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").strip().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);}
 public static List<String> unique(String v){var m=new LinkedHashMap<String,String>();for(var s:lines(v))m.putIfAbsent(key(s),s.replaceAll("\\s+"," "));return List.copyOf(m.values());}
 public static int limit(int length){return length==160?3:length==40?100:10;}
 public static String normalize(String v,int max,String label){var rows=unique(v);if(rows.stream().anyMatch(s->s.length()>max||s.codePoints().anyMatch(Character::isISOControl)))throw new BusinessRuleException(label+": máximo "+max+" caracteres por opción.");return String.join("\n",rows);}
 public static void checkGrowth(String next,String old,int max,String label){int count=unique(next).size();if(count>max && count>unique(old).size())throw new BusinessRuleException(label+": máximo "+max+". Conservamos el exceso anterior; reduce o edita sin aumentar.");}
 public static void newLimits(EventForm f){checkGrowth(f.getMeetingPoints(),"",3,"Puntos");checkGrowth(f.getEncounterWords(),"",100,"Palabras");checkGrowth(f.getEncounterActivities(),"",10,"Actividades");}
 public static String select(String v,long id,String fallback){var rows=lines(v);return rows.isEmpty()?fallback:rows.get((int)Math.floorMod(id-1,(long)rows.size()));}
}
