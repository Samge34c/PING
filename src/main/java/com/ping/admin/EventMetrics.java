package com.ping.admin;
import com.ping.event.EventStatus;
public record EventMetrics(long id, String name, EventStatus status, long participants, long available, long free,
                           long offered, long active, long completed, long cancelled, long points,
                           long useful, long neutral, long notUseful) {
    public long feedbackCount() { return useful + neutral + notUseful; }
}
