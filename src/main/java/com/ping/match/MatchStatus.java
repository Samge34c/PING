package com.ping.match;
public enum MatchStatus {
    OFFERED("Propuesta recibida"), ACTIVE("Encuentro activo"), COMPLETED("Completada"), CANCELLED("Cancelada");
    private final String label;
    MatchStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
