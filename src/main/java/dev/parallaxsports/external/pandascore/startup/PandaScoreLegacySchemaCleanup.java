package dev.parallaxsports.external.pandascore.startup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PandaScoreLegacySchemaCleanup implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        Integer count = jdbcTemplate.queryForObject(
            """
            select count(*)
            from information_schema.tables
            where table_schema = 'public'
              and table_name = 'pandascore_matches'
            """,
            Integer.class
        );

        if (count != null && count > 0) {
            jdbcTemplate.execute("drop table if exists public.pandascore_matches cascade");
            log.warn("Dropped legacy table public.pandascore_matches; PandaScore now uses sports/competitions/events");
        }
    }
}




