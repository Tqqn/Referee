package dev.tqqn.referee.player;

import dev.tqqn.referee.team.MatchTeam;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
@Getter
@Setter
public class TeamPlayer extends MatchPlayer {

    private MatchTeam team;

    public TeamPlayer(UUID uuid) {
        super(uuid);
    }
}
