package com.ping.admin;
import com.ping.event.EventStatus;
/**
 * Figures of one event. {@code connectedPeople}: distinct participants (by id) of COMPLETED encounters (DEC-079).
 * {@code door}: optional total that entered through the door, recorded by the organisation; null when not recorded.
 */
public record EventMetrics(long id, String name, EventStatus status, long participants, long available, long free,
                           long offered, long active, long completed, long cancelled, long points,
                           long useful, long neutral, long notUseful, long connectedPeople, Integer door, Claims claims) {
    public enum Door { MISSING, ZERO, VALID, REVIEW }
    /** Receipts of the event by state. {@code requested} = issued and not delivered yet. */
    public record Claims(long requested, long delivered, long incidents, long cancelled) {
        public static final Claims NONE = new Claims(0, 0, 0, 0);
        public long total() { return requested + delivered + incidents + cancelled; }
    }
    public long unavailable() { return participants - available; }
    public long proposals() { return offered + active + completed + cancelled; }
    /** Conversion of connections: completed / all proposals (existing definition). */
    public long completionPercent() { long total = proposals(); return total == 0 ? 0 : Math.round(100.0 * completed / total); }
    public long feedbackCount() { return useful + neutral + notUseful; }
    /** People who completed a connection cannot exceed the people who entered: that pair of numbers needs review, never a capped percentage. */
    public Door doorState() { return door == null ? Door.MISSING : door == 0 ? Door.ZERO : connectedPeople > door ? Door.REVIEW : Door.VALID; }
    /** Unique people with a completed connection / people who entered; only for a valid door total. */
    public Long doorPercent() { return doorState() == Door.VALID ? Math.round(100.0 * connectedPeople / door) : null; }
    /** Width of one proposal state in the distribution bar, in percent of all proposals. */
    public double share(long value) { long total = proposals(); return total == 0 ? 0 : 100.0 * value / total; }
}
