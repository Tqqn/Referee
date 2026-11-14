package dev.tqqn.referee.state.buildin;

import dev.tqqn.referee.match.Match;
import dev.tqqn.referee.player.Role;
import dev.tqqn.referee.state.AbstractState;
import dev.tqqn.referee.status.MatchStatus;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 11/11/2026
 */
public abstract class PlayingState<G extends Match<?>> extends AbstractState<G> {

    public static final String SPECTATE_PERMISSION = "referee.spectate";

    private final Map<UUID, BukkitTask> disconnected = new HashMap<>();

    protected PlayingState(G match, String name, int durationSeconds) {
        super(match, name, durationSeconds);
    }

    /** Seconds a player who quits may take to reconnect before being eliminated. 0 = out on quit. */
    protected int rejoinSeconds() {
        return 0;
    }

    protected boolean canSpectate(Player player) {
        return player.hasPermission(SPECTATE_PERMISSION);
    }

    @Override
    protected void onEnter() {
        match.setStatus(MatchStatus.IN_GAME);
    }

    @Override
    public void onJoin(Player player) {
        final BukkitTask pending = disconnected.remove(player.getUniqueId());
        if (pending != null && !pending.isCancelled()) {
            pending.cancel(); // still PLAYING, Bukkit restored their location and inventory
            match.broadcast(match.messages().playerRejoined(player.getName()));
            return;
        }
        if (match.getPlayer(player).isEmpty() && !canSpectate(player)) {
            player.kick(MiniMessage.miniMessage().deserialize(match.messages().notJoinable()));
            return;
        }
        match.addPlayer(player, Role.SPECTATOR);
        match.spectate(player);
    }

    @Override
    public void onQuit(Player player) {
        final UUID uuid = player.getUniqueId();
        final boolean playing = match.getPlayer(uuid).filter(p -> p.getRole() == Role.PLAYING).isPresent();
        if (!playing) return;

        final String name = player.getName();
        final int seconds = rejoinSeconds();
        if (seconds <= 0) {
            match.broadcast(match.messages().playerLeftMatch(name));
            match.eliminate(uuid);
            return;
        }

        match.broadcast(match.messages().playerDisconnected(name, seconds));
        // registered: canceled automatically if the state ends first
        disconnected.put(uuid, register(Bukkit.getScheduler().runTaskLater(match.getPlugin(), () -> {
            disconnected.remove(uuid);
            match.broadcast(match.messages().playerLeftMatch(name));
            match.eliminate(uuid);
        }, seconds * 20L)));
    }

    /** Whether this player disconnected and may still come back. */
    public boolean isRejoining(UUID uuid) {
        final BukkitTask pending = disconnected.get(uuid);
        return pending != null && !pending.isCancelled();
    }
}
