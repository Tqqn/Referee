package dev.tqqn.referee.spectator;

import dev.tqqn.referee.match.Match;
import dev.tqqn.referee.player.MatchPlayer;
import dev.tqqn.referee.player.Role;
import dev.tqqn.referee.player.TeamPlayer;
import dev.tqqn.referee.team.MatchTeam;
import dev.tqqn.satchel.Satchel;
import dev.tqqn.satchel.util.ItemBuilder;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 14/11/2026
 */
public class SpectatorHandler<P extends MatchPlayer> implements Listener {

    // tab sorts by team name: "~" comes after letters, digits and "_", so spectators are listed below the players
    private static final String SPECTATOR_TEAM = "~referee_spectators";

    protected final Match<P> match;

    private final TeleporterItem teleporterItem;

    private boolean transparent;

    public SpectatorHandler(Match<P> match) {
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

        final Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(SPECTATOR_TEAM);
        if (team == null) return;
        for (String entry : team.getEntries()) {
            final Player player = Bukkit.getPlayerExact(entry);
            if (player != null) player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
        team.unregister();
    }

    /**
     * Spectators see each other as see-through ghosts instead of solid players. Players never see spectators either way.
     * Off by default; set it in {@code createSpectatorHandler()}.
     */
    public SpectatorHandler<P> setTransparent(boolean transparent) {
        this.transparent = transparent;
        return this;
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

        // the team gives the name tag and tab entry their prefix, e.g. "✖ | Tqqn"
        spectatorTeam().addEntry(player.getName());
        if (transparent) {
            // invisible + same team with canSeeFriendlyInvisibles = other spectators see a ghost
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false, false));
        }

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

    private Team spectatorTeam() {
        final Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(SPECTATOR_TEAM);
        if (team == null) {
            team = scoreboard.registerNewTeam(SPECTATOR_TEAM);
            team.prefix(MiniMessage.miniMessage().deserialize(match.messages().spectatorNamePrefix()));
            team.color(match.messages().spectatorNameColor());
            team.setCanSeeFriendlyInvisibles(true);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }
        return team;
    }

    private static MatchTeam teamOf(MatchPlayer player) {
        return player instanceof TeamPlayer teamPlayer ? teamPlayer.getTeam() : null;
    }

    // ---- spectator chat ----

    /**
     * Spectators only chat with other spectators. Runs after the game's own chat listener (HIGH) and replaces its
     * viewers and format, so the game's listener should leave spectators alone.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        final Player player = event.getPlayer();
        final Optional<P> matchPlayer = match.getPlayer(player.getUniqueId());
        if (matchPlayer.isEmpty() || matchPlayer.get().getRole() != Role.SPECTATOR) return;

        final MatchTeam team = teamOf(matchPlayer.get());
        final String teamPrefix = team == null ? "" : match.messages().spectatorChatTeam(team.getColor().getMiniColor(), team.getColor().name());
        final Component prefix = MiniMessage.miniMessage().deserialize(match.messages().spectatorChat(teamPrefix, player.getName()));

        event.viewers().clear();
        event.viewers().add(Bukkit.getConsoleSender());
        Bukkit.getOnlinePlayers().stream().filter(this::isSpectator).forEach(event.viewers()::add);
        event.renderer((source, displayName, message, viewer) -> prefix.append(message.colorIfAbsent(NamedTextColor.GRAY)));
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

    @EventHandler(priority = EventPriority.LOWEST)
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
    public void onPickupArrow(PlayerPickupArrowEvent event) {
        if (isSpectator(event.getPlayer())) event.setCancelled(true);
    }

    /** Hidden spectators still have a hitbox; let arrows and other projectiles fly through them. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (isSpectator(event.getHitEntity())) event.setCancelled(true);
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
