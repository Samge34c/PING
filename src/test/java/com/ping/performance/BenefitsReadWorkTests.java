package com.ping.performance;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class BenefitsReadWorkTests extends BackendReadWorkSupport {
    @Test void benefitsUsesBoundedPredicatesAndOnlyTheVisibleCatalog() throws Exception {
        var p=pair(true);var foreign=pair(false);long id=matching.search(p.first());history(id,48);matches.cancel(id,p.first());
        for(int i=0;i<24;i++) jdbc.update("""
            insert into benefits(name,points_cost,active,event_id,description,claim_location,claim_instructions,default_delivery,version)
            values(?,5,true,?,'Synthetic description','Synthetic desk','Synthetic instructions',false,0)
            """, "Foreign benefit " + i, foreign.event());
        jdbc.update("""
            insert into benefits(name,points_cost,active,event_id,description,claim_location,claim_instructions,default_delivery,version)
            values('Own synthetic benefit',5,true,?,'Synthetic description','Synthetic desk','Synthetic instructions',false,0)
            """, p.event());
        long visible=jdbc.queryForObject("select count(*) from benefits where active=true and (event_id is null or event_id=?)",Long.class,p.event());
        try(var a=login(p.emailA())) {
            var measured=get(a,"/me/benefits","benefits-sofa");
            assertThat(measured.response().statusCode()).isEqualTo(200);
            assertThat(measured.response().body()).contains("Own synthetic benefit").doesNotContain("Foreign benefit");
            var checks=new org.assertj.core.api.SoftAssertions();
            checks.assertThat(measured.sql().rows("benefits")).as("no foreign catalog rows hydrated").isEqualTo(visible);
            checks.assertThat(measured.sql().rows("ping_matches")).as("boolean predicates must not hydrate history").isLessThanOrEqualTo(2);
            checks.assertThat(calls(matches,"list")).as("promo needs only an existence predicate").isZero();checks.assertAll();
        }
    }
    @Test void pendingCompletionRemainsFreshAndScopedToTheParticipant() {
        var p=pair(false);var other=pair(false);long id=matching.search(p.first());history(id,48);
        for(boolean pending:List.of(false,true,false)) {
            jdbc.update("update ping_matches set completion_pending=? where id=?",pending,id);
            SqlProbe.begin();boolean answer=rewards.pendingCompletion(p.first());var snapshot=SqlProbe.end(0);snapshot.print("pending-"+pending);
            assertThat(answer).isEqualTo(pending);
            assertThat(matches.completionState(p.first()).pending()).isEqualTo(pending);
            assertThat(rewards.pendingCompletion(p.second())).isEqualTo(pending);
            assertThat(rewards.pendingCompletion(other.first())).isFalse();
            assertThat(snapshot.rows("ping_matches")).as("existence predicate returns only one row").isLessThanOrEqualTo(1);
        }
    }
    @Test void completedPredicatePreservesBothOrientationsAndRefreshesWithoutCaching() {
        var p=pair(false);var other=pair(false);long id=matching.search(p.first());
        for(String status:List.of("OFFERED","COMPLETED","OFFERED")) {
            jdbc.update("update ping_matches set status=? where id=?",status,id);
            boolean expected=status.equals("COMPLETED");
            assertThat(matches.completionState(p.first()).completed()).isEqualTo(expected);
            assertThat(matches.completionState(p.second()).completed()).isEqualTo(expected);
            assertThat(matches.completionState(other.first()).completed()).isFalse();
        }
    }}
