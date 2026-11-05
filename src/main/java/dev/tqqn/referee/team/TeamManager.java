package dev.tqqn.referee.team;

import dev.tqqn.referee.player.MatchPlayer;
import dev.tqqn.referee.player.TeamPlayer;
import lombok.Getter;

import java.util.*;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
public final class TeamManager {

    private final List<MatchTeam> teams = new ArrayList<>();
    @Getter private final int teamSize;

    public TeamManager(int teamCount, int teamSize) {
        this.teamSize = teamSize;
        if (teamCount > TeamColor.values().length) {
            throw new IllegalArgumentException(teamCount + " teams but only " + TeamColor.values().length
                    + " colours; use SoloMatch for free-for-all games");
        }
        final List<TeamColor> colors = new ArrayList<>(List.of(TeamColor.values()));
        Collections.shuffle(colors);
        for (int i = 0; i < teamCount; i++) {
            teams.add(new MatchTeam(colors.get(i), i + 1));
        }
    }

    /** Moves the player into the team. Returns false (and does nothing) if the team is full. */
    public boolean join(MatchTeam team, TeamPlayer player) {
        if (isFull(team)) return false;
        remove(player);
        team.getMembers().add(player);
        player.setTeam(team);
        return true;
    }

    public void remove(TeamPlayer player) {
        if (player.getTeam() != null) player.getTeam().getMembers().remove(player);
        player.setTeam(null);
    }

    public boolean isFull(MatchTeam team) {
        return team.getMembers().size() >= teamSize;
    }

    public Optional<MatchTeam> emptiestOpenTeam() {
        return teams.stream()
                .filter(t -> !isFull(t))
                .min(Comparator.comparingInt(t -> t.getMembers().size()));
    }

    /** Teams with at least one member. */
    public List<MatchTeam> getActiveTeams() {
        return teams.stream().filter(t -> !t.getMembers().isEmpty()).toList();
    }

    public List<MatchTeam> getTeams() {
        return List.copyOf(teams);
    }
}
