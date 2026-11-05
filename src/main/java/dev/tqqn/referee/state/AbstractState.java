package dev.tqqn.referee.state;

import dev.tqqn.referee.match.Match;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
public abstract class AbstractState<G extends Match<?>> implements MatchState {

    @Getter protected final G match;
    private final String name;
    @Getter
    private final int duration;

    @Getter @Setter
    private int timeLeft;

    @Getter @Setter
    private boolean frozen;

    private StateScope scope;

    protected AbstractState(G match, String name, int durationSeconds) {
        this.match = match;
        this.name = name;
        this.duration = durationSeconds;
    }

    // ---- hooks ----
    protected void onEnter() {}
    protected void onExit() {}
    protected void onTick() {}
    protected void onTimerEnd() { match.nextState(); }

    // ---- lifecycle, fixed ----

    @Override
    public final String name() {
        return name;
    }

    @Override
    public final void enter(StateScope scope) {
        this.scope = scope;
        this.timeLeft = duration;
        this.frozen = false;
        onEnter();
    }

    @Override
    public final void exit() {
        onExit();
        this.scope = null;
    }

    @Override
    public final void tick() {
        if (frozen) return;
        if (duration > 0 && --timeLeft <= 0) {
            onTimerEnd();
            return;
        }
        onTick();
    }

    /** Listener, BaseCommand or BukkitTask, removed when this state exits. Only valid between enter and exit. */
    protected <T> T register(T object) {
        if (scope == null) throw new IllegalStateException(name + " is not active");
        return scope.register(object);
    }

    /** "mm:ss", or "h:mm:ss" from one hour up. */
    public String getFormattedTime() {
        final int h = timeLeft / 3600, m = timeLeft % 3600 / 60, s = timeLeft % 60;
        return h > 0 ? "%d:%02d:%02d".formatted(h, m, s) : "%02d:%02d".formatted(m, s);
    }
}
