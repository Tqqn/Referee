package dev.tqqn.referee.event;

import dev.tqqn.referee.match.Match;
import dev.tqqn.referee.state.MatchState;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
@Getter
public final class MatchStateChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Match<?> match;
    private final @Nullable MatchState from;
    private final MatchState to;

    public MatchStateChangeEvent(Match<?> match, @Nullable MatchState from, MatchState to) {
        this.match = match;
        this.from = from;
        this.to = to;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
