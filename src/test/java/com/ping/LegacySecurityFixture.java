package com.ping;
import org.springframework.context.annotation.*;
@Configuration @Profile("test")
public class LegacySecurityFixture {
 @Bean @Primary com.ping.match.JourneyPolicy historicalPolicyFixture(){return new com.ping.match.JourneyPolicy(){@Override public int version(){return 1;}};}
}
