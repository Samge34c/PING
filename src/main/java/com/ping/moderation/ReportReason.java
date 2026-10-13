package com.ping.moderation;
public enum ReportReason {
    UNCOMFORTABLE("Comportamiento incómodo"), NO_SHOW("No se presentó"), INAPPROPRIATE("Uso inapropiado"), OTHER("Otro");
    private final String label;
    ReportReason(String label) { this.label = label; }
    public String getLabel() { return label; }
}
