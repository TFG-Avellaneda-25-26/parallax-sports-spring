package dev.parallaxsports.notification.service;

import dev.parallaxsports.core.exception.ResourceNotFoundException;
import dev.parallaxsports.sport.model.Event;
import dev.parallaxsports.sport.model.EventEntry;
import dev.parallaxsports.sport.model.MediaAsset;
import dev.parallaxsports.sport.model.Participant;
import dev.parallaxsports.sport.repository.EventEntryRepository;
import dev.parallaxsports.sport.repository.EventRepository;
import dev.parallaxsports.sport.repository.MediaAssetRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class AlertRenderService {

    private static final String TEMPLATE_VERSION = "v1";
    private static final String FORMULA1_KEY = "formula1";
    private static final String OWNER_VENUE = "venue";
    private static final String OWNER_PARTICIPANT = "participant";
    private static final String OWNER_COMPETITION = "competition";
    private static final String ASSET_LOGO = "logo";

    private final EventRepository eventRepository;
    private final EventEntryRepository eventEntryRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final TemplateEngine templateEngine;

    public record RenderResult(String html, String hash) {}

    public String computeHash(Long eventId, String channel, String tz) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        boolean isEmail = "email".equalsIgnoreCase(channel);
        ZoneId zone = resolveZone(tz, event);
        String sportKey = event.getSport() != null ? event.getSport().getKey() : null;
        String template = pickTemplate(sportKey, event);
        if (template == null) return null;
        return computeHash(eventId, sportKey, event.getStatus(), template, isEmail ? zone.getId() : null);
    }

    private String pickTemplate(String sportKey, Event event) {
        if (FORMULA1_KEY.equals(sportKey)) return "alert/f1";
        if ("teams".equals(event.getParticipantsMode())) return "alert/versus";
        return null;
    }

    @Transactional(readOnly = true)
    public RenderResult render(Long eventId, String channel, String tz, String localeTag) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        boolean isEmail = "email".equalsIgnoreCase(channel);
        ZoneId zone = resolveZone(tz, event);
        Locale locale = resolveLocale(localeTag);

        String sportKey = event.getSport() != null ? event.getSport().getKey() : null;
        String template = pickTemplate(sportKey, event);
        if (template == null) {
            throw new ResourceNotFoundException("No template available for event " + eventId);
        }
        Context ctx = new Context(locale);
        ctx.setVariable("event", event);
        ctx.setVariable("isEmail", isEmail);
        ctx.setVariable("localizedTime", formatTime(event.getStartTimeUtc(), zone, locale));
        ctx.setVariable("localizedDate", formatDate(event.getStartTimeUtc(), zone, locale));
        ctx.setVariable("zoneLabel", zone.getId());

        if ("alert/f1".equals(template)) {
            ctx.setVariable("gpName", resolveGpName(event));
            ctx.setVariable("sessionLabel", resolveSessionLabel(event));
            ctx.setVariable("backgroundUrl", resolveVenueImage(event));
        } else {
            List<EventEntry> entries = eventEntryRepository.findWithParticipantByEventIdIn(List.of(eventId));
            entries.sort(Comparator.comparing(e -> Optional.ofNullable(e.getDisplayOrder()).orElse(0)));
            EventEntry home = entries.isEmpty() ? null : entries.get(0);
            EventEntry away = entries.size() < 2 ? null : entries.get(1);
            ctx.setVariable("home", home != null ? home.getParticipant() : null);
            ctx.setVariable("away", away != null ? away.getParticipant() : null);
            ctx.setVariable("homeLogo", logoFor(home));
            ctx.setVariable("awayLogo", logoFor(away));
            ctx.setVariable("competitionName", event.getCompetition() != null ? event.getCompetition().getName() : "");
        }

        String html = templateEngine.process(template, ctx);
        String hash = computeHash(eventId, sportKey, event.getStatus(), template, isEmail ? zone.getId() : null);
        return new RenderResult(html, hash);
    }

    private ZoneId resolveZone(String tz, Event event) {
        if (tz != null && !tz.isBlank()) {
            try { return ZoneId.of(tz); } catch (Exception ignored) {}
        }
        if (event.getVenue() != null && event.getVenue().getTimezone() != null) {
            try { return ZoneId.of(event.getVenue().getTimezone()); } catch (Exception ignored) {}
        }
        return ZoneId.of("UTC");
    }

    private Locale resolveLocale(String tag) {
        if (tag == null || tag.isBlank()) return Locale.ENGLISH;
        try { return Locale.forLanguageTag(tag); } catch (Exception e) { return Locale.ENGLISH; }
    }

    private String formatTime(OffsetDateTime utc, ZoneId zone, Locale locale) {
        return utc.atZoneSameInstant(zone)
            .format(DateTimeFormatter.ofPattern("HH:mm", locale));
    }

    private String formatDate(OffsetDateTime utc, ZoneId zone, Locale locale) {
        return utc.atZoneSameInstant(zone)
            .format(DateTimeFormatter.ofPattern("EEE d MMM", locale));
    }

    private String resolveGpName(Event event) {
        if (event.getParentEvent() != null && event.getParentEvent().getName() != null) {
            return event.getParentEvent().getName();
        }
        if (event.getCompetition() != null) {
            return event.getCompetition().getName();
        }
        return event.getName();
    }

    private String resolveSessionLabel(Event event) {
        return event.getName();
    }

    private String resolveVenueImage(Event event) {
        if (event.getVenue() != null && event.getVenue().getId() != null) {
            Optional<MediaAsset> venueAsset = mediaAssetRepository
                .findFirstByOwnerTypeAndOwnerIdAndAssetTypeOrderByIdDesc(OWNER_VENUE, event.getVenue().getId(), ASSET_LOGO);
            if (venueAsset.isPresent()) return venueAsset.get().getUrl();
        }
        if (event.getCompetition() != null && event.getCompetition().getId() != null) {
            Optional<MediaAsset> compAsset = mediaAssetRepository
                .findFirstByOwnerTypeAndOwnerIdAndAssetTypeOrderByIdDesc(OWNER_COMPETITION, event.getCompetition().getId(), ASSET_LOGO);
            if (compAsset.isPresent()) return compAsset.get().getUrl();
        }
        return null;
    }

    private String logoFor(EventEntry entry) {
        if (entry == null) return null;
        Participant p = entry.getParticipant();
        if (p == null || p.getId() == null) return null;
        return mediaAssetRepository
            .findFirstByOwnerTypeAndOwnerIdAndAssetTypeOrderByIdDesc(OWNER_PARTICIPANT, p.getId(), ASSET_LOGO)
            .map(MediaAsset::getUrl)
            .orElse(null);
    }

    private String computeHash(Long eventId, String sportKey, String status, String template, String tzOrNull) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String input = String.join("|",
                String.valueOf(eventId),
                TEMPLATE_VERSION,
                template,
                String.valueOf(sportKey),
                String.valueOf(status),
                tzOrNull == null ? "" : tzOrNull);
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (Exception e) {
            throw new IllegalStateException("Hash failure", e);
        }
    }
}
