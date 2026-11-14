package dev.tqqn.referee.match;

import co.aikar.commands.PaperCommandManager;
import dev.tqqn.referee.event.MatchStateChangeEvent;
import dev.tqqn.referee.event.PlayerEliminatedEvent;
import dev.tqqn.referee.message.MatchMessages;
import dev.tqqn.referee.player.MatchPlayer;
import dev.tqqn.referee.player.Role;
import dev.tqqn.referee.spectator.SpectatorHandler;
import dev.tqqn.referee.state.MatchState;
import dev.tqqn.referee.state.StateScope;
import dev.tqqn.referee.status.MatchStatus;
import dev.tqqn.referee.status.StatusPublisher;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.logging.Logger;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
@Getter
public abstract class Match<P extends MatchPlayer> implements Listener {

    private final JavaPlugin plugin;
    private final PaperCommandManager commandManager;
    private final MatchSettings settings;

    private final Map<UUID, P> players = new LinkedHashMap<>();
    private List<MatchState> states = List.of();
    private int stateIndex = -1;
    private StateScope scope;
    private BukkitTask ticker;
    private SpectatorHandler spectatorHandler;

    private MatchStatus status = MatchStatus.NOT_JOINABLE;

    protected Match(JavaPlugin plugin, PaperCommandManager commandManager, MatchSettings settings) {
        this.plugin = plugin;
        this.commandManager = commandManager;
        this.settings = settings;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /** Ordered states; nextState() walks them, switchTo(Class) jumps. */
    protected abstract List<MatchState> createStates();

    protected abstract P createPlayer(UUID uuid);

    public abstract Logger getLogger();

    public MatchMessages messages() {
        return MatchMessages.DEFAULT;
    }

    protected SpectatorHandler createSpectatorHandler() {
        return new SpectatorHandler(this);
    }

    protected StatusPublisher statusPublisher() {
        return StatusPublisher.NONE;
    }

    public void start() {
        states = List.copyOf(createStates());
        if (states.isEmpty()) throw new IllegalStateException("createStates() returned no states");
        spectatorHandler = createSpectatorHandler();
        spectatorHandler.register();
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, () -> getState().ifPresent(MatchState::tick), 20L, 20L);
        switchTo(0);
    }

    public void stop() {
        if (ticker != null) ticker.cancel();
        exitCurrent();
        HandlerList.unregisterAll(this);
        if (spectatorHandler != null) spectatorHandler.unregister();
    }

    public void nextState() {
        if (stateIndex + 1 >= states.size()) { stop(); return; }
        switchTo(stateIndex + 1);
    }

    public void switchTo(Class<? extends MatchState> type) {
        for (int i = 0; i < states.size(); i++) {
            if (type.isInstance(states.get(i))) { switchTo(i); return; }
        }
        throw new IllegalArgumentException("No state of type " + type.getSimpleName());
    }

    private void switchTo(int index) {
        final MatchState from = exitCurrent();
        stateIndex = index;
        final MatchState to = states.get(index);
        Bukkit.getPluginManager().callEvent(new MatchStateChangeEvent(this, from, to));
        scope = new StateScope(plugin, commandManager);
        to.enter(scope);
    }

    private MatchState exitCurrent() {
        final MatchState current = getState().orElse(null);
        if (current != null) {
            current.exit();
            scope.close();
        }
        return current;
    }

    public Optional<MatchState> getState() {
        return stateIndex < 0 ? Optional.empty() : Optional.of(states.get(stateIndex));
    }

    public boolean isIn(Class<? extends MatchState> type) {
        return getState().filter(type::isInstance).isPresent();
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
        statusPublisher().publish(this, status);
    }

    public Optional<P> getPlayer(UUID uuid) {
        return Optional.ofNullable(players.get(uuid));
    }

    public Optional<P> getPlayer(Player player) {
        return getPlayer(player.getUniqueId());
    }

    public P addPlayer(Player player, Role role) {
        final P matchPlayer = players.computeIfAbsent(player.getUniqueId(), this::createPlayer);
        matchPlayer.setRole(role);
        return matchPlayer;
    }

    public void eliminate(P player) {
        if (player.getRole() != Role.PLAYING) return;
        player.setRole(Role.SPECTATOR);
        player.getPlayer().ifPresent(this::spectate);
        Bukkit.getPluginManager().callEvent(new PlayerEliminatedEvent(this, player));
        if (isDecided()) onDecided();
    }

    public void eliminate(UUID uuid) {
        getPlayer(uuid).ifPresent(this::eliminate);
    }

    public void spectate(Player player) {
        spectatorHandler.apply(player);
    }

    protected void onDecided() {
        nextState();
    }

    public List<P> getAlive() {
        return players.values().stream().filter(p -> p.getRole() == Role.PLAYING).toList();
    }

    /** Called by WaitingState when the countdown ends, right before the next state. TeamMatch assigns teams here. */
    public void onLobbyEnd() {}

    /** True when the match is decided (one player / one team left). eliminate() checks it for you. */
    public abstract boolean isDecided();

    /** Default rule; override for other start conditions. */
    public boolean canStart() {
        return getAlive().size() >= settings.minPlayers();
    }

    public void broadcast(String miniMessage) {
        Bukkit.broadcast(MiniMessage.miniMessage().deserialize(miniMessage));
    }

    public void broadcast(String miniMessage, Sound sound) {
        broadcast(miniMessage);
        Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), sound, 1, 1));
    }

    // ---- routing: Bukkit events -> current state ----

    @EventHandler
    public void onLogin(PlayerConnectionValidateLoginEvent event) {
        if (!event.isAllowed()) return;
        getState().ifPresentOrElse(s -> s.onLogin(event),
                () -> event.kickMessage(MiniMessage.miniMessage().deserialize(messages().notJoinable())));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        getState().ifPresent(s -> s.onJoin(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        getState().ifPresent(s -> s.onQuit(event.getPlayer()));
    }
}
