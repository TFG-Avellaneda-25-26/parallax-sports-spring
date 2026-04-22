package dev.parallaxsports.notification.event;

import java.util.List;

public record EventsIngestedEvent(List<Long> eventIds) {}
