package com.ping.participant;

public record ParticipantView(Long id, String alias, Long eventId, String eventName, boolean available, long version, java.util.List<String> interests) { }
