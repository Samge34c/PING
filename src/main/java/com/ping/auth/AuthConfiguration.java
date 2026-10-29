package com.ping.auth;
import java.util.*;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.crypto.password.*;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
@Configuration
public class AuthConfiguration {
 @Bean PasswordEncoder pingPasswordEncoder(){return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();}
 @Bean @Profile("!bootstrap") ApplicationRunner firstGeneral(AccountService a,Environment e){return args->a.bootstrap(e.getProperty("PING_BOOTSTRAP_EMAIL",""),e.getProperty("PING_BOOTSTRAP_PASSWORD",""));}
 @Bean @Profile("!bootstrap") ProviderCatalog providers(Environment e){return new ProviderCatalog(e);}
 /** Providers exist only with PING_OAUTH_ENABLED and a complete id/secret pair; otherwise PING starts with e-mail access only. */
 static class ProvidersConfigured implements Condition{public boolean matches(ConditionContext c,org.springframework.core.type.AnnotatedTypeMetadata m){return !new ProviderCatalog(c.getEnvironment(),false).registrations().isEmpty();}}
 @Bean @Profile("!bootstrap") @Conditional(ProvidersConfigured.class) ClientRegistrationRepository clients(ProviderCatalog p){return new InMemoryClientRegistrationRepository(p.registrations());}
 /** Access tokens are not needed after sign-in: keep none in memory or in the session. */
 static class DiscardAuthorizedClients implements OAuth2AuthorizedClientRepository{
  public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String id,org.springframework.security.core.Authentication p,jakarta.servlet.http.HttpServletRequest r){return null;}
  public void saveAuthorizedClient(OAuth2AuthorizedClient c,org.springframework.security.core.Authentication p,jakarta.servlet.http.HttpServletRequest r,jakarta.servlet.http.HttpServletResponse s){}
  public void removeAuthorizedClient(String id,org.springframework.security.core.Authentication p,jakarta.servlet.http.HttpServletRequest r,jakarta.servlet.http.HttpServletResponse s){}
 }
 /** Stable, non-sensitive reason for the login page; only the OAuth error code is logged. */
 static String failure(org.springframework.security.core.AuthenticationException e){
  String code=e instanceof OAuth2AuthenticationException o?o.getError().getErrorCode():"unknown";
  org.slf4j.LoggerFactory.getLogger(AuthConfiguration.class).info("Acceso con proveedor no completado: {}",code);
  return switch(code){case "access_denied"->"cancelled";case "authorization_request_not_found","invalid_state_parameter"->"expired";default->"provider";};
 }
 @Bean @Profile("bootstrap") SecurityFilterChain technical(HttpSecurity http)throws Exception{return http.authorizeHttpRequests(a->a.anyRequest().permitAll()).csrf(c->c.disable()).build();}
 @Bean @Profile("bootstrap") org.springframework.security.core.userdetails.UserDetailsService technicalUsers(){return name->{throw new org.springframework.security.core.userdetails.UsernameNotFoundException("No hay cuentas en el perfil técnico.");};}
 @Bean @Profile("!bootstrap") SecurityFilterChain normal(HttpSecurity http,AccountService accounts,LoginSuccess success,AuthThrottle throttle,ObjectProvider<ClientRegistrationRepository> clients,AuthSupport auth)throws Exception{
  if(auth.legacyFixture())return http.authorizeHttpRequests(a->a.anyRequest().permitAll()).csrf(c->c.disable()).build();
  http.authorizeHttpRequests(a->a.dispatcherTypeMatchers(DispatcherType.ERROR,DispatcherType.FORWARD).permitAll().requestMatchers("/","/login","/register","/join","/join/**","/account/recover","/account/verify","/account/reset","/css/**","/js/**","/img/**","/fonts/**","/vendor/**","/error","/errors/**","/oauth2/**","/login/oauth2/**").permitAll().anyRequest().authenticated());
  http.csrf(c->c.csrfTokenRepository(new ExistingCsrfRepository()).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));
  http.userDetailsService(accounts).requestCache(c->c.requestCache(new NullRequestCache())).formLogin(f->f.loginPage("/login").usernameParameter("email").successHandler(success).failureUrl("/login?error=true"));
  http.logout(l->l.logoutUrl("/logout").logoutSuccessUrl("/login?logout=true").invalidateHttpSession(true).deleteCookies("JSESSIONID"));
  http.exceptionHandling(e->e.authenticationEntryPoint((r,s,x)->{s.setHeader("Cache-Control","no-store");if(r.getRequestURI().equals("/me/state")){s.setStatus(401);s.setContentType("application/json");s.getWriter().write("{\"error\":\"SESSION_REQUIRED\",\"entryUrl\":\"/login\"}");}else s.sendRedirect("/login");}).accessDeniedHandler((r,s,x)->s.sendError(403,"No tienes permiso para esta acción.")));
  http.addFilterBefore(new OncePerRequestFilter(){@Override protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest r,jakarta.servlet.http.HttpServletResponse s,jakarta.servlet.FilterChain chain)throws java.io.IOException,jakarta.servlet.ServletException{if(r.getMethod().equals("POST")&&r.getRequestURI().equals("/login")){try{throttle.check("login-ip",r.getRemoteAddr(),50,10);throttle.check("login-account",Objects.toString(r.getParameter("email"),"").strip().toLowerCase(Locale.ROOT),10,10);}catch(com.ping.web.BusinessRuleException e){s.setStatus(429);s.setContentType("text/plain;charset=UTF-8");s.getWriter().write(e.getMessage());return;}}chain.doFilter(r,s);}},UsernamePasswordAuthenticationFilter.class);
  if(clients.getIfAvailable()!=null)http.oauth2Login(o->o.loginPage("/login").authorizedClientRepository(new DiscardAuthorizedClients()).successHandler(success).failureHandler((r,s,e)->{
   var session=r.getSession();boolean linking=session.getAttribute("pingLinkAccount")!=null;session.removeAttribute("pingLinkAccount");session.removeAttribute("pingLinkUntil");session.removeAttribute("pingLinkEpoch");
   String provider=r.getRequestURI().endsWith("/facebook")?"facebook":"google",reason=failure(e);
   // A cancelled link keeps the PING session that started it; a failed sign-in returns to the login page with the event draft intact.
   s.sendRedirect((linking?"/account?linkError=":"/login?oauthError=")+reason+"&provider="+provider);}));
  return http.build();
 }
 public static class ProviderCatalog{
  private final List<ClientRegistration> clients;
  public ProviderCatalog(Environment e){this(e,true);}
  ProviderCatalog(Environment e,boolean report){var list=new ArrayList<ClientRegistration>();boolean enabled=e.getProperty("ping.oauth.enabled",Boolean.class,false);if(enabled){add(list,e,"google",CommonOAuth2Provider.GOOGLE);add(list,e,"facebook",CommonOAuth2Provider.FACEBOOK);}clients=List.copyOf(list);
   if(report&&enabled&&clients.isEmpty())org.slf4j.LoggerFactory.getLogger(AuthConfiguration.class).warn("PING_OAUTH_ENABLED=true sin PING_GOOGLE_CLIENT_ID/SECRET completos: Google queda oculto y sigue el acceso por correo.");}
  private void add(List<ClientRegistration> list,Environment e,String id,CommonOAuth2Provider provider){String prefix="PING_"+id.toUpperCase(Locale.ROOT);String key=e.getProperty(prefix+"_CLIENT_ID","").strip(),secret=e.getProperty(prefix+"_CLIENT_SECRET","").strip();if(key.isBlank()||secret.isBlank())return;var b=provider.getBuilder(id).clientId(key).clientSecret(secret).redirectUri(origin(e)+"/login/oauth2/code/{registrationId}");if(id.equals("google"))b.scope("openid","email","profile");else b.scope("email","public_profile");list.add(b.build());}
  /** The provider must receive the exact registered callback, so prefer PING_PUBLIC_BASE_URL over proxy-derived headers. */
  static String origin(Environment e){String base=e.getProperty("ping.public-base-url","").strip().replaceAll("/+$","");if(base.isEmpty())return "{baseUrl}";try{var uri=java.net.URI.create(base);if(("https".equals(uri.getScheme())||"http".equals(uri.getScheme()))&&uri.getHost()!=null&&uri.getUserInfo()==null&&(uri.getRawPath()==null||uri.getRawPath().isEmpty())&&uri.getRawQuery()==null)return base;}catch(IllegalArgumentException invalid){}return "{baseUrl}";}
  public List<ClientRegistration> registrations(){return clients;}
  public List<String> available(){return clients.stream().map(ClientRegistration::getRegistrationId).toList();}
 }
}
