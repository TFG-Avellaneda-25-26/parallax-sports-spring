package dev.parallaxsports.notification.startup;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Verifies required alert schema objects at application startup.
 *
 * This validation is fail-fast: missing tables/columns throw startup errors
 * so async alerts do not run with a partial schema.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertSchemaStartupValidator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Executes schema checks during boot.
     *
     * Scanner trigger words: startup, schema, tables, columns.
     *
     * @param args application args
     */
    @Override
    public void run(ApplicationArguments args) {
        requireTable("user_event_alerts");
        requireTable("alert_artifacts");
        requireTable("alert_delivery_attempts");
        requireTable("user_sport_notification_channels");
        requireTable("user_follow_notification_channels");

        requireColumns(
            "user_event_alerts",
            List.of(
                "idempotency_key",
                "attempts",
                "max_attempts",
                "next_retry_at_utc",
                "queued_at_utc",
                "processing_started_at_utc",
                "dispatched_at_utc",
                "sent_at_utc",
                "stream_name",
                "stream_message_id",
                "provider_message_id",
                "worker_id",
                "last_error",
                "last_error_code",
                "updated_at",
                "artifact_required",
                "artifact_id"
            )
        );

        requireColumns(
            "user_sport_notification_channels",
            List.of("user_id", "sport_id", "channel", "enabled", "default_lead_time_minutes")
        );

        requireColumns(
            "user_follow_notification_channels",
            List.of("follow_id", "channel", "enabled", "override_lead_time_minutes")
        );

        log.info("Alert schema startup validation passed");
    }

    /**
     * Ensures one required table exists in public schema.
     *
     * @param tableName required table name
     */
    private void requireTable(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
            """
            select count(*)
            from information_schema.tables
            where table_schema = 'public'
              and table_name = ?
            """,
            Integer.class,
            tableName
        );

        if (count == null || count == 0) {
            throw new IllegalStateException("Required table missing: public." + tableName);
        }
    }

    /**
     * Ensures required columns exist for a table.
     *
     * @param tableName table to inspect
     * @param columns required column names
     */
    private void requireColumns(String tableName, List<String> columns) {
        for (String column : columns) {
            Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = ?
                  and column_name = ?
                """,
                Integer.class,
                tableName,
                column
            );

            if (count == null || count == 0) {
                throw new IllegalStateException(
                    "Required column missing: public." + tableName + "." + column
                );
            }
        }
    }
}