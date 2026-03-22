package dev.parallaxsports.basketball.service;

import java.util.Locale;

public final class NbaTeamLogoResolver {

    private static final String ESPN_NBA_LOGO_BASE = "https://a.espncdn.com/i/teamlogos/nba/500/";

    private NbaTeamLogoResolver() {
    }

    public static String resolveLogoUrl(String abbreviation) {
        if (abbreviation == null || abbreviation.isBlank()) {
            return null;
        }

        String normalized = abbreviation.trim().toLowerCase(Locale.ROOT);
        return ESPN_NBA_LOGO_BASE + normalized + ".png";
    }
}