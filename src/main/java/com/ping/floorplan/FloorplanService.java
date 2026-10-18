package com.ping.floorplan;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import com.ping.event.*;
import com.ping.encounter.*;
import com.ping.web.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Service @Profile("!bootstrap") @Transactional(readOnly = true)
public class FloorplanService {
    private final JdbcTemplate sql;
    private final EventService events;
    private final MeetingPointRepository points;
    private final FloorplanImageCodec codec;
    public FloorplanService(JdbcTemplate sql, EventService events, MeetingPointRepository points, FloorplanImageCodec codec) {
        this.sql = sql; this.events = events; this.points = points; this.codec = codec;
    }
    public record Plan(String id, long eventId, String filename, int width, int height, String contentType, int byteSize, Instant createdAt, long revision) { }
    public record Position(long pointId, double x, double y) { }
    public record Point(long id, String name, String directions, boolean active, Double x, Double y) {
        public boolean placed() { return x != null && y != null; }
    }
    public record Snapshot(String planId, Long pointId, Double x, Double y) { }
    public Plan plan(long eventId, String id) {
        return sql.query("select id,event_id,filename,width,height,content_type,byte_size,created_at,revision from event_floorplans where event_id=? and id=?",
            (r, n) -> new Plan(r.getString(1), r.getLong(2), r.getString(3), r.getInt(4), r.getInt(5), r.getString(6), r.getInt(7), r.getTimestamp(8).toInstant(), r.getLong(9)), eventId, id)
            .stream().findFirst().orElseThrow(() -> new NotFoundException("No encontramos ese plano en este evento."));
    }
    public Plan current(long eventId) {
        String id = events.get(eventId).getFloorplanId(); return id == null ? null : plan(eventId, id);
    }
    public List<Position> positions(String id) {
        return sql.query("select point_id,x,y from event_floorplan_positions where plan_id=? order by point_id", (r,n) -> new Position(r.getLong(1),r.getDouble(2),r.getDouble(3)), id);
    }
    public List<Point> points(long eventId) {
        var plan = current(eventId); var positions = plan == null ? List.<Position>of() : positions(plan.id());
        return points.findByEventIdOrderByIdAsc(eventId).stream().map(p -> {
            var position = positions.stream().filter(pos -> pos.pointId() == p.getId()).findFirst().orElse(null);
            return new Point(p.getId(), p.getName(), p.getDirections(), p.isActive(), position == null ? null : position.x(), position == null ? null : position.y());
        }).toList();
    }
    public byte[] image(long eventId, String planId) {
        plan(eventId, planId);
        return sql.query("select image_data from event_floorplan_images where plan_id=?", (r,n) -> r.getBytes(1), planId).stream().findFirst().orElseThrow(() -> new NotFoundException("El recurso del plano no está disponible."));
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public String upload(long eventId, String expectedId, boolean replace, MultipartFile file) {
        // Decode before taking the event lock; validate the expected pointer again under it.
        var image = codec.decode(file); var event = events.lock(eventId);
        String expected = expectedId == null || expectedId.isBlank() ? null : expectedId;
        if (!Objects.equals(expected, event.getFloorplanId())) throw new BusinessRuleException("El plano cambió. Recarga la página antes de sustituirlo.");
        if (event.getFloorplanId() != null && !replace) throw new BusinessRuleException("Confirma Sustituir plano: la nueva imagen necesitará recolocar sus puntos.");
        String id = UUID.randomUUID().toString();
        sql.update("insert into event_floorplans(id,event_id,filename,width,height,content_type,byte_size,created_at,revision) values(?,?,?,?,?,?,?,?,0)",
            id,eventId,image.filename(),image.width(),image.height(),image.contentType(),image.bytes().length,Timestamp.from(Instant.now()));
        sql.update("insert into event_floorplan_images(plan_id,image_data) values(?,?)",id,image.bytes());
        event.useFloorplan(id); return id;
    }
    private Plan editing(long eventId, String planId, long revision, long pointId) {
        var event = events.lock(eventId);
        if (!Objects.equals(event.getFloorplanId(),planId)) throw new BusinessRuleException("El plano fue sustituido. Recarga y recoloca el punto sobre la imagen actual.");
        var plan = plan(eventId,planId);
        if (plan.revision() != revision) throw new BusinessRuleException("Las posiciones cambiaron. Recarga el plano antes de guardar.");
        var point = points.findById(pointId).orElseThrow(() -> new NotFoundException("No encontramos ese punto."));
        if (point.getEventId() != eventId) throw new BusinessRuleException("Elige un punto existente de este evento.");
        return plan;
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void place(long eventId, String planId, long revision, long pointId, double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || x < 0 || x > 1 || y < 0 || y > 1) throw new BusinessRuleException("La posición horizontal y vertical deben estar entre 0 y 1.");
        editing(eventId,planId,revision,pointId);
        sql.update("delete from event_floorplan_positions where plan_id=? and point_id=?",planId,pointId);
        sql.update("insert into event_floorplan_positions(plan_id,point_id,x,y) values(?,?,?,?)",planId,pointId,x,y);
        sql.update("update event_floorplans set revision=revision+1 where id=?",planId);
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void remove(long eventId, String planId, long revision, long pointId) {
        editing(eventId,planId,revision,pointId);
        sql.update("delete from event_floorplan_positions where plan_id=? and point_id=?",planId,pointId);
        sql.update("update event_floorplans set revision=revision+1 where id=?",planId);
    }
    // Caller holds the event lock used by upload/place/remove, so activation sees one coherent version.
    @Transactional(propagation = Propagation.MANDATORY)
    public Snapshot snapshot(Event event, Long pointId) {
        if (event.getFloorplanId() == null) return new Snapshot(null,pointId,null,null);
        var plan = plan(event.getId(),event.getFloorplanId());
        var position = positions(plan.id()).stream().filter(p -> Objects.equals(p.pointId(),pointId)).findFirst().orElse(null);
        return new Snapshot(plan.id(),pointId,position == null ? null : position.x(),position == null ? null : position.y());
    }
}
