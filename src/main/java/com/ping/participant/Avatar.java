package com.ping.participant;
import java.util.*;
import com.ping.web.BusinessRuleException;
/** Avatar público. Los campos preset son el catálogo anterior y se conservan; v3 (DEC-058) es opcional y, cuando existe, es el que se muestra. */
public record Avatar(String animal,String hat,String accessory,String background,AvatarV3 v3){
 public Avatar(String animal,String hat,String accessory,String background){this(animal,hat,accessory,background,null);}
 public Avatar withV3(AvatarV3 value){return new Avatar(animal,hat,accessory,background,value);}
 public static final Map<String,String> ANIMALS=Map.of("gato","Gato","perro","Perro","zorro","Zorro","oso","Oso","conejo","Conejo","panda","Panda","rana","Rana","buho","Búho");
 public static final Map<String,String> HATS=Map.of("none","Sin sombrero","gorra","Gorra","gorro","Gorro","fiesta","Fiesta","corona","Corona");
 public static final Map<String,String> ACCESSORIES=Map.of("none","Sin accesorio","gafas","Gafas","pajarita","Pajarita","bufanda","Bufanda");
 public static final Map<String,String> COLORS=Map.of("violeta","Violeta","amarillo","Amarillo","menta","Menta","coral","Coral","azul","Azul","crema","Crema");
 public Avatar{if(!ANIMALS.containsKey(animal)||!HATS.containsKey(hat)||!ACCESSORIES.containsKey(accessory)||!COLORS.containsKey(background))throw new BusinessRuleException("Elige un avatar del catálogo de PING.");}
 public String label(){if(v3!=null)return v3.label();return ANIMALS.get(animal)+", "+HATS.get(hat)+", "+ACCESSORIES.get(accessory)+", fondo "+COLORS.get(background);}
 public static Avatar defaults(){return new Avatar("gato","none","none","violeta");}
}
