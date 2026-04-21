package dev.parallaxsports.pandascore.model;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Legacy DTO kept for compatibility with older debug endpoints.
 * Persistence moved to the normalized sport/competition/event model.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PandaScoreMatch {

    private Long id;

    private Long pandascoreId;

    private String name;

    private String leagueName;

    private String status;

    private String slug;

    private String videogame;

    private String rawJson;

    private OffsetDateTime beginAt;

    private OffsetDateTime endAt;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}

