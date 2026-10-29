package com.ping.eventextras;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
/** Standalone own-data SVG; PNG rasterization is voluntary and local in the browser. */
@Service @Profile("!bootstrap")
public class ExperienceCard {
 private final EventExperienceService service;
 public ExperienceCard(EventExperienceService s){service=s;}
 static String xml(String s){var clean=new StringBuilder();s.codePoints().filter(c->c==9||c==10||c==13||c>=32&&c<=0xD7FF||c>=0xE000&&c<=0xFFFD||c>=0x10000&&c<=0x10FFFF).forEach(clean::appendCodePoint);return clean.toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
 private byte[] read(String path)throws IOException{try(var in=new ClassPathResource(path).getInputStream()){return in.readAllBytes();}}
 private String asset(String path)throws IOException{return Base64.getEncoder().encodeToString(read("static"+path));}
 private String brand()throws IOException{
  // Reuse the complete canonical vector component, never redraw its paths.
  String layout=new String(read("templates/fragments/layout.html"),StandardCharsets.UTF_8);int start=layout.indexOf("<svg class=\"ping-logo\"");int end=layout.indexOf("</svg>",start)+6;
  if(start<0||end<6)throw new IOException("Brand component unavailable");
  String svg=layout.substring(start,end).replaceFirst("<svg ","<svg xmlns=\"http://www.w3.org/2000/svg\" ");
  int tag=svg.indexOf('>')+1;svg=svg.substring(0,tag)+"<style>.logo-stem,.logo-type{fill:#0D0B12}.logo-ring{fill:#B58CF0}.logo-lens{fill:#E2D21C}</style>"+svg.substring(tag);
  return Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
 }
 private static double unit(int c){return c==' '?0.34:c>=0x2E80?1:0.6;}
 private static List<String> lines(String text,int size,int width){
  var out=new ArrayList<String>();var line=new StringBuilder();double used=0;
  for(String word:text.strip().split("\\s+")){
   double wordWidth=word.codePoints().mapToDouble(ExperienceCard::unit).sum()*size;
   if(!line.isEmpty()&&used+unit(' ')*size+wordWidth>width){out.add(line.toString());line.setLength(0);used=0;}
   if(!line.isEmpty()){line.append(' ');used+=unit(' ')*size;}
   // Only split a single token when it cannot fit on a line by itself.
   for(int cp:word.codePoints().toArray()){double w=unit(cp)*size;if(used+w>width&&!line.isEmpty()){out.add(line.toString());line.setLength(0);used=0;}line.appendCodePoint(cp);used+=w;}
  }
  if(!line.isEmpty())out.add(line.toString());return out;
 }
 private static void text(StringBuilder s,String value,int x,int y,int size,String color,int weight){s.append("<text x=\"").append(x).append("\" y=\"").append(y).append("\" font-size=\"").append(size).append("\" fill=\"").append(color).append("\" font-weight=\"").append(weight).append("\">").append(xml(value)).append("</text>");}
 public byte[] render(long self)throws IOException{
  var p=service.personal(self);var avatar=p.avatar();var layers=new ArrayList<String>();String disc="#ECE4F6";
  if(avatar.v3()!=null){layers.addAll(avatar.v3().layers());disc=avatar.v3().disc();}
  else{disc=switch(avatar.background()){case "amarillo"->"#F8E836";case "menta"->"#B7DCC3";case "coral"->"#FAE0DB";case "azul"->"#C8D7EF";case "crema"->"#FBE9CF";default->"#ECE4F6";};layers.add("/img/avatars/animales/"+avatar.animal()+".svg");if(!avatar.accessory().equals("none"))layers.add("/img/avatars/accesorios/"+avatar.accessory()+".svg");if(!avatar.hat().equals("none"))layers.add("/img/avatars/sombreros/"+avatar.hat()+".svg");}
  var s=new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1080\" height=\"1350\" viewBox=\"0 0 600 750\"><title>Mi experiencia en "+xml(p.eventName())+"</title><defs><style>@font-face{font-family:Manrope;src:url(data:font/ttf;base64,");
  s.append(asset("/fonts/Manrope-Variable.ttf")).append(")}text{font-family:Manrope,Arial,sans-serif}</style></defs><rect width=\"600\" height=\"750\" rx=\"32\" fill=\"#F8E836\"/><image x=\"40\" y=\"40\" width=\"170\" height=\"62\" href=\"data:image/svg+xml;base64,").append(brand()).append("\"/><circle cx=\"120\" cy=\"220\" r=\"80\" fill=\"").append(disc).append("\"/>");
  // Preserve the approved 84% framing and V3 rear/base/front/ear order.
  for(var layer:layers)s.append("<image x=\"52.8\" y=\"152.8\" width=\"134.4\" height=\"134.4\" href=\"data:image/svg+xml;base64,").append(asset(layer)).append("\"/>");
  double units=p.alias().codePoints().mapToDouble(ExperienceCard::unit).sum();int aliasSize=(int)Math.min(50,Math.max(22,900/Math.max(1,units)));int y=175;
  for(String line:lines(p.alias(),aliasSize,330)){text(s,line,225,y,aliasSize,"#0D0B12",800);y+=aliasSize+4;}
  y=340;for(String line:lines(p.eventName(),22,520)){text(s,line,40,y,22,"#0D0B12",700);y+=26;}
  s.append("<rect x=\"40\" y=\"485\" width=\"254\" height=\"110\" rx=\"20\" fill=\"#0D0B12\"/><rect x=\"306\" y=\"485\" width=\"254\" height=\"110\" rx=\"20\" fill=\"#0D0B12\"/>");
  text(s,Long.toString(p.completed()),62,540,50,"#F8E836",800);text(s,"Conexiones completadas",62,578,18,"#F7F4EF",700);
  text(s,Long.toString(p.balance()),328,540,50,"#F8E836",800);text(s,"Saldo actual",328,578,18,"#F7F4EF",700);
  y=632;int budget=4;
  if(p.completed()==0&&p.interests().isEmpty())text(s,"Aún no hay conexiones completadas.",40,y,20,"#0D0B12",700);
  for(String interest:p.interests()){var wrapped=lines(interest,18,510);if(wrapped.size()>budget)continue;for(String line:wrapped){text(s,line,40,y,18,"#0D0B12",700);y+=22;}budget-=wrapped.size();}
  text(s,"Hecho con PING · proyecto académico",40,730,16,"#0D0B12",700);s.append("</svg>");return s.toString().getBytes(StandardCharsets.UTF_8);
 }
}
