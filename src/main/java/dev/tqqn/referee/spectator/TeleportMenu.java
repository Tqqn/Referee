package dev.tqqn.referee.spectator;

import dev.tqqn.referee.player.MatchPlayer;
import dev.tqqn.referee.player.Role;
import dev.tqqn.satchel.menu.AutoPagedMenu;
import dev.tqqn.satchel.menu.MenuButton;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 14/11/2026
 */
public final class TeleportMenu<P extends MatchPlayer> extends AutoPagedMenu {

    private static final List<Integer> CONTENT_SLOTS = IntStream.range(0, 45).boxed().toList();

    private final SpectatorHandler<P> handler;

    TeleportMenu(SpectatorHandler<P> handler, Player viewer) {
        super(handler.match.messages().teleportMenuTitle(), 6, viewer);
        this.handler = handler;
        addPageButton(PageButton.BACK, 45);
        addPageButton(PageButton.CLOSE, 49);
        addPageButton(PageButton.FORWARD, 53);
    }

    @Override
    protected List<Integer> getContentSlots() {
        return CONTENT_SLOTS;
    }

    @Override
    protected List<MenuButton> getContents() {
        final List<MenuButton> buttons = new ArrayList<>();
        for (P target : handler.match.getAlive()) {
            final Optional<Player> online = target.getPlayer();
            if (online.isEmpty()) continue;
            buttons.add(new MenuButton(handler.createHead(target, online.get())).onClick(viewer -> {
                // target may have died or left since the menu opened
                final Player now = target.getPlayer().orElse(null);
                if (now != null && target.getRole() == Role.PLAYING) viewer.teleport(now);
                close();
            }));
        }
        return buttons;
    }
}