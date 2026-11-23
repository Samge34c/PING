package com.ping.showcase;

import java.util.*;
import com.ping.admin.AdminService;
import com.ping.auth.AccountService;
import com.ping.encounter.*;
import com.ping.event.*;
import com.ping.match.*;
import com.ping.participant.*;
import com.ping.review.ActivityReviewService;
import com.ping.reward.*;
import com.ping.verification.CredentialService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * DEC-079 · Local candidate with FICTITIOUS data, isolated in an in-memory H2 database (nothing touches MySQL, Render or Aiven).
 * Not part of the suite: it only runs when asked for explicitly and keeps the server up so the pages can be opened.
 *
 *   .\mvnw.cmd test "-Dtest=ResultsShowcase" "-Dping.showcase=true"            (http://127.0.0.1:8096, 120 min)
 *   optional: "-Dping.showcase.minutes=30"
 *
 * Every encounter is completed through the real services (arrival, personal codes, bilateral confirmation, Saga), so
 * points, connections and unique people come from the same code as production. Receipts are marked delivered or
 * incident directly in the database (the receipt code step is not part of what this showcase illustrates).
 */
@EnabledIfSystemProperty(named = "ping.showcase", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {"server.port=8096", "spring.datasource.url=jdbc:h2:mem:showcase;MODE=MySQL;DB_CLOSE_DELAY=-1", "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=validate"})
@ActiveProfiles("verification-test")
class ResultsShowcase {
    static final String PASSWORD = "Ensayo-ficticio-2026";
    @Autowired EventService events; @Autowired EncounterService config; @Autowired ParticipantService people; @Autowired MatchingService matching;
    @Autowired MatchService matches; @Autowired CredentialService credentials; @Autowired AccountService accounts; @Autowired ActivityReviewService reviews;
    @Autowired RewardService rewards; @Autowired BenefitAdminService benefits; @Autowired AdminService admin; @Autowired JdbcTemplate sql; @Autowired PasswordEncoder encoder;

    long account(String email) { sql.update("insert into ping_accounts(email,password_hash,email_verified) values(?,?,true)", email, encoder.encode(PASSWORD)); return accounts.find(email).orElseThrow(); }
    long event(String name, String description) { var f = new EventForm(); f.setName(name); f.setDescription(description); return events.create(f).getId(); }
    long person(long event, String alias) { return person(event, alias, UUID.randomUUID() + "@ficticio.test"); }
    long person(long event, String alias, String email) { var f = new ParticipantForm(); f.setAlias(alias); f.setEventId(event); f.setAvailable(true); long id = people.create(f).id(); accounts.bind(account(email), id, event); return id; }
    void away(long... ids) { for (long id : ids) sql.update("update participants set available=false where id=?", id); }
    String key() { return UUID.randomUUID().toString(); }
    long active(long x, long y) { long m = matching.search(x); matches.accept(m, x); matches.accept(m, y); return m; }
    void verified(long m, long x, long y) {
        matches.arrive(m, x); matches.arrive(m, y);
        var cx = credentials.encounter(m, x, key()); var cy = credentials.encounter(m, y, key());
        credentials.verify(m, x, cy.manual(), "showcase"); credentials.verify(m, y, cx.manual(), "showcase");
    }
    long complete(long x, long y) { long m = active(x, y); verified(m, x, y); matches.confirm(m, x); matches.confirm(m, y); return m; }
    long place(long event, String name) { var p = new PointForm(); p.setName(name); p.setDirections("Junto a la señal de PING"); return config.savePoint(event, null, p); }
    long activity(long event, long point, String title, int points, CheckMethod method) {
        var a = new ActivityForm(); a.setTitle(title); a.setInstructions("Actividad breve de ejemplo."); a.setRewardPoints(points); a.setPointIds(Set.of(point)); a.setVerificationMethod(method);
        if (method == CheckMethod.STAFF_REVIEW) { a.setCheckInstructions("Que cuenten qué canción eligió cada persona."); a.setResponsePrompt("¿Qué canciones eligieron?"); }
        return config.saveActivity(event, null, a);
    }

    @Test void serve() throws Exception {
        accounts.bootstrap("admin@ping.test", PASSWORD);
        long general = accounts.find("admin@ping.test").orElseThrow();
        long organizer = account("organizacion@ping.test"), staff = account("personal@ping.test");

        // Event 1 · open, with activity, encounters in every state, opinions, receipts and one task to review.
        long main = event("Bienvenida universitaria (ficticio)", "Evento de demostración con datos ficticios.");
        events.transition(main, EventStatus.OPEN);
        long zone = place(main, "Gamer Zone");
        long coop = activity(main, zone, "Ronda cooperativa", 10, CheckMethod.BILATERAL);
        var bf = new BenefitForm(); bf.setName("Snack del evento"); bf.setDescription("Un snack por persona"); bf.setPointsCost(10); bf.setClaimLocation("Mesa amarilla de entregas"); bf.setClaimInstructions("Junto a la entrada principal.");
        long snack = benefits.save(main, null, bf);
        long ana = person(main, "Ana"), samuel = person(main, "Samuel");
        long m1 = complete(ana, samuel); away(samuel);
        long julian = person(main, "Julián"); long m2 = complete(ana, julian); away(ana, julian);
        long carlos = person(main, "Carlos"), dana = person(main, "Dana"); long m3 = complete(carlos, dana); away(dana);
        long eva = person(main, "Eva"); long m4 = complete(eva, carlos); away(carlos);
        long felipe = person(main, "Felipe"); long m5 = complete(eva, felipe); away(eva, felipe);
        long gabi = person(main, "Gabi", "gabi@ping.test"), hugo = person(main, "Hugo"); long m6 = complete(gabi, hugo); away(gabi, hugo);
        long ivan = person(main, "Iván"), juana = person(main, "Juana"); long cancelled = matching.search(ivan); matches.cancel(cancelled, ivan); away(ivan, juana);
        long karla = person(main, "Karla"), lucas = person(main, "Lucas"); long open = active(karla, lucas); matches.arrive(open, karla);
        long mateo = person(main, "Mateo"), nora = person(main, "Nora"); matching.search(mateo);
        // A task for staff review: only this activity is active when the encounter starts, so the method is frozen as STAFF_REVIEW.
        var off = config.activityForm(main, coop); off.setActive(false); config.saveActivity(main, coop, off);
        activity(main, zone, "Reto de canciones", 20, CheckMethod.STAFF_REVIEW);
        long olga = person(main, "Olga"), pablo = person(main, "Pablo"); long reviewed = active(olga, pablo); verified(reviewed, olga, pablo);
        reviews.submit(reviewed, olga, "Ella eligió Clocks y él Yellow.");
        var on = config.activityForm(main, coop); on.setActive(true); config.saveActivity(main, coop, on);
        person(main, "Quique", "participante@ping.test"); person(main, "Rosa");
        // An inactive activity configured before DEC-079 with «Comprobar al entregar» (kept as the earlier method).
        var legacy = new ActivityForm(); legacy.setTitle("Foto con la mascota"); legacy.setInstructions("Actividad anterior de ejemplo."); legacy.setRewardPoints(20); legacy.setPointIds(Set.of(zone));
        legacy.setVerificationMethod(CheckMethod.STAFF_REVIEW); legacy.setCheckInstructions("Una foto de los dos con la mascota"); legacy.setActive(false);
        sql.update("update event_activities set verification_method='AT_DELIVERY' where id=?", config.saveActivity(main, null, legacy));
        sql.update("update participants set available=true where id in (?,?,?)", ana, samuel, carlos);
        matches.giveFeedback(m1, ana, Feedback.Rating.USEFUL); matches.giveFeedback(m1, samuel, Feedback.Rating.USEFUL);
        matches.giveFeedback(m2, julian, Feedback.Rating.USEFUL); matches.giveFeedback(m3, dana, Feedback.Rating.NEUTRAL);
        matches.giveFeedback(m4, eva, Feedback.Rating.USEFUL); matches.giveFeedback(m5, felipe, Feedback.Rating.NOT_USEFUL); matches.giveFeedback(m6, gabi, Feedback.Rating.USEFUL);
        long[] claimed = { rewards.request(ana, snack, key()), rewards.request(samuel, snack, key()), rewards.request(eva, snack, key()), rewards.request(carlos, snack, key()), rewards.request(gabi, snack, key()) };
        sql.update("update reward_claims set status='DELIVERED' where id in (?,?,?)", claimed[0], claimed[1], claimed[2]);
        sql.update("update reward_claims set status='INCIDENT' where id=?", claimed[3]);

        // Event 2 · draft, empty. Event 3 · paused, small, with a door total that is valid.
        long draft = event("Feria de clubes (ficticio)", "");
        long paused = event("Jornada de integración (ficticio)", "");
        events.transition(paused, EventStatus.OPEN);
        long hall = place(paused, "Patio central"); activity(paused, hall, "Dibujo a cuatro manos", 15, CheckMethod.BILATERAL);
        long p1 = person(paused, "Valentina"), p2 = person(paused, "Tomás"); complete(p1, p2); away(p1, p2);
        long p3 = person(paused, "Sara"), p4 = person(paused, "Diego"); complete(p3, p4);
        events.transition(paused, EventStatus.PAUSED);
        admin.recordDoor(paused, 30);

        for (long e : List.of(main, draft, paused)) accounts.assign(general, organizer, e, true, AccountService.EventRole.ORGANIZER);
        accounts.assign(general, staff, main, true, AccountService.EventRole.STAFF);

        System.out.println("""

                ============================================================
                PING · candidato local con DATOS FICTICIOS (H2 en memoria)
                Abre http://127.0.0.1:8096/login  · contraseña de las tres cuentas: %s
                  admin@ping.test          Administración general (/organizer/events)
                  organizacion@ping.test   Organización de los tres eventos (/organizer, /admin)
                  personal@ping.test       Personal de «Bienvenida universitaria» (solo revisar y entregar)
                  participante@ping.test   Participante «Quique», sin match (/me, Actividades abiertas)
                  gabi@ping.test           Participante «Gabi», con un canje pendiente de entregar
                Eventos: %d (abierto) · %d (borrador) · %d (pausado, puerta 30)
                Se detiene solo a los %d minutos o con Ctrl+C.
                ============================================================
                """.formatted(PASSWORD, main, draft, paused, Long.getLong("ping.showcase.minutes", 120)));
        Thread.sleep(Long.getLong("ping.showcase.minutes", 120) * 60_000);
    }
}
