package dev.tqqn.referee.arena;

import dev.tqqn.referee.arena.config.ArenaPoint;
import dev.tqqn.referee.arena.internal.ArenaFiles;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
public abstract class AbstractArena implements Arena {

    protected final Plugin plugin;
    protected final String worldName;
    private String name;
    private World world;
    private final Map<String, List<ArenaPoint>> points = new HashMap<>();

    protected AbstractArena(Plugin plugin, String worldName, String name) {
        this.plugin = plugin;
        this.worldName = worldName;
        this.name = name;
    }

    /** Create (or copy + load) the world. Must complete on the main thread. */
    protected abstract CompletableFuture<World> createWorld();

    /** Hook after the world exists, on the main thread. Return a future if setup is async (e.g. FAWE paste). */
    protected CompletableFuture<Void> setUp(World world) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public final CompletableFuture<World> load() {
        if (world != null) return CompletableFuture.failedFuture(new IllegalStateException("Arena already loaded"));
        return createWorld()
                .thenCompose(w -> {
                    world = w;
                    return setUp(w).thenApply(v -> w);
                })
                // setUp may finish on an async thread; callers expect main.
                .thenApplyAsync(w -> w, ArenaFiles.main(plugin));
    }

    @Override
    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    @Override
    public World getWorld() {
        if (world == null) throw new IllegalStateException("Arena '" + name + "' is not loaded");
        return world;
    }

    @Override
    public List<Location> points(String key) {
        final World w = getWorld();
        return points.getOrDefault(key, List.of()).stream().map(p -> p.toLocation(w)).toList();
    }

    /** Store points found at runtime, e.g. a spawn read from a pasted schematic. Replaces existing ones. */
    public void setPoints(String key, List<Location> locations) {
        points.put(key, locations.stream().map(ArenaPoint::of).toList());
    }

    public void setPoint(String key, Location location) {
        setPoints(key, List.of(location));
    }

    protected void putPoints(Map<String, List<ArenaPoint>> loaded) {
        points.putAll(loaded);
    }

    protected Path worldFolder() {
        return Bukkit.getWorldContainer().toPath().resolve(worldName);
    }

    @Override
    public void unload() {
        if (world == null) return;
        Bukkit.unloadWorld(world, false);
        world = null;
        ArenaFiles.delete(worldFolder());
    }
}
