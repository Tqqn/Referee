package dev.tqqn.referee.arena.internal;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
public final class ArenaFiles {

    /** uid.dat makes Paper refuse a copied world as a duplicate; session.lock belongs to the old process. */
    private static final Set<String> SKIP = Set.of("uid.dat", "session.lock");

    private ArenaFiles() {}

    public static Executor main(Plugin plugin) {
        return task -> {
            if (Bukkit.isPrimaryThread()) task.run();
            else Bukkit.getScheduler().runTask(plugin, task);
        };
    }

    public static Executor async(Plugin plugin) {
        return task -> Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    public static void copy(Path from, Path to) {
        try (Stream<Path> files = Files.walk(from)) {
            for (Path source : (Iterable<Path>) files::iterator) {
                if (SKIP.contains(source.getFileName().toString())) continue;
                final Path target = to.resolve(from.relativize(source).toString());
                if (Files.isDirectory(source)) Files.createDirectories(target);
                else Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Copying " + from + " to " + to + " failed", e);
        }
    }

    public static void delete(Path dir) {
        if (!Files.exists(dir)) return;
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path path : (Iterable<Path>) files.sorted(Comparator.reverseOrder())::iterator) {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Deleting " + dir + " failed", e);
        }
    }
}
