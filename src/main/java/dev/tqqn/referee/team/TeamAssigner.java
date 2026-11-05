package dev.tqqn.referee.team;

import dev.tqqn.referee.player.MatchPlayer;
import dev.tqqn.referee.player.TeamPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
@FunctionalInterface
public interface TeamAssigner {

    void assign(TeamManager teams, List<? extends TeamPlayer> players);

    /** Random order, always into the emptiest open team. Players that don't fit stay team-less. */
    static TeamAssigner balanced() {
        return (teams, players) -> {
            final List<TeamPlayer> shuffled = new ArrayList<>(players);
            Collections.shuffle(shuffled);
            shuffled.forEach(p -> teams.emptiestOpenTeam().ifPresent(t -> teams.join(t, p)));
        };
    }
}
