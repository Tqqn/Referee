package dev.tqqn.referee.player;

import dev.tqqn.referee.team.MatchTeam;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
@Getter
@Setter
public class MatchPlayer {

    private final UUID uuid;
    private Role role = Role.PLAYING;

    public MatchPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    public Optional<Player> getPlayer() {
        return Optional.ofNullable(Bukkit.getPlayer(uuid));
    }
}
