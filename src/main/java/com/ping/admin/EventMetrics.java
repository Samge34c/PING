package com.ping.admin;
import com.ping.event.EventStatus;
public record EventMetrics(long id, String name, EventStatus status, long participants, long available, long free,
                           long offered, long active, long completed, long cancelled, long points,
                           long useful, long neutral, long notUseful) {
    public long unavailable() { return participants - available; }
    public long completionPercent() { long total = offered + active + completed + cancelled; return total == 0 ? 0 : Math.round(100.0 * completed / total); }
    public long feedbackCount() { return useful + neutral + notUseful; }
}
