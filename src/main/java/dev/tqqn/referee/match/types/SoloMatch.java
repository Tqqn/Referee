package dev.tqqn.referee.match.types;

import co.aikar.commands.PaperCommandManager;
import dev.tqqn.referee.match.Match;
import dev.tqqn.referee.match.MatchSettings;
import dev.tqqn.referee.player.MatchPlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
public abstract class SoloMatch<P extends MatchPlayer> extends Match<P> {

    protected SoloMatch(JavaPlugin plugin, PaperCommandManager commandManager, MatchSettings settings) {
        super(plugin, commandManager, settings);
    }

    @Override
    public boolean isDecided() {
        return getAlive().size() <= 1;
    }

    /** The last player standing, once decided. Empty if nobody is left. */
    public Optional<P> getWinner() {
        final List<P> alive = getAlive();
        return alive.size() == 1 ? Optional.of(alive.getFirst()) : Optional.empty();
    }
}
