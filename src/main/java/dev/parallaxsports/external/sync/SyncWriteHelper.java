package dev.parallaxsports.external.sync;

import dev.parallaxsports.sport.model.Competition;
import dev.parallaxsports.sport.model.Event;
import dev.parallaxsports.sport.model.Season;
import dev.parallaxsports.sport.model.Venue;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Shared helpers used by both Formula 1 and Basketball sync write services.
 */
public final class SyncWriteHelper {

    private SyncWriteHelper() {}

    /**
     * Updates a field only when the new value differs from the current one.
     *
     * @return true when the setter was called
     */
    public static <T> boolean setIfChanged(T current, T next, Consumer<T> setter) {
        if (Objects.equals(current, next)) {
            return false;
        }
        setter.accept(next);
        return true;
    }

    /**
     * Returns the fallback when value is null or blank.
     */
    public static String nullSafe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * Returns the fallback when value is null.
     */
    public static long nullSafe(Long value, long fallback) {
        return value == null ? fallback : value;
    }

    /**
     * Compares two entities by their database id to avoid false positives
     * from detached JPA instances. Supports Competition, Season, Event, and Venue.
     *
     * @return true when both represent the same persisted entity, or both are null
     */
    public static boolean sameEntityId(Object left, Object right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (!left.getClass().equals(right.getClass())) {
            return false;
        }
        Long leftId = readEntityId(left);
        Long rightId = readEntityId(right);
        if (leftId == null && rightId == null) {
            return Objects.equals(left, right);
        }
        return Objects.equals(leftId, rightId);
    }

    private static Long readEntityId(Object entity) {
        if (entity instanceof Competition c) {
            return c.getId();
        }
        if (entity instanceof Season s) {
            return s.getId();
        }
        if (entity instanceof Event e) {
            return e.getId();
        }
        if (entity instanceof Venue v) {
            return v.getId();
        }
        return null;
    }
}
