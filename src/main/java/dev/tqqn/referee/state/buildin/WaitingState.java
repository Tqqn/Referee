package dev.tqqn.referee.state.buildin;

import dev.tqqn.referee.match.Match;
import dev.tqqn.referee.player.Role;
import dev.tqqn.referee.state.AbstractState;
import dev.tqqn.referee.status.MatchStatus;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
public class WaitingState<G extends Match<?>> extends AbstractState<G> {

    private final int countdown;
    private int left = -1;
    private boolean forceStart;

    public WaitingState(G match, int countdownSeconds) {
        super(match, "Waiting", 0);
        this.countdown = countdownSeconds;
    }

    public WaitingState(G match) {
        this(match, 60);
    }

    @Override
    protected void onEnter() {
        match.setStatus(MatchStatus.LOBBY);
    }

    @Override
    protected void onTick() {
        if (!forceStart && !match.canStart()) {
            if (left >= 0) {
                left = -1;
                match.setStatus(MatchStatus.LOBBY);
                match.broadcast(match.messages().countdownCancelled(), Sound.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_OFF);
            }
            return;
        }

        if (left < 0) {
            left = countdown;
            match.setStatus(MatchStatus.LOBBY_STARTING);
            match.broadcast(match.messages().countdownStarted(left), Sound.ENTITY_PARROT_IMITATE_BLAZE);
            return;
        }

        if (--left <= 0) {
            match.nextState();
            return;
        }
        if (shouldAnnounce(left)) {
            match.broadcast(match.messages().countdownTick(left), Sound.BLOCK_NOTE_BLOCK_PLING);
        }
    }

    @Override
    protected void onExit() {
        match.onLobbyEnd();
    }

    @Override
    public void onLogin(PlayerConnectionValidateLoginEvent event) {
        final MatchStatus status = match.getStatus();
        if (status != MatchStatus.LOBBY && status != MatchStatus.LOBBY_STARTING) {
            refuse(event, match.messages().notJoinable());
        } else if (match.getPlayers().size() >= match.getSettings().maxPlayers()) {
            refuse(event, match.messages().full());
        }
    }

    @Override
    public void onJoin(Player player) {
        match.addPlayer(player, Role.PLAYING);
        resetPlayer(player);
        match.broadcast(match.messages().playerJoined(player.getName(),
                match.getPlayers().size(), match.getSettings().maxPlayers()));
    }

    @Override
    public void onQuit(Player player) {
        match.getPlayers().remove(player.getUniqueId()); // leaving the lobby frees the slot
    }

    /** Admin /start. */
    public void forceStart() {
        forceStart = true;
        if (left < 0 || left > 10) left = 10;
    }

    protected boolean shouldAnnounce(int secondsLeft) {
        return secondsLeft == 30 || secondsLeft == 10 || secondsLeft <= 5;
    }

    protected void resetPlayer(Player player) {
        player.getInventory().clear();
        player.getActivePotionEffects().stream().map(PotionEffect::getType).forEach(player::removePotionEffect);
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setGameMode(GameMode.SURVIVAL);
    }

    private static void refuse(PlayerConnectionValidateLoginEvent event, String message) {
        event.kickMessage(MiniMessage.miniMessage().deserialize(message));
    }
}