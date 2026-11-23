package com.ping.admin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import com.ping.PingApplication;
import com.ping.auth.AccountService;
import com.ping.encounter.*;
import com.ping.event.*;
import com.ping.match.*;
import com.ping.participant.*;
import com.ping.review.ActivityReviewService;
import com.ping.reward.*;
import com.ping.verification.CredentialService;
import com.ping.web.BusinessRuleException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.*;

/**
 * DEC-079 + DEC-081 · manual MySQL check of the V26 → V27 → V27.1 upgrade (the service runs V26) (not part of the default suite: the class name does not end in
 * "Tests"). Run only against a NEW, EMPTY exclusive database on this computer, never Aiven or production:
 *   PING_MYSQL_EXCLUSIVE_URL=jdbc:mysql://127.0.0.1:3323/ping_v27_exclusive mvnw -Dtest=DoorAttendanceUpgradeMySqlCheck test
 *
 * 1. Starts PING with Flyway stopped at V26 (entities validated against that schema) and fills it with FICTITIOUS data
 *    through the real services: completed encounters (Saga), a staff review, redemptions, opinions, an earlier
 *    «al entregar» activity and a paused event.
 * 2. Takes a fingerprint of every table (all rows, V26 columns only) and of the schema.
 * 3. Applies the pending migrations with Flyway, as a deployment would, and compares: only events.door_attendance,
 *    the empty table activity_qr_validations and the widened purpose CHECK are new, every row and balance is identical,
 *    V1–V26 history is untouched and old events have no door total. The widened CHECK accepts ACTIVITY only.
 * 4. Starts PING again on the upgraded schema (validate) and checks results, the door total and, for the pending DEC-079
 *    rule, that a NEW activity with points is still credited today by both confirmations alone.
 */
class DoorAttendanceUpgradeMySqlCheck {
    static final String URL = System.getenv("PING_MYSQL_EXCLUSIVE_URL");
    static final String USER = Objects.requireNonNullElse(System.getenv("PING_MYSQL_EXCLUSIVE_USER"), "root");
    static final String PASSWORD = Objects.requireNonNullElse(System.getenv("PING_MYSQL_EXCLUSIVE_PASSWORD"), "");

    record Snapshot(Map<String, String> tables, Map<String, Integer> rows, Map<String, List<String>> columns, Set<String> schema) { }
    static final java.util.regex.Pattern LOCAL = java.util.regex.Pattern.compile("^jdbc:mysql://(?:127\\.0\\.0\\.1|localhost):\\d{2,5}/([A-Za-z0-9_]+_exclusive)(?:\\?[^,/]*)?$");

