package com.ping.participant;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.ping.auth.AccountService;
import com.ping.event.EventForm;
import com.ping.event.EventService;
import com.ping.event.EventStatus;
import com.ping.support.TestBrowser;
import com.ping.web.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:avatar-v3-tests;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"})
// Como WorkIntegrationTests: H2 y seguridad real, sin la excepción de fixtures del perfil test.
@ActiveProfiles("integration-test")
class AvatarV3Tests {
    @Autowired ParticipantService participants;
    @Autowired EventService events;
    @Autowired AccountService accounts;
    @Autowired PasswordEncoder passwords;
    @Autowired JdbcTemplate sql;
    @Autowired ResourceUrlProvider assetUrls;
    @LocalServerPort int port;

    private static final String PASSWORD = "Clave exclusiva avatar V3 2026";

    private static AvatarV3 from(String[] values) {
        return new AvatarV3(values[0], values[1], values[2], values[3], values[4],
                values[5], values[6], values[7], values[8], values[9]);
    }

    private static String[] completeValues() {
        return new String[]{"nutria", "cacao", "banda", "amarillo", "microfono", "menta",
                "gafas-cuadradas", "coral", "pase", "violeta"};
    }

    static Stream<Arguments> invalidValues() {
        String[] fields = {"animal", "palette", "head", "headColor", "audio", "audioColor",
                "face", "faceColor", "neck", "neckColor"};
        return IntStream.range(0, fields.length).boxed().flatMap(index ->
                Stream.of("fuera-del-catalogo", "../gato").map(value -> Arguments.of(index, fields[index], value)));
    }

