package com.ping.participant;
import org.springframework.ui.Model;
/** Catálogo V3 para las vistas que muestran el editor (alta y /me/avatar). */
public final class AvatarCatalog{
 private AvatarCatalog(){}
 public static void addTo(Model m,AvatarV3 draft){
  m.addAttribute("avatarDraft",draft);m.addAttribute("avatarV3Animals",AvatarV3.ANIMALS);m.addAttribute("avatarV3Palettes",AvatarV3.PALETTES);
  m.addAttribute("avatarV3Heads",AvatarV3.HEADS);m.addAttribute("avatarV3Audios",AvatarV3.AUDIOS);m.addAttribute("avatarV3Faces",AvatarV3.FACES);m.addAttribute("avatarV3Necks",AvatarV3.NECKS);
  m.addAttribute("avatarV3Colors",AvatarV3.COLORS);m.addAttribute("avatarV3Discs",AvatarV3.DISCS);m.addAttribute("avatarV3Bulky",String.join(",",AvatarV3.BULKY_HEADS));
 }
}
