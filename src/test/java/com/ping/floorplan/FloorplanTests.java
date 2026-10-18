package com.ping.floorplan;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import javax.imageio.ImageIO;
import com.ping.encounter.*;
import com.ping.event.*;
import com.ping.match.*;
import com.ping.participant.*;
import com.ping.web.BusinessRuleException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:floorplan-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class FloorplanTests {
    @Autowired FloorplanService plans;
    @Autowired FloorplanImageCodec codec;
    @Autowired EventService events;
    @Autowired EncounterService encounters;
    @Autowired ParticipantService people;
    @Autowired MatchingService matching;
    @Autowired MatchService matches;
    long event, point;
    @BeforeEach void setup() {
        EventForm f=new EventForm();f.setName("Plano · Prueba ficticia");event=events.create(f).getId();events.transition(event,EventStatus.OPEN);
        var p=new PointForm();p.setName("Mesa pública");p.setDirections("Junto a la entrada señalizada");point=encounters.savePoint(event,null,p);
        var a=new ActivityForm();a.setTitle("Una recomendación");a.setInstructions("Compartan un libro favorito.");a.setPointIds(Set.of(point));a.setRewardPoints(37);encounters.saveActivity(event,null,a);
    }
    static MockMultipartFile image(String format,int w,int h) throws Exception {
        var bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB),format,bytes);
        return new MockMultipartFile("image","../../plano."+format,"application/octet-stream",bytes.toByteArray());
    }
    String upload() throws Exception {return plans.upload(event,null,false,image("png",400,300));}
    long person(String alias){var f=new ParticipantForm();f.setAlias(alias);f.setEventId(event);f.setAvailable(true);return people.create(f).id();}
    long activate(long a,long b){long id=matching.search(a);matches.accept(id,a);matches.accept(id,b);return id;}
    @Test void acceptsRealFormatsByContentAndNormalizesWithoutUserPaths() throws Exception {
        for(String format:List.of("png","jpeg")) {
            var converted=codec.decode(image(format,200,100));assertThat(converted.width()).isEqualTo(200);assertThat(converted.height()).isEqualTo(100);
            assertThat(converted.filename()).isEqualTo("plano."+format);assertThat(ImageIO.read(new java.io.ByteArrayInputStream(converted.bytes()))).isNotNull();
        }
        byte[] webp=Base64.getDecoder().decode("UklGRlIAAABXRUJQVlA4IEYAAADQAwCdASpAAEAAPm02mEkkIyKhIggAgA2JaQB2AAAg0K4tiFxgFEAA/vPCZ//69H/5lf/rK/4wH/5JHq5jWMEu+D1HAAAA");
        assertThat(codec.decode(new MockMultipartFile("image","plano.webp","image/webp",webp)).width()).isEqualTo(64);
    }
    @Test void rejectsNonImagesOtherFormatsOversizeAndExcessiveDimensions() throws Exception {
        for(String text:List.of("<svg xmlns='http://www.w3.org/2000/svg'/>","<html>Plano</html>","https://example.com/plano.png"))
            assertThatThrownBy(()->codec.decode(new MockMultipartFile("image","fake.png","image/png",text.getBytes()))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(()->codec.decode(image("gif",64,64))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(()->codec.decode(image("png",6001,32))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(()->codec.decode(new MockMultipartFile("image","large.png","image/png",new byte[FloorplanImageCodec.MAX_INPUT+1]))).isInstanceOf(BusinessRuleException.class);
        assertThat(plans.current(event)).isNull();
    }
    @Test void optimizesProportionallyAndPersistsBytesSeparately() throws Exception {
        String id=plans.upload(event,null,false,image("png",4000,1000));var plan=plans.current(event);
        assertThat(plan.width()).isEqualTo(3200);assertThat(plan.height()).isEqualTo(800);assertThat(plan.byteSize()).isLessThanOrEqualTo(FloorplanImageCodec.MAX_STORED);
        assertThat(ImageIO.read(new java.io.ByteArrayInputStream(plans.image(event,id))).getWidth()).isEqualTo(3200);
    }
    @Test void rejectsForeignPointsInvalidCoordinatesAndStaleEditors() throws Exception {
        String id=upload();EventForm other=new EventForm();other.setName("Otro");long foreignEvent=events.create(other).getId();var f=new PointForm();f.setName("Ajeno");f.setDirections("En otro recinto");long foreign=encounters.savePoint(foreignEvent,null,f);
        assertThatThrownBy(()->plans.place(event,id,0,foreign,.2,.3)).isInstanceOf(BusinessRuleException.class);
        for(double bad:List.of(-.01,1.01,Double.NaN,Double.POSITIVE_INFINITY)) assertThatThrownBy(()->plans.place(event,id,0,point,bad,.5)).isInstanceOf(BusinessRuleException.class);
        plans.place(event,id,0,point,0,1);assertThat(plans.points(event).getFirst().placed()).isTrue();
        assertThatThrownBy(()->plans.place(event,id,0,point,.5,.5)).isInstanceOf(BusinessRuleException.class);
        plans.remove(event,id,1,point);assertThat(plans.points(event).getFirst().placed()).isFalse();assertThat(encounters.points(event)).hasSize(1);
    }
    @Test void replacementRequiresExplicitCurrentVersionAndStartsWithoutPositions() throws Exception {
        String id=upload();plans.place(event,id,0,point,.25,.75);
        assertThatThrownBy(()->plans.upload(event,id,false,image("png",400,300))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(()->plans.upload(event,null,true,image("png",400,300))).isInstanceOf(BusinessRuleException.class);
        String next=plans.upload(event,id,true,image("jpeg",400,300));assertThat(next).isNotEqualTo(id);assertThat(plans.positions(next)).isEmpty();assertThat(plans.positions(id)).hasSize(1);assertThat(plans.image(event,id)).isNotEmpty();
        assertThatThrownBy(()->plans.place(event,id,1,point,.8,.8)).isInstanceOf(BusinessRuleException.class);
    }
    @Test void activationSnapshotsActualPointCoordinatesAndVersionWhileNewMatchesUseNewImage() throws Exception {
        String id=upload();plans.place(event,id,0,point,.25,.75);long a=person("Uno"),b=person("Dos"),match=activate(a,b);byte[] old=plans.image(event,id);
        assertThat(matches.get(match,a).floorplanId()).isEqualTo(id);assertThat(matches.get(match,a).floorplanX()).isEqualTo(.25);
        plans.place(event,id,1,point,.8,.2);assertThat(matches.get(match,b).floorplanX()).isEqualTo(.25);
        String next=plans.upload(event,id,true,image("png",500,300));assertThat(matches.get(match,a).floorplanId()).isEqualTo(id);assertThat(plans.image(event,id)).isEqualTo(old);
        long c=person("Tres"),d=person("Cuatro"),fresh=activate(c,d);assertThat(matches.get(fresh,c).floorplanId()).isEqualTo(next);assertThat(matches.get(fresh,c).floorplanX()).isNull();
        plans.place(event,next,0,point,.7,.6);long e=person("Cinco"),f=person("Seis"),placed=activate(e,f);assertThat(matches.get(placed,e).floorplanX()).isEqualTo(.7);
        matches.arrive(match,a);matches.confirm(match,a);matches.confirm(match,b);assertThat(people.get(a).points()).isEqualTo(37);assertThat(people.get(b).points()).isEqualTo(37);
    }
    @Test void encounterWithoutPlanStillActivatesAndDoesNotInventCoordinates(){
        long a=person("Sin plano uno"),b=person("Sin plano dos"),id=activate(a,b);assertThat(matches.get(id,a).floorplanId()).isNull();assertThat(matches.get(id,a).floorplanX()).isNull();assertThat(matches.get(id,a).meetingPoint()).isEqualTo("Mesa pública");
    }
}
