package dev.tqqn.referee.arena;

import dev.tqqn.referee.player.MatchPlayer;
import dev.tqqn.referee.player.TeamPlayer;
import dev.tqqn.referee.team.MatchTeam;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
public interface Arena {

    /** Map name, e.g. for the scoreboard. */
    String getName();

    /** Creates the world and runs setup. Completes on the main thread; fails instead of hanging if anything breaks. */
    CompletableFuture<World> load();

    /** @throws IllegalStateException before load() completed */
    World getWorld();

    /** All locations stored under this key, in order. Empty if none. */
    List<Location> points(String key);

    /** Unloads the world without saving and deletes its folder. */
    void unload();

    /** First location under this key. */
    default Location point(String key) {
        final List<Location> points = points(key);
        if (points.isEmpty()) throw new IllegalStateException("Arena '" + getName() + "' has no point '" + key + "'");
        return points.getFirst();
    }

    /** "spawn" point #index (0-based), wrapping around if there are more players/teams than spawns. */
    default Location spawnFor(int index) {
        final List<Location> spawns = points("spawn");
        if (spawns.isEmpty()) throw new IllegalStateException("Arena '" + getName() + "' has no 'spawn' points");
        return spawns.get(Math.floorMod(index, spawns.size()));
    }

    /** Team games: team N spawns at spawn point N. */
    default Location spawnFor(MatchTeam team) {
        return spawnFor(team.getIndex() - 1);
    }

    /** SoloMatch: player i goes to spawn i. */
    default void teleportToSpawns(List<? extends MatchPlayer> players) {
        for (int i = 0; i < players.size(); i++) {
            final Location spawn = spawnFor(i);
            players.get(i).getPlayer().ifPresent(p -> p.teleport(spawn));
        }
    }

    /** TeamMatch: every player goes to their team's spawn. */
    default void teleportTeamsToSpawns(List<MatchTeam> teams) {
        for (MatchTeam team : teams) {
            final Location spawn = spawnFor(team);
            team.getMembers().stream().map(TeamPlayer::getPlayer).flatMap(java.util.Optional::stream)
                    .forEach(p -> p.teleport(spawn));
        }
    }
}