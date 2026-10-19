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
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
@Configuration
public class AuthConfiguration {
 @Bean PasswordEncoder pingPasswordEncoder(){return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();}
 @Bean @Profile("!bootstrap") ApplicationRunner firstGeneral(AccountService a,Environment e){return args->a.bootstrap(e.getProperty("PING_BOOTSTRAP_EMAIL",""),e.getProperty("PING_BOOTSTRAP_PASSWORD",""));}
 @Bean @Profile("!bootstrap") ProviderCatalog providers(Environment e){return new ProviderCatalog(e);}
 @Bean @Profile("!bootstrap") @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="ping.oauth.enabled",havingValue="true") ClientRegistrationRepository clients(ProviderCatalog p){if(p.registrations().isEmpty())throw new IllegalStateException("PING_OAUTH_ENABLED requires a configured provider");return new InMemoryClientRegistrationRepository(p.registrations());}
 @Bean @Profile("bootstrap") SecurityFilterChain technical(HttpSecurity http)throws Exception{return http.authorizeHttpRequests(a->a.anyRequest().permitAll()).csrf(c->c.disable()).build();}
 @Bean @Profile("!bootstrap & !test") SecurityFilterChain normal(HttpSecurity http,AccountService accounts,LoginSuccess success,AuthThrottle throttle,ObjectProvider<ClientRegistrationRepository> clients)throws Exception{
  http.authorizeHttpRequests(a->a.dispatcherTypeMatchers(DispatcherType.ERROR,DispatcherType.FORWARD).permitAll().requestMatchers("/","/login","/register","/join","/join/**","/account/recover","/account/verify","/account/reset","/css/**","/js/**","/img/**","/fonts/**","/vendor/**","/error","/errors/**","/oauth2/**","/login/oauth2/**").permitAll().anyRequest().authenticated());
  http.csrf(c->c.csrfTokenRepository(new ExistingCsrfRepository()).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));
  http.userDetailsService(accounts).requestCache(c->c.requestCache(new NullRequestCache())).formLogin(f->f.loginPage("/login").usernameParameter("email").successHandler(success).failureUrl("/login?error=true"));
  http.logout(l->l.logoutUrl("/logout").logoutSuccessUrl("/login?logout=true").invalidateHttpSession(true).deleteCookies("JSESSIONID"));
  http.exceptionHandling(e->e.authenticationEntryPoint((r,s,x)->{s.setHeader("Cache-Control","no-store");if(r.getRequestURI().equals("/me/state")){s.setStatus(401);s.setContentType("application/json");s.getWriter().write("{\"error\":\"SESSION_REQUIRED\",\"entryUrl\":\"/login\"}");}else s.sendRedirect("/login");}).accessDeniedHandler((r,s,x)->s.sendError(403,"No tienes permiso para esta acción.")));
  http.addFilterBefore(new OncePerRequestFilter(){@Override protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest r,jakarta.servlet.http.HttpServletResponse s,jakarta.servlet.FilterChain chain)throws java.io.IOException,jakarta.servlet.ServletException{if(r.getMethod().equals("POST")&&r.getRequestURI().equals("/login")){try{throttle.check("login-ip",r.getRemoteAddr(),50,10);throttle.check("login-account",Objects.toString(r.getParameter("email"),"").strip().toLowerCase(Locale.ROOT),10,10);}catch(com.ping.web.BusinessRuleException e){s.setStatus(429);s.setContentType("text/plain;charset=UTF-8");s.getWriter().write(e.getMessage());return;}}chain.doFilter(r,s);}},UsernamePasswordAuthenticationFilter.class);
  if(clients.getIfAvailable()!=null)http.oauth2Login(o->o.loginPage("/login").successHandler(success).failureHandler((r,s,e)->{r.getSession().removeAttribute("pingLinkAccount");r.getSession().removeAttribute("pingLinkUntil");s.sendRedirect("/login?oauthError=true");}));
  return http.build();
 }
 public static class ProviderCatalog{
  private final List<ClientRegistration> clients;
  public ProviderCatalog(Environment e){var list=new ArrayList<ClientRegistration>();if(e.getProperty("ping.oauth.enabled",Boolean.class,false)){add(list,e,"google",CommonOAuth2Provider.GOOGLE);add(list,e,"facebook",CommonOAuth2Provider.FACEBOOK);}clients=List.copyOf(list);}
  private void add(List<ClientRegistration> list,Environment e,String id,CommonOAuth2Provider provider){String prefix="PING_"+id.toUpperCase(Locale.ROOT);String key=e.getProperty(prefix+"_CLIENT_ID",""),secret=e.getProperty(prefix+"_CLIENT_SECRET","");if(key.isBlank()||secret.isBlank())return;var b=provider.getBuilder(id).clientId(key).clientSecret(secret).redirectUri("{baseUrl}/login/oauth2/code/{registrationId}");if(id.equals("google"))b.scope("openid","email","profile");else b.scope("email","public_profile");list.add(b.build());}
  public List<ClientRegistration> registrations(){return clients;}
  public List<String> available(){return clients.stream().map(ClientRegistration::getRegistrationId).toList();}
 }
}
