package com.ping.participant;
import java.util.*;
import com.ping.web.BusinessRuleException;
/** Avatar V3 (DEC-058): animal, pelaje y una pieza por slot con su color. Valida contra el catálogo y las incompatibilidades. */
public record AvatarV3(String animal,String palette,String head,String headColor,String audio,String audioColor,String face,String faceColor,String neck,String neckColor){
 private static Map<String,String> ordered(String... kv){var m=new LinkedHashMap<String,String>();for(int i=0;i<kv.length;i+=2)m.put(kv[i],kv[i+1]);return Collections.unmodifiableMap(m);}
 public static final Map<String,String> ANIMALS=ordered("gato","Gato","conejo","Conejo","nutria","Nutria","panda-rojo","Panda rojo");
 public static final Map<String,String> PALETTES=ordered("natural","Natural","dorado","Dorado","lila","Lila","perla","Perla","cacao","Cacao","coral","Coral");
 public static final Map<String,String> HEADS=ordered("none","Ninguno","gorro","Gorro tejido","gorra","Gorra","bucket","Sombrero bucket","banda","Banda deportiva","lazo","Lazo","pin-estrella","Pin estrella","vincha","Vincha de estrellas");
 public static final Map<String,String> AUDIOS=ordered("none","Ninguno","auriculares","Auriculares","microfono","Auriculares con micrófono");
 public static final Map<String,String> FACES=ordered("none","Ninguno","gafas-redondas","Gafas redondas","gafas-cuadradas","Gafas cuadradas","gafas-sol","Gafas de sol");
 public static final Map<String,String> NECKS=ordered("none","Ninguno","panuelo","Pañuelo","bufanda","Bufanda","corbatin","Corbatín","pase","Pase del evento");
 public static final Map<String,String> COLORS=ordered("violeta","Violeta","amarillo","Amarillo","menta","Menta","coral","Coral");
 /** Fondo del círculo por pelaje (variable «fondo/disco» de Figma). */
 public static final Map<String,String> DISCS=ordered("natural","#ECE4F6","dorado","#FBE9CF","lila","#E6DDF5","perla","#E1E8F0","cacao","#E9DDD2","coral","#FAE0DB");
 /** Piezas de cabeza que se cruzan con la diadema de los auriculares (catálogo: blockedPairs). */
 public static final Set<String> BULKY_HEADS=Set.of("gorro","gorra","bucket","vincha");
 static final Set<String> WITH_REAR=Set.of("auriculares","microfono","vincha","pase");
 public static final String CONFLICT="Los auriculares no combinan con gorro tejido, gorra, sombrero bucket ni vincha: se cruzan. Elige solo una de las dos piezas.";
 public AvatarV3{
  if(!ANIMALS.containsKey(animal)||!PALETTES.containsKey(palette)||!HEADS.containsKey(head)||!AUDIOS.containsKey(audio)||!FACES.containsKey(face)||!NECKS.containsKey(neck))throw new BusinessRuleException("Elige un avatar del catálogo de PING.");
  headColor=color(head,headColor);audioColor=color(audio,audioColor);faceColor=color(face,faceColor);neckColor=color(neck,neckColor);
  if(!"none".equals(audio)&&BULKY_HEADS.contains(head))throw new BusinessRuleException(CONFLICT);
 }
 private static String color(String piece,String value){if("none".equals(piece))return "violeta";if(value==null||value.isBlank())return "violeta";if(!COLORS.containsKey(value))throw new BusinessRuleException("Elige un color del catálogo de PING.");return value;}
 public static AvatarV3 defaults(){return new AvatarV3("gato","natural","none","violeta","none","violeta","none","violeta","none","violeta");}
 /** Sugerencia para quien todavía tiene un avatar del catálogo anterior: solo cuando el animal existe en V3; nunca se guarda sola. */
 public static AvatarV3 suggestionFor(Avatar preset){return preset!=null&&ANIMALS.containsKey(preset.animal())?new AvatarV3(preset.animal(),"natural","none","violeta","none","violeta","none","violeta","none","violeta"):defaults();}
 public static boolean hasEquivalent(Avatar preset){return preset!=null&&ANIMALS.containsKey(preset.animal());}
 public String disc(){return DISCS.get(palette);}
 /** Selección guardada como pares campo=valor para el editor: Deshacer y el estado «guardado» comparan contra esto, no contra el borrador mostrado. */
 public String formFields(){return "animal="+animal+";palette="+palette+";head="+head+";headColor="+headColor+";audio="+audio+";audioColor="+audioColor+";face="+face+";faceColor="+faceColor+";neck="+neck+";neckColor="+neckColor;}
 private static String piece(String slotLabel,String id,String color){return "none".equals(id)?null:slotLabel+" color "+COLORS.get(color).toLowerCase(Locale.ROOT);}
 public String label(){
  var parts=new ArrayList<String>();parts.add(ANIMALS.get(animal)+", pelaje "+PALETTES.get(palette).toLowerCase(Locale.ROOT));
  for(var p:Arrays.asList(piece(HEADS.get(head),head,headColor),piece(AUDIOS.get(audio),audio,audioColor),piece(FACES.get(face),face,faceColor),piece(NECKS.get(neck),neck,neckColor)))if(p!=null)parts.add(p);
  return String.join(", ",parts);
 }
 /** Capas en orden de pintado: traseras (cuello, cabeza, audio), cuerpo, delanteras (cuello, cara, cabeza), orejas del conejo sobre el bucket, audio. */
 public List<String> layers(){
  var out=new ArrayList<String>();
  for(var s:List.of(new String[]{neck,neckColor},new String[]{head,headColor},new String[]{audio,audioColor}))if(WITH_REAR.contains(s[0]))out.add("/img/avatars/v3/rear/"+animal+"-"+s[0]+"-"+s[1]+".svg");
  out.add("/img/avatars/v3/base/"+animal+"-"+palette+".svg");
  for(var s:List.of(new String[]{neck,neckColor},new String[]{face,faceColor},new String[]{head,headColor}))if(!"none".equals(s[0]))out.add("/img/avatars/v3/front/"+animal+"-"+s[0]+"-"+s[1]+".svg");
  if("conejo".equals(animal)&&"bucket".equals(head))out.add("/img/avatars/v3/base/conejo-"+palette+"-orejas.svg");
  if(!"none".equals(audio))out.add("/img/avatars/v3/front/"+animal+"-"+audio+"-"+audioColor+".svg");
  return out;
 }
 /** Capas de una miniatura del editor: el animal y pelaje actuales con una sola opción aplicada. */
 public List<String> tile(String kind,String id){
  return switch(kind){
   case "animal"->new AvatarV3(id,palette,"none",null,"none",null,"none",null,"none",null).layers();
   case "palette"->new AvatarV3(animal,id,"none",null,"none",null,"none",null,"none",null).layers();
   case "head"->new AvatarV3(animal,palette,id,headColor,"none",null,"none",null,"none",null).layers();
   case "audio"->new AvatarV3(animal,palette,"none",null,id,audioColor,"none",null,"none",null).layers();
   case "face"->new AvatarV3(animal,palette,"none",null,"none",null,id,faceColor,"none",null).layers();
   case "neck"->new AvatarV3(animal,palette,"none",null,"none",null,"none",null,id,neckColor).layers();
   default->throw new IllegalArgumentException(kind);};
 }
 /** Borrador tolerante para volver a mostrar el editor tras un error: descarta valores fuera del catálogo y la pieza de audio si choca con la cabeza. */
 public static AvatarV3 lenient(String animal,String palette,String head,String headColor,String audio,String audioColor,String face,String faceColor,String neck,String neckColor){
  String a=ANIMALS.containsKey(animal)?animal:"gato",p=PALETTES.containsKey(palette)?palette:"natural",h=HEADS.containsKey(head)?head:"none",au=AUDIOS.containsKey(audio)?audio:"none",f=FACES.containsKey(face)?face:"none",n=NECKS.containsKey(neck)?neck:"none";
  if(!"none".equals(au)&&BULKY_HEADS.contains(h))au="none";
  return new AvatarV3(a,p,h,COLORS.containsKey(headColor)?headColor:null,au,COLORS.containsKey(audioColor)?audioColor:null,f,COLORS.containsKey(faceColor)?faceColor:null,n,COLORS.containsKey(neckColor)?neckColor:null);
 }
}
