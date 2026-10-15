package com.ping.admin;
import java.time.Instant;
import java.util.List;
public record EventDetails(String meetingPoint, List<Person> participants, List<Connection> matches, List<Opinion> feedback) {
    public record Person(long id, String alias, List<String> interests, boolean available, long points, long completed) { }
    public record Connection(long id, String first, String second, String type, String status, String word, String activity, Instant createdAt, String result) { }
    public record Opinion(long matchId, String alias, String rating, Instant createdAt) { }
}
