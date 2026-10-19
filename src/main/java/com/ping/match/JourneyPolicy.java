package com.ping.match;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
/** New runtime activations/receipts always use policy2; historical rows keep their frozen version. */
@Component @Profile("!bootstrap") public class JourneyPolicy {public int version(){return 2;}}
