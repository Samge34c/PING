package com.ping.auth;
import java.util.*;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
public record PingUser(long id,String email,String passwordHash,boolean verified,boolean general,boolean organizer,long epoch) implements UserDetails {
 @Override public String getUsername(){return email;}
 @Override public String getPassword(){return passwordHash;}
 @Override public boolean isEnabled(){return verified;}
 @Override public Collection<? extends GrantedAuthority> getAuthorities(){var a=new ArrayList<GrantedAuthority>();a.add(new SimpleGrantedAuthority("ROLE_PARTICIPANT"));if(organizer||general)a.add(new SimpleGrantedAuthority("ROLE_EVENT_ADMIN"));if(general)a.add(new SimpleGrantedAuthority("ROLE_GENERAL_ADMIN"));return a;}
}
