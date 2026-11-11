package dev.tqqn.referee.event;

import dev.tqqn.referee.match.Match;
import dev.tqqn.referee.player.MatchPlayer;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 11/11/2026
 */
@Getter
public final class PlayerEliminatedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Match<?> match;
    private final MatchPlayer player;

    public PlayerEliminatedEvent(Match<?> match, MatchPlayer player) {
        this.match = match;
        this.player = player;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
