package dev.tqqn.referee.state;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
public final class StateScope {

    private final Plugin plugin;
    private final PaperCommandManager commandManager;

    private final List<Listener> listeners = new ArrayList<>();
    private final List<BaseCommand> commands = new ArrayList<>();
    private final List<BukkitTask> tasks = new ArrayList<>();

    public StateScope(Plugin plugin, PaperCommandManager commandManager) {
        this.plugin = plugin;
        this.commandManager = commandManager;
    }

    public <T> T register(T object) {
        boolean known = false;
        if (object instanceof Listener listener) {
            Bukkit.getPluginManager().registerEvents(listener, plugin);
            listeners.add(listener);
            known = true;
        }
        if (object instanceof BaseCommand command) {
            commandManager.registerCommand(command);
            commands.add(command);
            Bukkit.getOnlinePlayers().forEach(Player::updateCommands);
            known = true;
        }
        if (object instanceof BukkitTask task) {
            tasks.add(task);
            known = true;
        }
        if (!known) throw new IllegalArgumentException("Can't scope " + object.getClass().getName());
        return object;
    }

    public void close() {
        tasks.forEach(BukkitTask::cancel);
        listeners.forEach(HandlerList::unregisterAll);
        commands.forEach(commandManager::unregisterCommand);
        if (!commands.isEmpty()) Bukkit.getOnlinePlayers().forEach(Player::updateCommands);
        tasks.clear();
        listeners.clear();
        commands.clear();
    }
}
