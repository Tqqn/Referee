package dev.tqqn.referee.spectator;

import dev.tqqn.referee.match.Match;
import dev.tqqn.referee.player.MatchPlayer;
import dev.tqqn.referee.player.Role;
import dev.tqqn.referee.player.TeamPlayer;
import dev.tqqn.referee.team.MatchTeam;
import dev.tqqn.satchel.Satchel;
import dev.tqqn.satchel.util.ItemBuilder;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 14/11/2026
 */
public class SpectatorHandler<P extends MatchPlayer> implements Listener {

    protected final Match<P> match;

    private final TeleporterItem teleporterItem;

    public SpectatorHandler(Match<?> match) {
        if (!Bukkit.getPluginManager().isPluginEnabled("Satchel")) {
            throw new IllegalStateException("Spectators need the Satchel plugin: install it and add depend: [Satchel] to plugin.yml");
        }
        this.match = match;
        this.teleporterItem = new TeleporterItem(this);
        Satchel.register(match.getPlugin(), teleporterItem);
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
        teleporterItem.give(player, 0);

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
        new TeleportMenu<>(this, viewer).open();
    }

    protected ItemStack createHead(P target, Player online) {
        final ItemStack head = ItemBuilder.of(Material.PLAYER_HEAD)
                .name(headName(target, online))
                .lore(headLore(target, online))
                .build();
        head.editMeta(SkullMeta.class, meta -> meta.setOwningPlayer(online));
        return head;
    }

    protected String headName(P target, Player online) {
        final MatchTeam team = teamOf(target);
        return team == null ? "<white>" + online.getName() : team.getColor().getMiniColor() + "[" + team.getColor().getDisplayName().toUpperCase() + "] " + online.getName();
    }

    protected List<String> headLore(P target, Player online) {
        final List<String> lore = new ArrayList<>();
        final MatchTeam team = teamOf(target);
        if (team != null) lore.add(match.messages().teleportHeadTeam(team.getColoredName()));
        lore.add(match.messages().teleportHeadClick());
        return lore;
    }

    private static MatchTeam teamOf(MatchPlayer player) {
        return player instanceof TeamPlayer teamPlayer ? teamPlayer.getTeam() : null;
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (isSpectator(event.getPlayer())) event.setCancelled(true); // Satchel still runs the compass click
    }

    public void onInventoryClick(InventoryClickEvent event) {
        if (isSpectator(event.getWhoClicked())) event.setCancelled(true);
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

}
