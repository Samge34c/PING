package com.ping.encounter;
/** DEC-066. How an encounter activity is checked before the existing completion grants points. */
public enum CheckMethod {
 /** Both participants confirm (behaviour before DEC-066 and default for every existing row). */
 BILATERAL("Confirmación de ambos","Se completa cuando ambos confirman que la hicieron."),
 /** Authorised staff of the same event approve the pair's submission; both still confirm. */
 STAFF_REVIEW("Revisión del personal","El personal del evento revisa lo que envíen; los puntos llegan al aprobarse.");
 public final String label,help;
 CheckMethod(String label,String help){this.label=label;this.help=help;}
 public String getLabel(){return label;} public String getHelp(){return help;}
 /** Unknown or blank stored values fall back to the historical bilateral rule. */
 public static CheckMethod of(String value){try{return value==null?BILATERAL:valueOf(value);}catch(IllegalArgumentException unknown){return BILATERAL;}}
}
