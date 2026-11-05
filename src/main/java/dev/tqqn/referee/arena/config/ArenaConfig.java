package dev.tqqn.referee.arena.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
public record ArenaConfig(String name, Map<String, List<ArenaPoint>> points) {

    public static ArenaConfig empty(String name) {
        return new ArenaConfig(name, Map.of());
    }

    /** Missing file = no points, name falls back. */
    public static ArenaConfig load(Path file, String fallbackName) {
        if (!Files.isRegularFile(file)) return empty(fallbackName);
        return parse(YamlConfiguration.loadConfiguration(file.toFile()), fallbackName);
    }

    /** Each point key holds one point [x, y, z(, yaw, pitch)] or a list of them. */
    public static ArenaConfig parse(ConfigurationSection yaml, String fallbackName) {
        final Map<String, List<ArenaPoint>> points = new HashMap<>();
        final ConfigurationSection section = yaml.getConfigurationSection("points");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                final List<?> raw = section.getList(key);
                if (raw == null || raw.isEmpty()) {
                    throw new IllegalArgumentException("points." + key + ": expected a point or a list of points");
                }
                points.put(key, raw.getFirst() instanceof Number
                        ? List.of(ArenaPoint.parse(key, raw))
                        : raw.stream().map(r -> ArenaPoint.parse(key, r)).toList());
            }
        }
        return new ArenaConfig(yaml.getString("name", fallbackName), Map.copyOf(points));
    }
}
