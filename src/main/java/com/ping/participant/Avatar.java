package com.ping.participant;
import java.util.*;
import com.ping.web.BusinessRuleException;
public record Avatar(String animal,String hat,String accessory,String background){
 public static final Map<String,String> ANIMALS=Map.of("gato","Gato","perro","Perro","zorro","Zorro","oso","Oso","conejo","Conejo","panda","Panda","rana","Rana","buho","Búho");
 public static final Map<String,String> HATS=Map.of("none","Sin sombrero","gorra","Gorra","gorro","Gorro","fiesta","Fiesta","corona","Corona");
 public static final Map<String,String> ACCESSORIES=Map.of("none","Sin accesorio","gafas","Gafas","pajarita","Pajarita","bufanda","Bufanda");
 public static final Map<String,String> COLORS=Map.of("violeta","Violeta","amarillo","Amarillo","menta","Menta","coral","Coral","azul","Azul","crema","Crema");
 public Avatar{if(!ANIMALS.containsKey(animal)||!HATS.containsKey(hat)||!ACCESSORIES.containsKey(accessory)||!COLORS.containsKey(background))throw new BusinessRuleException("Elige un avatar del catálogo de PING.");}
 public static Avatar defaults(){return new Avatar("gato","none","none","violeta");}
}
