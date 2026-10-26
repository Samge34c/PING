package com.ping.participant;

import java.time.Instant;
import java.util.*;
import com.ping.interest.Interest;
import jakarta.persistence.*;
import com.ping.event.Event;

@Entity @Table(name = "participants")
public class Participant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 80) private String alias;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "event_id", nullable = false) private Event event;
    @Column(nullable = false) private boolean available;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "waiting_since") private Instant waitingSince;
    @Column(nullable = false) private long points;
    public Instant getWaitingSince() { return waitingSince; }
    public long getPoints() { return points; }
    private void rewardAmount(int amount) { if(amount<0 || amount>100)throw new IllegalArgumentException("Invalid completion amount"); }
    public void rewardCompletedMatch(int amount) { rewardAmount(amount);points = Math.addExact(points, amount); }
    public void reverseCompletionReward(int amount) {
        rewardAmount(amount);
        if (points < amount) throw new IllegalStateException("Cannot reverse a reward without its balance");
        points -= amount;
    }
    public void spendPoints(long cost) {
        if (cost <= 0 || points < cost) throw new com.ping.web.BusinessRuleException("No tienes puntos suficientes para este beneficio.");
        points -= cost;
    }
    public void restartWaiting() { waitingSince = available ? Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS) : null; }
    @Column(name = "recognition_hint", length = 120) private String recognitionHint;
    public String getRecognitionHint() { return recognitionHint; }
    void changeRecognitionHint(String hint) { recognitionHint = hint; }
    @Column(nullable=false,length=16) private String avatarAnimal="gato";
    @Column(nullable=false,length=16) private String avatarHat="none";
    @Column(nullable=false,length=16) private String avatarAccessory="none";
    @Column(nullable=false,length=16) private String avatarBackground="violeta";
    // DEC-058: estilo del avatar. "preset" muestra el catálogo anterior (valores conservados); "v3" muestra las columnas avatar_v3_*.
    @Column(name="avatar_style",nullable=false,length=8) private String avatarStyle="preset";
    @Column(name="avatar_v3_animal",length=16) private String v3Animal;
    @Column(name="avatar_v3_palette",length=16) private String v3Palette;
    @Column(name="avatar_v3_head",length=16) private String v3Head;
    @Column(name="avatar_v3_head_color",length=16) private String v3HeadColor;
    @Column(name="avatar_v3_audio",length=16) private String v3Audio;
    @Column(name="avatar_v3_audio_color",length=16) private String v3AudioColor;
    @Column(name="avatar_v3_face",length=16) private String v3Face;
    @Column(name="avatar_v3_face_color",length=16) private String v3FaceColor;
    @Column(name="avatar_v3_neck",length=16) private String v3Neck;
    @Column(name="avatar_v3_neck_color",length=16) private String v3NeckColor;
    public Avatar getAvatar(){var preset=new Avatar(avatarAnimal,avatarHat,avatarAccessory,avatarBackground);return "v3".equals(avatarStyle)&&v3Animal!=null?preset.withV3(new AvatarV3(v3Animal,v3Palette,v3Head,v3HeadColor,v3Audio,v3AudioColor,v3Face,v3FaceColor,v3Neck,v3NeckColor)):preset;}
    /** Guarda los campos preset y, si llega un avatar V3, pasa a mostrarlo; un avatar sin V3 vuelve al estilo preset sin borrar el último V3 guardado. */
    public void avatar(Avatar a){avatarAnimal=a.animal();avatarHat=a.hat();avatarAccessory=a.accessory();avatarBackground=a.background();
        var v=a.v3();if(v==null){avatarStyle="preset";return;}
        avatarStyle="v3";v3Animal=v.animal();v3Palette=v.palette();v3Head=v.head();v3HeadColor=v.headColor();v3Audio=v.audio();v3AudioColor=v.audioColor();v3Face=v.face();v3FaceColor=v.faceColor();v3Neck=v.neck();v3NeckColor=v.neckColor();}
    @Version private long version;
    @ManyToMany
    @JoinTable(name = "participant_interests", joinColumns = @JoinColumn(name = "participant_id"), inverseJoinColumns = @JoinColumn(name = "interest_id"))
    private Set<Interest> interests = new LinkedHashSet<>();
    protected Participant() { }
    public Participant(String alias, Event event, boolean available) { this.alias = alias; this.event = event; this.available = available; }
    @PrePersist void onCreate() { createdAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS); if (available) waitingSince = createdAt; }
    public Long getId() { return id; }
    public String getAlias() { return alias; }
    public Event getEvent() { return event; }
    public boolean isAvailable() { return available; }
    public Instant getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
    public Set<Interest> getInterests() { return Collections.unmodifiableSet(interests); }
    public void replaceInterests(Collection<Interest> selected) { interests.clear(); interests.addAll(selected); }
    void changeAvailability(boolean available) { if (this.available != available) { this.available = available; restartWaiting(); } }
}
