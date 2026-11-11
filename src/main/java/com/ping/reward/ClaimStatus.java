package com.ping.reward;
public enum ClaimStatus {
    REQUESTED("Solicitado"), DELIVERED("Entregado"), INCIDENT("Incidencia"),
    /** DEC-068: cancelled because the organisation withdrew the participant; the cost went back to the balance. */
    CANCELLED("Anulado");
    private final String label;
    ClaimStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
