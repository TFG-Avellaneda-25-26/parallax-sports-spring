package dev.parallaxsports.core.util;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class DateUtils {

    private DateUtils() {
    }

    public static OffsetDateTime toUtc(OffsetDateTime dateTime) {
        return dateTime.withOffsetSameInstant(ZoneId.of("UTC").getRules().getOffset(dateTime.toInstant()));
    }
}
