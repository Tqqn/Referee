package dev.tqqn.referee.state.buildin;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.tqqn.referee.match.Match;
import dev.tqqn.referee.state.AbstractState;
import dev.tqqn.referee.status.MatchStatus;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
public class EndState<G extends Match<?>> extends AbstractState<G> {

    private static final String BUNGEE_CHANNEL = "BungeeCord";

    private final String lobbyServer;

    public EndState(G match, String lobbyServer, int seconds) {
        super(match, "End", seconds);
        this.lobbyServer = lobbyServer;
    }

    public EndState(G match, String lobbyServer) {
        this(match, lobbyServer, 10);
    }

    @Override
    protected void onEnter() {
        match.setStatus(MatchStatus.ENDING);
        final var messenger = Bukkit.getMessenger();
        if (!messenger.isOutgoingChannelRegistered(match.getPlugin(), BUNGEE_CHANNEL)) {
            messenger.registerOutgoingPluginChannel(match.getPlugin(), BUNGEE_CHANNEL);
        }
    }

    @Override
    protected void onTick() {
        match.broadcast(match.messages().restarting(getTimeLeft()));
    }

    @Override
    protected void onTimerEnd() {
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendMessage(MiniMessage.miniMessage().deserialize(match.messages().sendingToLobby()));
            sendToLobby(p);
        });
        // fixed 2s grace for the proxy to move players; anyone left gets kicked by shutdown.
        Bukkit.getScheduler().runTaskLater(match.getPlugin(), this::onShutdown, 40L);
    }

    protected void sendToLobby(Player player) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(lobbyServer);
        player.sendPluginMessage(match.getPlugin(), BUNGEE_CHANNEL, out.toByteArray());
    }

    protected void onShutdown() {
        Bukkit.shutdown();
    }

    @Override
    public void onLogin(PlayerConnectionValidateLoginEvent event) {
        event.kickMessage(MiniMessage.miniMessage().deserialize(match.messages().ended()));
    }
}