    ConfigurableApplicationContext app(String... extra) {
        // Command-line arguments: they take precedence over application.properties (ddl-auto=none), environment variables and system properties.
        var args = new ArrayList<>(List.of("--spring.datasource.url=" + URL, "--spring.datasource.username=" + USER, "--spring.datasource.password=" + PASSWORD,
                "--spring.jpa.hibernate.ddl-auto=validate", "--server.port=0", "--spring.main.banner-mode=off"));
        for (String e : extra) args.add("--" + e);
        var ctx = new SpringApplicationBuilder(PingApplication.class).profiles("verification-test").web(WebApplicationType.SERVLET).run(args.toArray(String[]::new));
        assertThat(ctx.getEnvironment().getProperty("spring.datasource.url")).as("effective datasource").isEqualTo(URL);
        assertThat(ctx.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto")).as("entities validated against the schema").isEqualTo("validate");
        assertThat(ctx.getBean(JdbcTemplate.class).queryForObject("select database()", String.class)).isEqualTo(dbName());
        return ctx;
    }

    static String dbName() {
        var m = LOCAL.matcher(Objects.requireNonNullElse(URL, ""));
        if (!m.matches()) throw new IllegalStateException("Use a NEW exclusive MySQL database on this computer only (jdbc:mysql://127.0.0.1:<port>/<name>_exclusive).");
        return m.group(1);
    }

    @Test void v26DatabaseUpgradesToV27WithoutTouchingBalancesOrHistory() throws Exception {
        String schema = dbName();
        var raw = new JdbcTemplate(new DriverManagerDataSource(URL, USER, PASSWORD));
        assertThat(raw.queryForObject("select database()", String.class)).isEqualTo(schema);
        assertThat(raw.queryForObject("select count(*) from information_schema.tables where table_schema=?", Integer.class, schema)).as("the database must start empty").isZero();

        // 1 · PING at V26 with fictitious data
        Map<String, Long> ids;
        try (var ctx = app("spring.flyway.target=26")) {
            assertThat(raw.queryForObject("select max(cast(version as unsigned)) from flyway_schema_history where success", Integer.class)).isEqualTo(26);
            ids = new Seed(ctx).fill();
        }
        var before = snapshot(raw, schema, null);
        var balancesBefore = raw.queryForList("select id, alias, points from participants order by id");
        var ledgerBefore = raw.queryForList("select action, count(*) n from match_completion_steps group by action order by action");
        int historyRows = raw.queryForObject("select count(*) from flyway_schema_history", Integer.class);

        // 2 · the upgrade, exactly as Spring Boot runs it at start-up (default location classpath:db/migration)
        long t0 = System.nanoTime();
        var result = Flyway.configure().dataSource(URL, USER, PASSWORD).load().migrate();
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).as("V27 and V27.1 (V28 stays reserved for the DEC-079 rule)").isEqualTo(2);
        assertThat(result.targetSchemaVersion).isEqualTo("27.1");

        // 3 · comparison
        var after = snapshot(raw, schema, before);
        var added = new TreeSet<>(after.schema()); added.removeAll(before.schema());
        var removed = new TreeSet<>(before.schema()); removed.removeAll(after.schema());
        assertThat(added).as("V27: one new column").contains("column events.door_attendance int nullable=YES default=null collation=null extra= generated=");
        assertThat(added).as("V27.1: only the new table and the widened purpose CHECK").allMatch(e -> e.startsWith("column events.door_attendance ")
                || e.contains("activity_qr_validations") || e.startsWith("check chk_credential_purpose ") && e.contains("ACTIVITY"));
        assertThat(added).anyMatch(e -> e.startsWith("table activity_qr_validations "));
        assertThat(removed).as("only the previous purpose CHECK clause was replaced").singleElement().matches(e -> e.startsWith("check chk_credential_purpose ") && !e.contains("ACTIVITY"));
        assertThat(raw.queryForObject("select count(*) from activity_qr_validations", Integer.class)).as("new table starts empty").isZero();
        // The widened CHECK: ACTIVITY is accepted, anything else is still rejected.
        var anyone = raw.queryForMap("select id, event_id from participants order by id limit 1");
        raw.update("insert into purpose_credentials(id,issue_key,purpose,context_id,event_id,issuer_id,token_hash,manual_hash,expires_at) values('chk-activity','chk-activity','ACTIVITY',0,?,?,'chk-a','chk-a',now(6))", anyone.get("event_id"), anyone.get("id"));
        assertThatThrownBy(() -> raw.update("insert into purpose_credentials(id,issue_key,purpose,context_id,event_id,issuer_id,token_hash,manual_hash,expires_at) values('chk-other','chk-other','OTHER',0,?,?,'chk-o','chk-o',now(6))", anyone.get("event_id"), anyone.get("id"))).isInstanceOf(org.springframework.dao.DataAccessException.class);
        raw.update("delete from purpose_credentials where id='chk-activity'");
        assertThat(after.rows()).as("row counts per table (history counted up to V26)").isEqualTo(before.rows());
        assertThat(after.tables()).as("every row identical, column by column").isEqualTo(before.tables());
        assertThat(raw.queryForList("select id, alias, points from participants order by id")).as("balances").isEqualTo(balancesBefore);
        assertThat(raw.queryForList("select action, count(*) n from match_completion_steps group by action order by action")).as("Saga ledger").isEqualTo(ledgerBefore);
        assertThat(raw.queryForObject("select count(*) from events where door_attendance is not null", Integer.class)).as("no back-fill").isZero();
        assertThat(raw.queryForObject("select count(*) from flyway_schema_history", Integer.class)).isEqualTo(historyRows + 2);

        // 4 · PING on the upgraded schema
        String rule;
        try (var ctx = app()) {
            var admin = ctx.getBean(AdminService.class);
            var main = admin.metrics().stream().filter(m -> m.id() == ids.get("main")).findFirst().orElseThrow();
            assertThat(main.door()).isNull();
            assertThat(main.doorState()).isEqualTo(EventMetrics.Door.MISSING);
            assertThat(main.completed()).isEqualTo(4);
            assertThat(main.connectedPeople()).isEqualTo(7);
            admin.recordDoor(ids.get("main"), 70);
            var recorded = admin.metrics().stream().filter(m -> m.id() == ids.get("main")).findFirst().orElseThrow();
            assertThat(recorded.doorState()).isEqualTo(EventMetrics.Door.VALID);
            assertThat(recorded.doorPercent()).isEqualTo(10);
            rule = new Seed(ctx).newActivityWithPointsAndBothConfirmations();
        }
        System.out.printf("""

                ===== DEC-079 + DEC-081 · V26 → V27 → V27.1 en MySQL aislada (%s) =====
                Migraciones aplicadas: %d (V27 y V27.1) en %d ms · esquema %s → %s
                Tablas comparadas: %d · filas: %d · idénticas columna a columna: sí · elementos de esquema comparados: %d
                Elementos nuevos: %s
                Saldos antes = después: %s
                Registro de la Saga antes = después: %s
                Historial Flyway V1–V26 intacto: sí (%d filas + V27 + V27.1) · activity_qr_validations vacía · CHECK admite ACTIVITY y rechaza otros
                Eventos anteriores con total de puerta: 0 (sin relleno)
                Después: PING arranca con validate, Resultados lee el evento anterior («sin registrar») y guarda 70 → 10 %%
                Regla pendiente: %s
                ==========================================================
                """, raw.queryForObject("select version()", String.class), result.migrationsExecuted, ms, result.initialSchemaVersion, result.targetSchemaVersion,
                before.tables().size(), before.rows().values().stream().mapToInt(Integer::intValue).sum(), before.schema().size(), added, balancesBefore, ledgerBefore, historyRows, rule);
    }

