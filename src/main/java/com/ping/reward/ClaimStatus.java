package com.ping.reward;
public enum ClaimStatus {
    REQUESTED("Solicitado"), DELIVERED("Entregado"), INCIDENT("Incidencia");
    private final String label;
    ClaimStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
