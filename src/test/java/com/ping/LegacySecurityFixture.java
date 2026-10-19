package com.ping;
import org.springframework.context.annotation.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
@Configuration @Profile("test")
public class LegacySecurityFixture {
 @Bean SecurityFilterChain legacyFixture(HttpSecurity http)throws Exception{return http.authorizeHttpRequests(a->a.anyRequest().permitAll()).csrf(c->c.disable()).build();}
}
