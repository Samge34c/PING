package com.ping.encounter;
/** DEC-066. How an encounter activity is checked before the existing completion grants points. */
public enum CheckMethod {
 /** Both participants confirm (behaviour before DEC-066 and default for every existing row). */
 BILATERAL("Confirmación de ambos","Se completa cuando ambos confirman que la hicieron."),
 /** Authorised staff of the same event approve the pair's submission; both still confirm. */
 STAFF_REVIEW("Revisión del personal","El personal del evento revisa lo que envíen; los puntos llegan al aprobarse."),
 /** DEC-068: both mark it as done (same bilateral confirmation) and the points arrive at once; the pair shows
  *  the evidence (e.g. a photo on their phone) when collecting a reward paid with those points. PING stores no photo. */
 AT_DELIVERY("Comprobar al entregar el beneficio","Ambos la marcan como hecha y reciben los puntos; al recoger el premio muestran la evidencia en el punto de entrega.");
 public final String label,help;
 CheckMethod(String label,String help){this.label=label;this.help=help;}
 public String getLabel(){return label;}
 /** Methods whose organiser text (what to review or what to show) is required. */
 public boolean needsInstructions(){return this!=BILATERAL;} public String getHelp(){return help;}
 /** Unknown or blank stored values fall back to the historical bilateral rule. */
 public static CheckMethod of(String value){try{return value==null?BILATERAL:valueOf(value);}catch(IllegalArgumentException unknown){return BILATERAL;}}
}
