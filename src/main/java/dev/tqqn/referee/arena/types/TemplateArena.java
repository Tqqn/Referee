package dev.tqqn.referee.arena.types;

import dev.tqqn.referee.arena.AbstractArena;
import dev.tqqn.referee.arena.config.ArenaConfig;
import dev.tqqn.referee.arena.internal.ArenaFiles;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
public class TemplateArena extends AbstractArena {

    public static final String CONFIG_FILE = "arena.yml";

    private final Path template;

    public TemplateArena(Plugin plugin, Path template, String worldName) {
        super(plugin, worldName, template.getFileName().toString());
        this.template = template;
    }

    public TemplateArena(Plugin plugin, Path template) {
        this(plugin, template, "arena");
    }

    /** Picks a random map folder inside {@code mapsDir}, e.g. plugins/YourGame/maps. */
    public static TemplateArena random(Plugin plugin, Path mapsDir) {
        final List<Path> maps;
        try (Stream<Path> children = Files.list(mapsDir)) {
            maps = children.filter(Files::isDirectory).toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read maps folder " + mapsDir, e);
        }
        if (maps.isEmpty()) throw new IllegalStateException("No maps in " + mapsDir);
        return new TemplateArena(plugin, maps.get(ThreadLocalRandom.current().nextInt(maps.size())));
    }

    @Override
    protected CompletableFuture<World> createWorld() {
        if (!Files.isDirectory(template)) {
            return CompletableFuture.failedFuture(new IllegalStateException("Template " + template + " does not exist"));
        }
        return CompletableFuture
                .supplyAsync(() -> {
                    final Path target = worldFolder();
                    ArenaFiles.delete(target); // leftovers from a crash
                    ArenaFiles.copy(template, target);
                    return ArenaConfig.load(target.resolve(CONFIG_FILE), getName());
                }, ArenaFiles.async(plugin))
                .thenApplyAsync(config -> {
                    setName(config.name());
                    putPoints(config.points());
                    final World world = new WorldCreator(worldName).createWorld();
                    if (world == null) throw new IllegalStateException("Failed to load world " + worldName);
                    world.setAutoSave(false);
                    return world;
                }, ArenaFiles.main(plugin));
    }
}
