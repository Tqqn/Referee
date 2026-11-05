package dev.tqqn.referee.arena.types;

import dev.tqqn.referee.arena.AbstractArena;
import dev.tqqn.referee.arena.internal.ArenaFiles;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
public class GeneratedArena extends AbstractArena {

    private final WorldCreator creator;

    public GeneratedArena(Plugin plugin, WorldCreator creator, String name) {
        super(plugin, creator.name(), name);
        this.creator = creator;
    }

    public GeneratedArena(Plugin plugin, WorldCreator creator) {
        this(plugin, creator, creator.name());
    }

    @Override
    protected CompletableFuture<World> createWorld() {
        return CompletableFuture
                .runAsync(() -> ArenaFiles.delete(worldFolder()), ArenaFiles.async(plugin)) // old world from last run
                .thenApplyAsync(v -> {
                    final World world = creator.createWorld();
                    if (world == null) throw new IllegalStateException("Failed to create world " + worldName);
                    world.setAutoSave(false);
                    return world;
                }, ArenaFiles.main(plugin));
    }
}
