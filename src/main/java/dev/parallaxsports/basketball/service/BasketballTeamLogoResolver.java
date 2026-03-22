package dev.parallaxsports.basketball.service;

import dev.parallaxsports.basketball.BasketballLeague;
import java.util.Locale;

public final class BasketballTeamLogoResolver {

    private static final String ESPN_NBA_LOGO_BASE = "https://a.espncdn.com/i/teamlogos/nba/500/";
    private static final String ESPN_WNBA_LOGO_BASE = "https://a.espncdn.com/i/teamlogos/wnba/500/";

    private BasketballTeamLogoResolver() {
    }

    public static String resolveLogoUrl(BasketballLeague league, String abbreviation) {
        if (abbreviation == null || abbreviation.isBlank()) {
            return null;
        }

        String base = switch (league) {
            case NBA -> ESPN_NBA_LOGO_BASE;
            case WNBA -> ESPN_WNBA_LOGO_BASE;
        };

        String normalized = abbreviation.trim().toLowerCase(Locale.ROOT);
        return base + normalized + ".png";
    }
}
