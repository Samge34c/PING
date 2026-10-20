package com.ping.web;

/** No browser-supplied ID may recover an identity after the session is lost. */
public class SessionRequiredException extends BusinessRuleException {
    public SessionRequiredException() { super("Tu sesión ya no está disponible. Entra de nuevo a tu cuenta para recuperar tu participación y saldo vinculados. Una entrada invitada anterior sin vincular requiere su sesión original."); }
}
