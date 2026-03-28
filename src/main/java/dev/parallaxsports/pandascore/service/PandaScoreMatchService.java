package dev.parallaxsports.pandascore.service;

import dev.parallaxsports.pandascore.dto.PandaScoreMatchResponse;
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
public class PandaScoreMatchService {

    private final PandaScoreMatchRepository matchRepository;

    @Transactional(readOnly = true)
    public List<PandaScoreMatchResponse> getAllMatches() {
        return matchRepository.findAllByOrderByBeginAtDesc()
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PandaScoreMatchResponse> getMatchesByLeague(String leagueName) {
        return matchRepository.findByLeagueNameOrderByBeginAtDesc(leagueName)
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PandaScoreMatchResponse> getMatchesByVideogame(String videogame) {
        return matchRepository.findByVideogameIgnoreCaseOrderByBeginAtDesc(videogame)
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PandaScoreMatchResponse> getMatchesBetweenDates(OffsetDateTime startDate, OffsetDateTime endDate) {
        return matchRepository.findByBeginAtBetweenOrderByBeginAtDesc(startDate, endDate)
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    private PandaScoreMatchResponse mapToResponse(PandaScoreMatch match) {
        return new PandaScoreMatchResponse(
            match.getId(),
            match.getName(),
            match.getLeagueName(),
            match.getStatus(),
            match.getSlug(),
            match.getBeginAt(),
            match.getEndAt(),
            match.getVideogame()
        );
    }
}

