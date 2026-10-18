package com.ping.floorplan;

import java.util.*;
import com.ping.encounter.*;
import com.ping.event.*;
import com.ping.support.TestBrowser;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="spring.datasource.url=jdbc:h2:mem:floorplan-web;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class FloorplanWebTests {
    @Autowired FloorplanService plans;@Autowired EventService events;@Autowired EncounterService encounters;
    @LocalServerPort int port;
    long event,point;String page,upload;
    @BeforeEach void setup(){var e=new EventForm();e.setName("Plano ficticio HTTP");event=events.create(e).getId();var p=new PointForm();p.setName("Mesa amarilla");p.setDirections("Junto al letrero de entrada");point=encounters.savePoint(event,null,p);page="/events/"+event+"/floorplan";upload=page+"/image";}
    @Test void multipartRequiresCsrfAndRealImageWhileErrorsKeepExistingResource() throws Exception {
        byte[] image=FloorplanTests.image("png",400,300).getBytes();
        try(var b=new TestBrowser(port)){
            assertThat(b.postFile(upload,Map.of(),"plano.png",image).statusCode()).isEqualTo(403);assertThat(plans.current(event)).isNull();
            assertThat(b.get(page).body()).contains("Aún no hay un plano");
            assertThat(b.postFile(upload,Map.of(),"plano.png",image).statusCode()).isEqualTo(302);String id=plans.current(event).id();
            assertThat(b.postFile(upload,Map.of("expectedPlanId",id,"replace","true"),"fraude.png","<svg/>".getBytes()).statusCode()).isEqualTo(409);
            assertThat(plans.current(event).id()).isEqualTo(id);assertThat(plans.image(event,id)).isNotEmpty();
            var error=b.postFile(upload,Map.of("expectedPlanId",id,"replace","true"),"grande.png",new byte[FloorplanImageCodec.MAX_INPUT+1]);assertThat(error.statusCode()).isEqualTo(409);assertThat(error.body()).contains("8 MiB");assertThat(plans.current(event).id()).isEqualTo(id);
        }
    }
    @Test void positionAndRemovalValidateEventRevisionAndRangeWithoutDeletingPoint() throws Exception {
        try(var b=new TestBrowser(port)){
            b.get(page);b.postFile(upload,Map.of(),"plano.png",FloorplanTests.image("png",400,300).getBytes());String id=plans.current(event).id();
            var data=new HashMap<>(Map.of("planId",id,"revision","0","pointId",""+point,"x",".25","y",".75"));assertThat(b.post(page+"/position",data).statusCode()).isEqualTo(302);
            assertThat(b.post(page+"/position",data).statusCode()).isEqualTo(409);data.put("revision","1");data.put("x","1.1");assertThat(b.post(page+"/position",data).statusCode()).isEqualTo(409);assertThat(plans.positions(id).getFirst().x()).isEqualTo(.25);
            var foreign=new EventForm();foreign.setName("Ajeno");long other=events.create(foreign).getId();var p=new PointForm();p.setName("Punto ajeno");p.setDirections("Otro evento");long foreignPoint=encounters.savePoint(other,null,p);data.put("x",".5");data.put("pointId",""+foreignPoint);assertThat(b.post(page+"/position",data).statusCode()).isEqualTo(409);
            assertThat(b.get("/events/"+other+"/floorplan/images/"+id).statusCode()).isEqualTo(404);
            data.put("pointId",""+point);assertThat(b.post(page+"/remove",data).statusCode()).isEqualTo(302);assertThat(plans.positions(id)).isEmpty();assertThat(encounters.points(event)).hasSize(1);
        }
    }
    @Test void requestBeyondMultipartEnvelopeReturnsRecoverableLimitError() throws Exception {
        try(var b=new TestBrowser(port)){
            b.get(page);var response=b.postFile(upload,Map.of(),"excesivo.png",new byte[11*1024*1024]);
            assertThat(response.statusCode()).isEqualTo(413);assertThat(response.body()).contains("8 MiB","Volver al plano");assertThat(plans.current(event)).isNull();
        }
    }
}
