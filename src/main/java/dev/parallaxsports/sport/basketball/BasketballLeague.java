package dev.parallaxsports.sport.basketball;

public enum BasketballLeague {

    NBA("v1", "NBA", "balldontlie-nba"),
    WNBA("wnba/v1", "WNBA", "balldontlie-wnba");

    private final String apiPathPrefix;
    private final String competitionName;
    private final String provider;

    BasketballLeague(String apiPathPrefix, String competitionName, String provider) {
        this.apiPathPrefix = apiPathPrefix;
        this.competitionName = competitionName;
        this.provider = provider;
    }

    public String getApiPathPrefix() {
        return apiPathPrefix;
    }

    public String getCompetitionName() {
        return competitionName;
    }

    public String getProvider() {
        return provider;
    }

    public static BasketballLeague fromCompetitionName(String name) {
        if (name == null) {
            return NBA;
        }
        for (BasketballLeague league : values()) {
            if (league.competitionName.equalsIgnoreCase(name)) {
                return league;
            }
        }
        return NBA;
    }
}
