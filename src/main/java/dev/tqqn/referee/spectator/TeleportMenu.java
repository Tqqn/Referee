package dev.tqqn.referee.spectator;

import dev.tqqn.referee.player.MatchPlayer;
import dev.tqqn.referee.player.Role;
import dev.tqqn.satchel.menu.AutoPagedMenu;
import dev.tqqn.satchel.menu.MenuButton;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 14/11/2026
 */
public final class TeleportMenu extends AutoPagedMenu {

    private static final List<Integer> CONTENT_SLOTS = IntStream.range(0, 45).boxed().toList();

    private final SpectatorHandler handler;

    TeleportMenu(SpectatorHandler handler, Player viewer) {
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
        return handler.match.getAlive().stream()
                .map(MatchPlayer::getPlayer).flatMap(Optional::stream)
                .map(target -> new MenuButton(handler.createHead(target)).onClick(viewer -> {
                    // target may have died since the menu opened
                    final boolean playing = handler.match.getPlayer(target.getUniqueId())
                            .map(p -> p.getRole() == Role.PLAYING).orElse(false);
                    if (playing && target.isOnline()) viewer.teleport(target);
                    close();
                }))
                .toList();
    }
}