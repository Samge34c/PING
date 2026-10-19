package com.ping.auth;
import jakarta.servlet.http.*;
import org.springframework.security.web.csrf.*;
/** Bridge the existing UUID form token without changing published forms. */
public class ExistingCsrfRepository implements CsrfTokenRepository {
 public CsrfToken generateToken(HttpServletRequest r){return new DefaultCsrfToken("X-CSRF-TOKEN","_csrf",java.util.UUID.randomUUID().toString());}
 public void saveToken(CsrfToken t,HttpServletRequest r,HttpServletResponse response){if(t==null){var s=r.getSession(false);if(s!=null)s.removeAttribute("pingCsrfToken");}else r.getSession().setAttribute("pingCsrfToken",t.getToken());}
 public CsrfToken loadToken(HttpServletRequest r){var s=r.getSession(false);var v=s==null?null:s.getAttribute("pingCsrfToken");return v instanceof String t?new DefaultCsrfToken("X-CSRF-TOKEN","_csrf",t):null;}
}
