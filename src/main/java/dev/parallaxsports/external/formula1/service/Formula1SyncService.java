package dev.parallaxsports.external.formula1.service;

import dev.parallaxsports.external.formula1.client.OpenF1Client;
import dev.parallaxsports.sport.formula1.dto.Formula1SessionResponse;
import dev.parallaxsports.sport.formula1.dto.Formula1SyncResponse;
import dev.parallaxsports.external.formula1.dto.OpenF1MeetingDto;
import dev.parallaxsports.external.formula1.dto.OpenF1SessionDto;
import dev.parallaxsports.notification.event.EventsIngestedEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class Formula1SyncService {

    private final OpenF1Client openF1Client;
    private final Formula1SyncWriteService formula1SyncWriteService;
    private final Formula1SessionReadService formula1SessionReadService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Synchronizes one Formula 1 season by year from OpenF1 into normalized domain tables.
     *
     * @param year season year to fetch and upsert
     * @return sync summary with fetched and inserted counters
     */
    @Transactional
    public Formula1SyncResponse syncYear(int year) {
        List<OpenF1MeetingDto> meetings = openF1Client.fetchMeetings(year);
        List<OpenF1SessionDto> sessions = openF1Client.fetchSessions(year);

        Formula1SyncWriteService.SyncCounters counters = formula1SyncWriteService.syncSeason(year, meetings, sessions);
        if (!counters.processedSessionEventIds().isEmpty()) {
            eventPublisher.publishEvent(new EventsIngestedEvent(List.copyOf(counters.processedSessionEventIds())));
        }

        log.info(
            "Formula1 sync finished year={} meetingsFetched={} sessionsFetched={} meetingsUpserted={} sessionsUpserted={} venuesUpserted={}",
            year,
            meetings.size(),
            sessions.size(),
            counters.meetingsUpserted(),
            counters.sessionsUpserted(),
            counters.venuesUpserted()
        );

        return new Formula1SyncResponse(
            year,
            meetings.size(),
            sessions.size(),
            counters.meetingsUpserted(),
            counters.sessionsUpserted(),
            counters.venuesUpserted()
        );
    }

    /**
     * Returns normalized Formula 1 session events for a given year.
     *
     * @param year season year requested by API clients
     * @return chronologically ordered session responses with venue logo URLs when present
     */
    @Transactional(readOnly = true)
    public List<Formula1SessionResponse> getSessionsForYear(int year) {
        return formula1SessionReadService.getSessionsForYear(year);
    }
}