    /** All base tables: row count and a SHA-256 of every row (sorted), reading only the columns that existed at V26. */
    static Snapshot snapshot(JdbcTemplate raw, String schema, Snapshot reference) throws Exception {
        // Schema: tables and options, columns (full type, length, unsigned, collation, generated), indexes and constraints.
        var schemaRows = new TreeSet<String>(); var columns = new TreeMap<String, List<String>>();
        raw.query("select table_name, engine, table_collation, create_options from information_schema.tables where table_schema=?", rs -> {
            schemaRows.add("table " + rs.getString(1) + " engine=" + rs.getString(2) + " collation=" + rs.getString(3) + " options=" + rs.getString(4)); }, schema);
        raw.query("select table_name, column_name, column_type, is_nullable, column_default, collation_name, extra, generation_expression from information_schema.columns where table_schema=? order by table_name, ordinal_position", rs -> {
            columns.computeIfAbsent(rs.getString(1), k -> new ArrayList<>()).add(rs.getString(2));
            schemaRows.add("column " + rs.getString(1) + "." + rs.getString(2) + " " + rs.getString(3) + " nullable=" + rs.getString(4) + " default=" + rs.getString(5)
                    + " collation=" + rs.getString(6) + " extra=" + rs.getString(7) + " generated=" + rs.getString(8)); }, schema);
        raw.query("select table_name, index_name, non_unique, seq_in_index, column_name, sub_part from information_schema.statistics where table_schema=?", rs -> {
            schemaRows.add("index " + rs.getString(1) + "." + rs.getString(2) + " unique=" + (rs.getInt(3) == 0) + " #" + rs.getInt(4) + " " + rs.getString(5) + " part=" + rs.getString(6)); }, schema);
        raw.query("select table_name, constraint_name, constraint_type from information_schema.table_constraints where table_schema=?", rs -> {
            schemaRows.add("constraint " + rs.getString(1) + "." + rs.getString(2) + " " + rs.getString(3)); }, schema);
        raw.query("select constraint_name, check_clause from information_schema.check_constraints where constraint_schema=?", rs -> {
            schemaRows.add("check " + rs.getString(1) + " " + rs.getString(2)); }, schema);
        raw.query("select constraint_name, table_name, referenced_table_name, update_rule, delete_rule from information_schema.referential_constraints where constraint_schema=?", rs -> {
            schemaRows.add("fk " + rs.getString(2) + "." + rs.getString(1) + " -> " + rs.getString(3) + " update=" + rs.getString(4) + " delete=" + rs.getString(5)); }, schema);
        var tables = new TreeMap<String, String>(); var rows = new TreeMap<String, Integer>();
        for (String table : raw.queryForList("select table_name from information_schema.tables where table_schema=? and table_type='BASE TABLE'", String.class, schema)) {
            var names = (reference == null ? columns : reference.columns()).get(table);
            if (names == null && reference != null && table.equals("activity_qr_validations")) continue; // V27.1, checked empty separately
            if (names == null) throw new IllegalStateException("table not present at V26: " + table);
            String where = table.equals("flyway_schema_history") ? " where cast(version as unsigned) <= 26" : "";
            var lines = new ArrayList<String>();
            raw.query("select `" + String.join("`,`", names) + "` from `" + table + "`" + where, rs -> {
                var b = new StringBuilder();
                // Unambiguous: NULL marker, then type tag + length prefix for every value.
                for (int i = 1; i <= names.size(); i++) {
                    Object o = rs.getObject(i);
                    String v = o == null ? null : o instanceof byte[] bytes ? "B" + Base64.getEncoder().encodeToString(bytes) : "S" + o;
                    b.append(v == null ? "N;" : v.length() + ":" + v);
                }
                lines.add(b.toString());
            });
            Collections.sort(lines);
            var digest = MessageDigest.getInstance("SHA-256");
            for (String line : lines) digest.update((line + "\n").getBytes(StandardCharsets.UTF_8));
            tables.put(table, HexFormat.of().formatHex(digest.digest())); rows.put(table, lines.size());
        }
        return new Snapshot(tables, rows, columns, schemaRows);
    }

