package dev.parallaxsports.sport.event.dto;

import java.util.List;

public record EventFeedResponse(
    List<EventResponse> items,
    Long nextCursor,
    boolean hasMore
) {}
