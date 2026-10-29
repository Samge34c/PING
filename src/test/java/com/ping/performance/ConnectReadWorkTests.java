package com.ping.performance;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class ConnectReadWorkTests extends BackendReadWorkSupport {
    @Test void connectRedirectsWithoutBuildingTheDashboardForEitherParticipant() throws Exception {
        var p=pair(false);long id=matching.search(p.first());history(id,48);
        try(var a=login(p.emailA());var b=login(p.emailB())) {
            var checks=new org.assertj.core.api.SoftAssertions();
            for(var state:List.of("OFFERED","ACTIVE")) {
                if(state.equals("ACTIVE")){matches.accept(id,p.first());matches.accept(id,p.second());}
                int side=0;
                for(var browser:List.of(a,b)) {
                    var measured=get(browser,"/connect","connect-"+state+"-"+side++);
                    assertThat(measured.response().statusCode()).isEqualTo(302);
                    assertThat(measured.response().headers().firstValue("Location").orElseThrow()).endsWith("/matches/"+id);
                    checks.assertThat(calls(matches,"list")).as("no dashboard history for redirect").isZero();
                    checks.assertThat(calls(hints,"ownForm")).as("no unused recognition form for redirect").isZero();
                    checks.assertThat(measured.sql().rows("ping_matches")).as("bounded current-match lookup").isLessThanOrEqualTo(1);
                }
            }
            checks.assertAll();
        }
    }
    @Test void idleConnectKeepsRealDataAndSignatureWithoutHistoryOrUnusedForms() throws Exception {
        var p=pair(false);long id=matching.search(p.first());history(id,48);matches.cancel(id,p.first());
        try(var a=login(p.emailA())) {
            var measured=get(a,"/connect","connect-idle");
            assertThat(measured.response().statusCode()).isEqualTo(200);
            assertThat(measured.response().body()).contains("Synthetic person 0","Conectar","data-ping-live=");
            assertThat(measured.response().body()).doesNotContain("Su palabra de encuentro");
            var checks=new org.assertj.core.api.SoftAssertions();
            checks.assertThat(calls(matches,"list")).isZero();checks.assertThat(calls(hints,"ownForm")).isZero();
            checks.assertThat(measured.sql().rows("ping_matches")).isLessThanOrEqualTo(1);checks.assertAll();
        }
    }
    @Test void connectStillChecksOwnershipAndFreshSessionEpoch() throws Exception {
        var p=pair(false);var foreign=pair(false);long id=matching.search(p.first());
        try(var a=login(p.emailA());var b=login(foreign.emailA())) {
            assertThat(a.get("/connect?participantId="+foreign.first()).headers().firstValue("Location").orElseThrow()).endsWith("/matches/"+id);
            assertThat(b.get("/connect").body()).doesNotContain("/matches/"+id);
            jdbc.update("update ping_accounts set auth_epoch=auth_epoch+1 where id=?",p.accountA());
            assertThat(a.get("/connect").headers().firstValue("Location").orElseThrow()).contains("/login");
            assertThat(b.get("/connect").statusCode()).isEqualTo(200);
        }
    }
}
