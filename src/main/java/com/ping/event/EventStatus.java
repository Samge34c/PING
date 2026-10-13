package com.ping.event;

public enum EventStatus {
    DRAFT("Borrador"), OPEN("Abierto"), PAUSED("Pausado"), CLOSED("Cerrado");

    private final String label;
    EventStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
