package com.ping.performance;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class RecognitionReadWorkTests extends BackendReadWorkSupport {
    @Test void activePageReadsTheAuthorizedHintOnceAndRefreshesOnNextRequest() throws Exception {
        var p=pair(false);var foreign=pair(false);long id=matching.search(p.first());
        try(var a=login(p.emailA());var b=login(p.emailB());var stranger=login(foreign.emailA())) {
            jdbc.update("update participants set recognition_hint='FIRST_PERSON_SYNTHETIC_HINT',version=version+1 where id=?",p.first());
            jdbc.update("update participants set recognition_hint='PRIVATE_SYNTHETIC_HINT',version=version+1 where id=?",p.second());
            assertThat(a.get("/matches/"+id).body()).doesNotContain("PRIVATE_SYNTHETIC_HINT");
            matches.accept(id,p.first());assertThat(a.get("/matches/"+id).body()).doesNotContain("PRIVATE_SYNTHETIC_HINT");
            matches.accept(id,p.second());
            var measured=get(a,"/matches/"+id,"active-hint");
            assertThat(measured.response().statusCode()).isEqualTo(200);
            assertThat(measured.response().body()).contains("PRIVATE_SYNTHETIC_HINT");
            long reads=calls(hints,"other");
            System.out.println("BACKEND_JAVA active-hint hints_other_calls="+reads);
            jdbc.update("update participants set recognition_hint='UPDATED_SYNTHETIC_HINT',version=version+1 where id=?",p.second());
            assertThat(a.get("/matches/"+id).body()).contains("UPDATED_SYNTHETIC_HINT").doesNotContain("PRIVATE_SYNTHETIC_HINT");
            assertThat(b.get("/matches/"+id).body()).contains("FIRST_PERSON_SYNTHETIC_HINT","UPDATED_SYNTHETIC_HINT");
            var denied=stranger.get("/matches/"+id);assertThat(denied.statusCode()).isNotEqualTo(200);
            assertThat(denied.body()).doesNotContain("UPDATED_SYNTHETIC_HINT");
            matches.cancel(id,p.first());assertThat(a.get("/matches/"+id).body()).doesNotContain("UPDATED_SYNTHETIC_HINT");
            assertThat(reads).as("reuse one guarded read for text and revision").isEqualTo(1);
        }
    }
}
