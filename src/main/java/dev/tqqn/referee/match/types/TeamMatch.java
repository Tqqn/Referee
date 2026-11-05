package dev.tqqn.referee.match.types;

import co.aikar.commands.PaperCommandManager;
import dev.tqqn.referee.match.Match;
import dev.tqqn.referee.match.MatchSettings;
import dev.tqqn.referee.player.Role;
import dev.tqqn.referee.player.TeamPlayer;
import dev.tqqn.referee.team.MatchTeam;
import dev.tqqn.referee.team.TeamAssigner;
import dev.tqqn.referee.team.TeamManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
@Getter
public abstract class TeamMatch<P extends TeamPlayer> extends Match<P> {

    private final int teamSize;
    private final TeamManager teams;

    protected TeamMatch(JavaPlugin plugin, PaperCommandManager commandManager, MatchSettings settings, int teamSize) {
        super(plugin, commandManager, settings);
        if (teamSize < 1) throw new IllegalArgumentException("teamSize must be >= 1, use SoloMatch for no teams");
        this.teamSize = teamSize;
        this.teams = new TeamManager(Math.ceilDiv(settings.maxPlayers(), teamSize), teamSize);
    }

    /** Swap for party-aware or skill-based splitting. */
    protected TeamAssigner teamAssigner() {
        return TeamAssigner.balanced();
    }

    @Override
    public void onLobbyEnd() {
        teamAssigner().assign(teams, getAlive());
    }

    /** Teams with at least one member still playing. */
    public List<MatchTeam> getAliveTeams() {
        return teams.getTeams().stream()
                .filter(t -> t.getMembers().stream().anyMatch(p -> p.getRole() == Role.PLAYING))
                .toList();
    }

    @Override
    public boolean isDecided() {
        return getAliveTeams().size() <= 1;
    }

    /** The last team standing, once decided. */
    public Optional<MatchTeam> getWinner() {
        final List<MatchTeam> alive = getAliveTeams();
        return alive.size() == 1 ? Optional.of(alive.getFirst()) : Optional.empty();
    }

    /** Whether everyone plays alone (one-player teams). */
    public boolean isSolo() {
        return teamSize == 1;
    }
}