    // Cada campo se invalida por separado: ni piezas ajenas al slot ni rutas llegan a los recursos.
    @ParameterizedTest(name = "rechaza {1}={2}")
    @MethodSource("invalidValues")
    void rejectsValuesOutsideEachCatalogue(int index, String field, String value) {
        var values = completeValues();
        values[index] = value;
        assertThatThrownBy(() -> from(values)).as(field).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsPiecesFromAnotherSlot() {
        // Una pieza válida en otro slot también debe ser rechazada.
        for (var entry : Map.of(2, "auriculares", 4, "banda", 6, "pase", 8, "gafas-sol").entrySet()) {
            var values = completeValues();
            values[entry.getKey()] = entry.getValue();
            assertThatThrownBy(() -> from(values)).isInstanceOf(BusinessRuleException.class);
        }
    }

    static Stream<Arguments> conflictingPieces() {
        return Stream.of("gorro", "gorra", "bucket", "vincha").flatMap(head ->
                Stream.of("auriculares", "microfono").map(audio -> Arguments.of(head, audio)));
    }

    // Protege las ocho incompatibilidades y el mensaje que presenta el formulario.
    @ParameterizedTest(name = "conflicto {0} + {1}")
    @MethodSource("conflictingPieces")
    void rejectsBulkyHeadwearWithEitherAudioPiece(String head, String audio) {
        var values = completeValues();
        values[2] = head;
        values[4] = audio;
        assertThatThrownBy(() -> from(values)).isInstanceOf(BusinessRuleException.class)
                .hasMessage(AvatarV3.CONFLICT);
    }

    @Test
    void allowsHeadphonesWithSmallHeadPieces() {
        // Evita ampliar por accidente la incompatibilidad a banda, lazo o pin.
        for (String head : List.of("banda", "lazo", "pin-estrella")) {
            var values = completeValues();
            values[2] = head;
            values[4] = "auriculares";
            var avatar = from(values);
            assertThat(avatar.head()).isEqualTo(head);
            assertThat(avatar.audio()).isEqualTo("auriculares");
        }
    }

    @Test
    void normalizesEveryAbsentPieceColorToViolet() {
        // El color de none es irrelevante: incluso uno ajeno al catálogo se normaliza.
        for (String color : new String[]{"menta", "../gato", "", null}) {
            var avatar = new AvatarV3("gato", "natural", "none", color, "none", color,
                    "none", color, "none", color);
            assertThat(List.of(avatar.headColor(), avatar.audioColor(), avatar.faceColor(), avatar.neckColor()))
                    .containsExactly("violeta", "violeta", "violeta", "violeta");
        }
    }

    @Test
    void paintsRearNeckAndAudioBeforeBaseAndAudioLast() {
        // Orden de pintado con audio: traseras, cuerpo, cuello/cara/cabeza y audio delante.
        assertThat(from(completeValues()).layers()).containsExactly(
                "/img/avatars/v3/rear/nutria-pase-violeta.svg",
                "/img/avatars/v3/rear/nutria-microfono-menta.svg",
                "/img/avatars/v3/base/nutria-cacao.svg",
                "/img/avatars/v3/front/nutria-pase-violeta.svg",
                "/img/avatars/v3/front/nutria-gafas-cuadradas-coral.svg",
                "/img/avatars/v3/front/nutria-banda-amarillo.svg",
                "/img/avatars/v3/front/nutria-microfono-menta.svg");
    }

    @Test
    void paintsRearNeckBeforeRearHeadAndOmitsAbsentAudio() {
        // Vincha tiene trasera propia y no permite audio: requiere un caso independiente.
        var avatar = new AvatarV3("gato", "lila", "vincha", "menta", "none", null,
                "gafas-sol", "amarillo", "pase", "coral");
        assertThat(avatar.layers()).containsExactly(
                "/img/avatars/v3/rear/gato-pase-coral.svg",
                "/img/avatars/v3/rear/gato-vincha-menta.svg",
                "/img/avatars/v3/base/gato-lila.svg",
                "/img/avatars/v3/front/gato-pase-coral.svg",
                "/img/avatars/v3/front/gato-gafas-sol-amarillo.svg",
                "/img/avatars/v3/front/gato-vincha-menta.svg");
    }

    @Test
    void paintsRabbitEarsAfterBucketAndKeepsBareAvatarToOneLayer() {
        // Las orejas del conejo deben sobresalir por delante del bucket.
        var avatar = new AvatarV3("conejo", "perla", "bucket", "coral", "none", null,
                "gafas-redondas", "menta", "bufanda", "amarillo");
        assertThat(avatar.layers()).containsExactly(
                "/img/avatars/v3/base/conejo-perla.svg",
                "/img/avatars/v3/front/conejo-bufanda-amarillo.svg",
                "/img/avatars/v3/front/conejo-gafas-redondas-menta.svg",
                "/img/avatars/v3/front/conejo-bucket-coral.svg",
                "/img/avatars/v3/base/conejo-perla-orejas.svg");
        assertThat(AvatarV3.defaults().layers()).containsExactly("/img/avatars/v3/base/gato-natural.svg");
    }

    @Test
    void everyAnimalPaletteAndPieceColorHasItsActualSvgFiles() {
        // Recorre el catálogo completo, incluidas traseras y orejas por cada pelaje del conejo.
        for (String animal : AvatarV3.ANIMALS.keySet()) {
            for (String palette : AvatarV3.PALETTES.keySet()) {
                assertAssets(new AvatarV3(animal, palette, "none", null, "none", null, "none", null, "none", null));
                var slots = List.of(AvatarV3.HEADS, AvatarV3.AUDIOS, AvatarV3.FACES, AvatarV3.NECKS);
                for (int slot = 0; slot < slots.size(); slot++) {
                    for (String piece : slots.get(slot).keySet()) {
                        for (String color : AvatarV3.COLORS.keySet()) {
                            String[] values = {animal, palette, "none", null, "none", null, "none", null, "none", null};
                            values[2 + slot * 2] = piece;
                            values[3 + slot * 2] = color;
                            assertAssets(from(values));
                        }
                    }
                }
            }
        }
    }

    private void assertAssets(AvatarV3 avatar) {
        for (String layer : avatar.layers()) {
            assertThat(layer).startsWith("/img/avatars/v3/").doesNotContain("..");
            assertThat(Path.of("src/main/resources/static").resolve(layer.substring(1)))
                    .as("SVG para %s: %s", avatar, layer).isRegularFile();
        }
    }

    private long newParticipant() {
        var eventForm = new EventForm();
        eventForm.setName("Avatar V3 ficticio " + UUID.randomUUID());
        long event = events.create(eventForm).getId();
        events.transition(event, EventStatus.OPEN);
        var form = new ParticipantForm();
        form.setAlias("Participante V3");
        form.setEventId(event);
        return participants.create(form).id();
    }

    @Test
    void roundTripsV3WithoutConvertingOrOverwritingPresetFields() {
        // Sin transacción de test: cada lectura vuelve a BD tras el commit del servicio.
        long id = newParticipant();
        assertThat(participants.avatar(id)).isEqualTo(Avatar.defaults());
        assertThat(participants.avatar(id).v3()).isNull();
        assertThat(sql.queryForObject("select avatar_style from participants where id=?", String.class, id))
                .isEqualTo("preset");

        var preset = new Avatar("rana", "corona", "gafas", "crema");
        participants.avatar(id, preset);
        assertThat(participants.avatar(id)).isEqualTo(preset);
        var v3 = from(completeValues());
        participants.avatar(id, preset.withV3(v3));
        assertThat(participants.avatar(id)).isEqualTo(preset.withV3(v3));
        assertThat(participants.get(id).avatar().v3()).isEqualTo(v3);
        assertThat(sql.queryForObject("select avatar_style from participants where id=?", String.class, id))
                .isEqualTo("v3");

        participants.avatar(id, preset);
        assertThat(participants.avatar(id)).isEqualTo(preset);
        assertThat(participants.avatar(id).v3()).isNull();
        assertThat(sql.queryForObject("select avatar_style from participants where id=?", String.class, id))
                .isEqualTo("preset");
    }

    private TestBrowser loggedIn(long participant) throws Exception {
        String email = UUID.randomUUID() + "@avatar-v3.test";
        sql.update("insert into ping_accounts(email,password_hash,email_verified) values(?,?,true)",
                email, passwords.encode(PASSWORD));
        long account = accounts.find(email).orElseThrow();
        accounts.bind(account, participant, participants.get(participant).eventId());
        var browser = new TestBrowser(port);
        try {
            assertThat(browser.get("/login").statusCode()).isEqualTo(200);
            assertThat(browser.post("/login", Map.of("email", email, "password", PASSWORD)).statusCode())
                    .isEqualTo(302);
            // TestBrowser captura _csrf en GET y lo incorpora al siguiente POST.
            assertThat(browser.get("/me").statusCode()).isEqualTo(200);
            return browser;
        } catch (Exception | AssertionError failure) {
            browser.close();
            throw failure;
        }
    }

    private Map<String, String> v3Form(AvatarV3 avatar) {
        var fields = new LinkedHashMap<String, String>();
        fields.put("animal", avatar.animal());
        fields.put("palette", avatar.palette());
        fields.put("head", avatar.head());
        fields.put("headColor", avatar.headColor());
        fields.put("audio", avatar.audio());
        fields.put("audioColor", avatar.audioColor());
        fields.put("face", avatar.face());
        fields.put("faceColor", avatar.faceColor());
        fields.put("neck", avatar.neck());
        fields.put("neckColor", avatar.neckColor());
        return fields;
    }

    @Test
    void v3FormSavesAndMeRendersTheVersionedBaseLayer() throws Exception {
        // Comprueba formulario, sesión propia, persistencia y renderizado con huella de recursos.
        long id = newParticipant();
        var v3 = from(completeValues());
        try (var browser = loggedIn(id)) {
            var response = browser.post("/me/avatar", v3Form(v3));
            assertThat(response.statusCode()).isEqualTo(302);
            assertThat(response.headers().firstValue("Location").orElseThrow()).endsWith("/me");
            assertThat(participants.avatar(id)).isEqualTo(Avatar.defaults().withV3(v3));
            var page = browser.get("/me");
            assertThat(page.statusCode()).isEqualTo(200);
            String base = assetUrls.getForLookupPath("/img/avatars/v3/base/nutria-cacao.svg");
            assertThat(base).isNotBlank();
            assertThat(page.body()).contains(base);
        }
    }

    @Test
    void conflictingHttpFormReturns400AndPreservesTheStoredAvatar() throws Exception {
        // Un rechazo HTTP no puede reemplazar el avatar previamente guardado.
        long id = newParticipant();
        var saved = new Avatar("zorro", "fiesta", "bufanda", "azul").withV3(from(completeValues()));
        participants.avatar(id, saved);
        try (var browser = loggedIn(id)) {
            var fields = v3Form(from(completeValues()));
            fields.put("head", "gorra");
            fields.put("audio", "auriculares");
            var response = browser.post("/me/avatar", fields);
            // Comprueba persistencia primero, incluso si falla el renderizado del error.
            assertThat(participants.avatar(id)).isEqualTo(saved);
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(response.body()).contains(AvatarV3.CONFLICT);
            // El editor recibe la selección persistida por separado: el borrador rechazado no se presenta como guardado.
            assertThat(response.body()).contains("data-saved=\"" + saved.v3().formFields() + "\"");
        }
    }

    @Test
    void presetOwnerSeesTheV3SuggestionAsAnUnsavedProposal() throws Exception {
        // Sin avatar V3 guardado, la sugerencia del editor no puede figurar como avatar persistido.
        long id = newParticipant();
        try (var browser = loggedIn(id)) {
            var page = browser.get("/me/avatar");
            assertThat(page.statusCode()).isEqualTo(200);
            assertThat(page.body()).contains("data-saved=\"none\"");
        }
    }

    @Test
    void legacyHttpFormWithoutPaletteStillSavesPresetAndLeavesV3Style() throws Exception {
        // Un cliente antiguo puede volver de V3 al preset con sus cuatro campos originales.
        long id = newParticipant();
        participants.avatar(id, Avatar.defaults().withV3(from(completeValues())));
        try (var browser = loggedIn(id)) {
            var response = browser.post("/me/avatar", Map.of("animal", "rana", "hat", "corona",
                    "accessory", "gafas", "background", "menta"));
            assertThat(response.statusCode()).isEqualTo(302);
            assertThat(participants.avatar(id)).isEqualTo(new Avatar("rana", "corona", "gafas", "menta"));
            assertThat(participants.avatar(id).v3()).isNull();
            assertThat(browser.get("/me").statusCode()).isEqualTo(200);
        }
    }
}
