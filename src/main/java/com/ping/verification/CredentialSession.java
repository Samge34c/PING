package com.ping.verification;
import java.util.*;
import jakarta.servlet.http.HttpSession;
public final class CredentialSession {
 private CredentialSession(){}
 public static CredentialService.Issued get(HttpSession s,String purpose,long context,long self){Object v=s.getAttribute("credential:"+purpose+":"+context+":"+self);return v instanceof CredentialService.Issued i?i:null;}
 public static void put(HttpSession s,CredentialService.Issued i){s.setAttribute("credential:"+i.purpose()+":"+i.context()+":"+i.issuer(),i);}
}
