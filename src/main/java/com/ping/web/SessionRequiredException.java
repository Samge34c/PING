package com.ping.web;

/** No browser-supplied ID may recover an identity after the session is lost. */
public class SessionRequiredException extends BusinessRuleException {
    public SessionRequiredException() { super("Tu sesión ya no está disponible. Vuelve a entrar con el código del evento. Esta entrada no recupera tu identidad ni tu saldo anterior."); }
}
