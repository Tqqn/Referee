package dev.tqqn.referee.state;

import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import org.bukkit.entity.Player;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
public interface MatchState {

    String name();

    default void enter(StateScope scope) {}

    default void exit() {}

    default void tick() {}

    default void onLogin(PlayerConnectionValidateLoginEvent event) {}

    default void onJoin(Player player) {}

    default void onQuit(Player player) {}
}