    /** Fictitious data created through PING's own services (the same code paths as the event). */
    static class Seed {
        final EventService events; final EncounterService config; final ParticipantService people; final MatchingService matching; final MatchService matches;
        final CredentialService credentials; final AccountService accounts; final ActivityReviewService reviews; final RewardService rewards; final BenefitAdminService benefits;
        final JdbcTemplate sql; final PasswordEncoder encoder;
        Seed(ConfigurableApplicationContext c) {
            events = c.getBean(EventService.class); config = c.getBean(EncounterService.class); people = c.getBean(ParticipantService.class); matching = c.getBean(MatchingService.class);
            matches = c.getBean(MatchService.class); credentials = c.getBean(CredentialService.class); accounts = c.getBean(AccountService.class); reviews = c.getBean(ActivityReviewService.class);
            rewards = c.getBean(RewardService.class); benefits = c.getBean(BenefitAdminService.class); sql = c.getBean(JdbcTemplate.class); encoder = c.getBean(PasswordEncoder.class);
        }
        String key() { return UUID.randomUUID().toString(); }
        long account() { String email = key() + "@ficticio.test"; sql.update("insert into ping_accounts(email,password_hash,email_verified) values(?,?,true)", email, encoder.encode("Ensayo-ficticio-2026")); return accounts.find(email).orElseThrow(); }
        long event(String name) { var f = new EventForm(); f.setName(name + " (ficticio)"); return events.create(f).getId(); }
        long person(long event, String alias) { var f = new ParticipantForm(); f.setAlias(alias); f.setEventId(event); f.setAvailable(true); long id = people.create(f).id(); accounts.bind(account(), id, event); return id; }
        long place(long event) { var p = new PointForm(); p.setName("Patio"); p.setDirections("Junto al letrero"); return config.savePoint(event, null, p); }
        long activity(long event, long point, String title, int points, CheckMethod method) {
            var a = new ActivityForm(); a.setTitle(title); a.setInstructions("Actividad breve de ejemplo."); a.setRewardPoints(points); a.setPointIds(Set.of(point)); a.setVerificationMethod(method);
            if (method == CheckMethod.STAFF_REVIEW) { a.setCheckInstructions("Que cuenten qué canción eligió cada persona."); a.setResponsePrompt("¿Qué canciones eligieron?"); }
            return config.saveActivity(event, null, a);
        }
        void toggle(long event, long activity, boolean on) { var f = config.activityForm(event, activity); f.setActive(on); config.saveActivity(event, activity, f); }
        long active(long x, long y) { long m = matching.search(x); matches.accept(m, x); matches.accept(m, y); return m; }
        void verified(long m, long x, long y) {
            matches.arrive(m, x); matches.arrive(m, y);
            var cx = credentials.encounter(m, x, key()); var cy = credentials.encounter(m, y, key());
            credentials.verify(m, x, cy.manual(), "upgrade-check"); credentials.verify(m, y, cx.manual(), "upgrade-check");
        }
        long complete(long x, long y) { long m = active(x, y); verified(m, x, y); matches.confirm(m, x); matches.confirm(m, y); return m; }
        void away(long... ids) { for (long id : ids) sql.update("update participants set available=false where id=?", id); }

