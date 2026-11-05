package dev.tqqn.referee.match;

import java.util.function.Function;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
public record MatchSettings(int minPlayers, int teamSize, int teamCount, String variant) {

    public int maxPlayers() {
        return teamSize * teamCount;
    }

    public boolean isSolo() {
        return teamSize <= 1;
    }

    public static MatchSettings fromEnv(MatchSettings defaults) {
        return new MatchSettings(
                env("MIN_GAME_PLAYERS_TO_START", Integer::parseInt, defaults.minPlayers),
                env("PLAYERS_PER_TEAM", Integer::parseInt, defaults.teamSize),
                env("TEAM_COUNT", Integer::parseInt, defaults.teamCount),
                env("GAME_TYPE", Function.identity(), defaults.variant));
    }

    private static <T> T env(String key, Function<String, T> parse, T fallback) {
        final String raw = System.getenv(key);
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return parse.apply(raw.trim());
        } catch (RuntimeException e) {
            System.getLogger(MatchSettings.class.getName())
                    .log(System.Logger.Level.WARNING, "Invalid " + key + "=" + raw + ", using " + fallback);
            return fallback;
        }
    }
}