package dev.tqqn.referee.spectator;

import dev.tqqn.satchel.item.PluginItem;
import dev.tqqn.satchel.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 14/11/2026
 */
public final class TeleporterItem extends PluginItem {

    private static final String KEY = "referee:teleporter";

    private final SpectatorHandler<?> handler;

    TeleporterItem(SpectatorHandler<?> handler) {
        super(KEY, ItemBuilder.of(Material.COMPASS).name(handler.match.messages().teleportCompassName()).build());
        this.handler = handler;
    }

    @Override
    public void onInteract(Player player, Click click) {
        if (click == Click.RIGHT && handler.isSpectator(player)) handler.openTeleportMenu(player);
    }
}
