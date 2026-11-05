package dev.tqqn.referee.team;

import dev.tqqn.referee.player.MatchPlayer;
import dev.tqqn.referee.player.TeamPlayer;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
@Getter
public final class MatchTeam {

    private final TeamColor color;
    private final int index;
    private final List<TeamPlayer> members = new ArrayList<>();

    MatchTeam(TeamColor color, int index) {
        this.color = color;
        this.index = index;
    }

    public String getColoredName() {
        return color.getMiniColor() + color.getDisplayName();
    }
}
