package dev.tqqn.referee.spectator;

import dev.tqqn.referee.match.Match;
import dev.tqqn.referee.player.MatchPlayer;
import dev.tqqn.referee.player.Role;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 14/11/2026
 */
public class SpectatorHandler implements Listener {

    protected final Match<?> match;

    public SpectatorHandler(Match<?> match) {
        this.match = match;
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, match.getPlugin());
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    /** Turns an online player into a spectator. Their role must already be SPECTATOR. */
    public void apply(Player player) {
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setCollidable(false);
        player.setInvulnerable(true);
        player.getInventory().setItem(0, compass());

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;
            if (isSpectator(other)) {
                player.showPlayer(match.getPlugin(), other); // spectators see each other
            } else {
                other.hidePlayer(match.getPlugin(), player);
            }
        }
    }

    public boolean isSpectator(Entity entity) {
        return entity instanceof Player player
                && match.getPlayer(player.getUniqueId()).map(p -> p.getRole() == Role.SPECTATOR).orElse(false);
    }

    /** Opens the teleport menu with every online player still in the match. */
    public void openTeleportMenu(Player viewer) {
        final List<Player> targets = new ArrayList<>();
        for (MatchPlayer alive : match.getAlive()) alive.getPlayer().ifPresent(targets::add);

        final TeleportMenu menu = new TeleportMenu(targets.stream().map(Player::getUniqueId).toList());
        final int size = Math.max(9, Math.min(54, Math.ceilDiv(targets.size(), 9) * 9));
        menu.inventory = Bukkit.createInventory(menu, size, mm(match.messages().teleportMenuTitle()));
        for (int i = 0; i < Math.min(size, targets.size()); i++) {
            menu.inventory.setItem(i, createHead(targets.get(i)));
        }
        viewer.openInventory(menu.inventory);
    }

    protected ItemStack createHead(Player target) {
        final ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        head.editMeta(SkullMeta.class, meta -> {
            meta.setOwningPlayer(target);
            meta.displayName(Component.text(target.getName()));
        });
        return head;
    }

    protected ItemStack compass() {
        final ItemStack compass = new ItemStack(Material.COMPASS);
        compass.editMeta(meta -> meta.displayName(mm(match.messages().teleportCompassName())));
        return compass;
    }

    // ---- keep spectators out of the game ----

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // hiding doesn't survive a relog: hide spectators from players (re)joining
        final Player joined = event.getPlayer();
        if (isSpectator(joined)) return;
        Bukkit.getOnlinePlayers().stream().filter(this::isSpectator)
                .forEach(spectator -> joined.hidePlayer(match.getPlugin(), spectator));
    }

    @EventHandler(priority = EventPriority.LOWEST) // no ignoreCancelled: right-clicking air arrives pre-cancelled
    public void onInteract(PlayerInteractEvent event) {
        if (!isSpectator(event.getPlayer())) return;
        event.setCancelled(true);
        if (event.getItem() != null && event.getItem().getType() == Material.COMPASS && event.getAction().isRightClick()) {
            openTeleportMenu(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMenuClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof TeleportMenu menu) {
            event.setCancelled(true);
            final int slot = event.getRawSlot();
            if (slot < 0 || slot >= menu.targets.size()) return;
            final Player target = Bukkit.getPlayer(menu.targets.get(slot));
            // target may have died since the menu opened
            if (target != null && match.getPlayer(target.getUniqueId()).map(p -> p.getRole() == Role.PLAYING).orElse(false)) {
                event.getWhoClicked().teleport(target);
            }
            event.getWhoClicked().closeInventory();
        } else if (isSpectator(event.getWhoClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (isSpectator(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (isSpectator(event.getDamager())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (isSpectator(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (isSpectator(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (isSpectator(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (isSpectator(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (isSpectator(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTarget(EntityTargetEvent event) {
        if (isSpectator(event.getTarget())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (isSpectator(event.getEntity())) event.setCancelled(true);
    }

    // ponytail: arrows can still be stopped by a hidden spectator's hitbox; cancel ProjectileHitEvent if that shows up.

    private static Component mm(String miniMessage) {
        return MiniMessage.miniMessage().deserialize(miniMessage);
    }

    private static final class TeleportMenu implements InventoryHolder {
        private final List<UUID> targets;
        private Inventory inventory;

        private TeleportMenu(List<UUID> targets) {
            this.targets = targets;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
