package com.ping.performance;

import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import com.ping.auth.AccountService;
import com.ping.event.*;
import com.ping.experience.EventExperienceCatalog;
import com.ping.match.*;
import com.ping.participant.*;
import com.ping.reward.RewardService;
import com.ping.support.TestBrowser;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
    "spring.datasource.url=jdbc:h2:mem:backend-read-work;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=validate",
    "ping.mail.from=performance@example.test", "ping.public-base-url=http://127.0.0.1:8090"})
@ActiveProfiles("security-test") @Import(SqlProbe.class)
abstract class BackendReadWorkSupport {
    @Autowired AccountService accounts;
    @Autowired EventService events;
    @Autowired ParticipantService people;
    @Autowired MatchingService matching;
    @MockitoSpyBean MatchService matches;
    @MockitoSpyBean RecognitionHintService hints;
    @Autowired RewardService rewards;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean JavaMailSender sender;
    @LocalServerPort int port;
    final List<com.ping.support.MailCapture.Mail> mail = new CopyOnWriteArrayList<>();
    static final String PASSWORD = "Synthetic backend measurement 2026";
    record Pair(long event, long first, long second, String emailA, String emailB, long accountA, long accountB) { }
    record Measured(HttpResponse<String> response, SqlProbe.Snapshot sql) { }

    @BeforeEach void transport() {
        mail.clear();
        com.ping.support.MailCapture.capture(sender, mail);
    }
    @AfterEach void stopCapture() { SqlProbe.CAPTURING.set(false); }
    long account(String email) {
        accounts.register(email,PASSWORD);
        var token=Pattern.compile("token=([A-Za-z0-9_-]{43})").matcher(mail.getLast().getText());
        if (!token.find()) throw new AssertionError("Synthetic verification token missing");
        accounts.consume(token.group(1),"VERIFY",PASSWORD);
        return accounts.find(email).orElseThrow();
    }
    Pair pair(boolean sofa) {
        var f=new EventForm(); f.setName(sofa ? "SOFA 2026" : "Performance " + UUID.randomUUID());
        long event=events.create(f).getId(); events.transition(event,EventStatus.OPEN);
        long[] p=new long[2], a=new long[2]; String[] addresses=new String[2];
        for(int i=0;i<2;i++) {
            addresses[i]="backend-"+UUID.randomUUID()+"@example.test"; a[i]=account(addresses[i]);
            var form=new ParticipantForm();form.setEventId(event);form.setAlias("Synthetic person " + i);form.setAvailable(true);
            p[i]=people.create(form).id();accounts.bind(a[i],p[i],event);
        }
        return new Pair(event,p[0],p[1],addresses[0],addresses[1],a[0],a[1]);
    }
    TestBrowser login(String address) throws Exception {
        var browser=new TestBrowser(port);browser.get("/login");
        assertThat(browser.post("/login",Map.of("email",address,"password",PASSWORD)).statusCode()).isEqualTo(302);
        return browser;
    }
    void history(long match, int size) {
        for(int i=0;i<size;i++) jdbc.update("""
            insert into ping_matches(event_id,first_id,second_id,status,first_wait,second_wait,
                common_interests,first_interests,second_interests,created_at)
            select event_id,first_id,second_id,'CANCELLED',first_wait,second_wait,
                common_interests,first_interests,second_interests,created_at from ping_matches where id=?
            """, match);
    }
    Measured get(TestBrowser browser, String route, String label) throws Exception {
        clearInvocations(matches,hints);
        SqlProbe.begin(); long start=System.nanoTime();
        HttpResponse<String> response;
        try { response=browser.get(route); }
        finally { SqlProbe.CAPTURING.set(false); }
        var snapshot=SqlProbe.end(System.nanoTime()-start);snapshot.print(label);
        return new Measured(response,snapshot);
    }
    long calls(Object spy,String method) {
        return mockingDetails(spy).getInvocations().stream().filter(i -> i.getMethod().getName().equals(method)).count();
    }
}