        Map<String, Long> fill() {
            accounts.bootstrap("admin@ping.test", "Ensayo-ficticio-2026");
            long general = accounts.find("admin@ping.test").orElseThrow(), host = account();
            long main = event("Bienvenida universitaria"); events.transition(main, EventStatus.OPEN);
            accounts.assign(general, host, main, true, AccountService.EventRole.STAFF);
            long zone = place(main);
            long coop = activity(main, zone, "Ronda cooperativa", 10, CheckMethod.BILATERAL);
            var bf = new BenefitForm(); bf.setName("Snack del evento"); bf.setDescription("Un snack por persona"); bf.setPointsCost(10); bf.setClaimLocation("Mesa amarilla"); bf.setClaimInstructions("Junto a la entrada.");
            long snack = benefits.save(main, null, bf);
            long ana = person(main, "Ana"), samuel = person(main, "Samuel"); long m1 = complete(ana, samuel); away(samuel);
            long julian = person(main, "Julián"); long m2 = complete(ana, julian); away(ana, julian);
            long carlos = person(main, "Carlos"), dana = person(main, "Dana"); complete(carlos, dana); away(carlos, dana);
            long eva = person(main, "Eva"), felix = person(main, "Félix"); long cancelled = matching.search(eva); matches.cancel(cancelled, eva); away(eva, felix);
            // A staff-reviewed encounter, approved and credited once (DEC-066).
            toggle(main, coop, false);
            activity(main, zone, "Reto de canciones", 20, CheckMethod.STAFF_REVIEW);
            long olga = person(main, "Olga"), pablo = person(main, "Pablo"); long reviewed = active(olga, pablo); verified(reviewed, olga, pablo);
            reviews.submit(reviewed, olga, "Ella eligió Clocks y él Yellow.");
            var user = accounts.user(host); SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
            matches.confirm(reviewed, olga); matches.confirm(reviewed, pablo);
            try { reviews.decide(main, reviewed, true, ""); } finally { SecurityContextHolder.clearContext(); }
            matches.completeAfterReview(reviewed);
            toggle(main, coop, true);
            // An activity stored with the earlier «al entregar» method (DEC-068).
            var legacy = new ActivityForm(); legacy.setTitle("Foto con la mascota"); legacy.setInstructions("Actividad anterior."); legacy.setRewardPoints(20); legacy.setPointIds(Set.of(zone));
            legacy.setVerificationMethod(CheckMethod.STAFF_REVIEW); legacy.setCheckInstructions("Una foto de los dos"); legacy.setActive(false);
            sql.update("update event_activities set verification_method='AT_DELIVERY' where id=?", config.saveActivity(main, null, legacy));
            sql.update("update participants set available=true where id in (?,?)", ana, carlos);
            matches.giveFeedback(m1, ana, Feedback.Rating.USEFUL); matches.giveFeedback(m2, julian, Feedback.Rating.NEUTRAL);
            long c1 = rewards.request(ana, snack, key()); rewards.request(carlos, snack, key());
            sql.update("update reward_claims set status='DELIVERED' where id=?", c1);
            long paused = event("Jornada de integración"); events.transition(paused, EventStatus.OPEN); events.transition(paused, EventStatus.PAUSED);
            event("Feria de clubes");
            return Map.of("main", main, "paused", paused);
        }

        /** Today's rule, measured: a NEW activity with points, created after the upgrade, credited by both confirmations alone. */
        String newActivityWithPointsAndBothConfirmations() {
            long e = event("Actividad nueva"); events.transition(e, EventStatus.OPEN);
            long p = place(e);
            var delivery = new ActivityForm(); delivery.setTitle("Mascota"); delivery.setInstructions("Búsquenla."); delivery.setRewardPoints(20); delivery.setPointIds(Set.of(p));
            delivery.setVerificationMethod(CheckMethod.AT_DELIVERY); delivery.setCheckInstructions("Foto");
            assertThatThrownBy(() -> config.saveActivity(e, null, delivery)).isInstanceOf(BusinessRuleException.class);
            long a = activity(e, p, "Canción compartida", 15, CheckMethod.BILATERAL);
            long x = person(e, "Iván"), y = person(e, "Juana");
            long m = complete(x, y);
            long px = people.get(x).points(), py = people.get(y).points();
            int grants = sql.queryForObject("select count(*) from match_completion_steps where match_id=? and action in ('GRANT_FIRST','GRANT_SECOND')", Integer.class, m);
            assertThat(grants).isEqualTo(2);
            assertThat(px).isEqualTo(15); assertThat(py).isEqualTo(15);
            return "actividad nueva con 15 puntos y «Confirmación de ambos» (id " + a + "): sin revisión del personal, la Saga acreditó " + px + " + " + py
                    + " puntos canjeables (" + grants + " pasos GRANT). «Al entregar» nuevo: rechazado.";
        }
    }
}
