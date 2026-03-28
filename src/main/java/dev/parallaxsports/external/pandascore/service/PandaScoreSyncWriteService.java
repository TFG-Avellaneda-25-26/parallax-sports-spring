package dev.parallaxsports.external.pandascore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.parallaxsports.external.pandascore.dto.PandaScoreMatchDto;
import dev.parallaxsports.pandascore.model.PandaScoreMatch;
import dev.parallaxsports.pandascore.repository.PandaScoreMatchRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PandaScoreSyncWriteService {

    private final PandaScoreMatchRepository matchRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public SyncCounters syncMatches(List<PandaScoreMatchDto> matches, String videogame) {
        int matchesUpserted = 0;

        for (PandaScoreMatchDto matchDto : matches) {
            try {
                if (upsertMatch(matchDto, videogame)) {
                    matchesUpserted++;
                }
            } catch (Exception e) {
                log.error("Error syncing match with id={}", matchDto.id(), e);
            }
        }

        log.info("PandaScore sync completed: matches={}", matchesUpserted);
        return new SyncCounters(matchesUpserted);
    }

    private boolean upsertMatch(PandaScoreMatchDto matchDto, String videogame) {
        var existing = matchRepository.findByPandascoreId(matchDto.id());

        OffsetDateTime beginAt = null;
        OffsetDateTime endAt = null;

        try {
            if (matchDto.beginAt() != null && !matchDto.beginAt().isBlank()) {
                beginAt = OffsetDateTime.parse(matchDto.beginAt());
            }
            if (matchDto.endAt() != null && !matchDto.endAt().isBlank()) {
                endAt = OffsetDateTime.parse(matchDto.endAt());
            }
        } catch (Exception e) {
            log.warn("Error parsing dates for match id={}", matchDto.id(), e);
        }

        String leagueName = matchDto.league() != null ? matchDto.league().name() : null;
        // If matchDto.id is null we skip (PandaScore should always provide an id)
        if (matchDto.id() == null) {
            log.warn("Skipping match without pandascore id: {}", matchDto);
            return false;
        }

        PandaScoreMatch match = PandaScoreMatch.builder()
            .pandascoreId(matchDto.id())
            .name(matchDto.name())
            .leagueName(leagueName)
            .status(matchDto.status())
            .slug(matchDto.slug())
            .beginAt(beginAt)
            .endAt(endAt)
            .videogame(videogame)
            .rawJson(serializeRaw(matchDto))
            .build();

        if (existing.isPresent()) {
            match.setId(existing.get().getId());
        }

        matchRepository.save(match);
        return existing.isEmpty();
    }

    public record SyncCounters(
        int matchesUpserted
    ) {
    }

    private String serializeRaw(PandaScoreMatchDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            log.warn("Failed to serialize match DTO to raw JSON for id={}: {}", dto.id(), e.getMessage());
            return null;
        }
    }
}

