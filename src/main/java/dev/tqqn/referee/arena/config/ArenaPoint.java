package dev.tqqn.referee.arena.config;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
public record ArenaPoint(double x, double y, double z, float yaw, float pitch) {

    public Location toLocation(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }

    public static ArenaPoint of(Location location) {
        return new ArenaPoint(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    /** From YAML: [x, y, z] or [x, y, z, yaw, pitch]. */
    static ArenaPoint parse(String key, Object raw) {
        if (!(raw instanceof List<?> values) || (values.size() != 3 && values.size() != 5)
                || !values.stream().allMatch(Number.class::isInstance)) {
            throw new IllegalArgumentException("points." + key + ": expected [x, y, z] or [x, y, z, yaw, pitch], got " + raw);
        }
        final double[] v = values.stream().mapToDouble(n -> ((Number) n).doubleValue()).toArray();
        return v.length == 3
                ? new ArenaPoint(v[0], v[1], v[2], 0, 0)
                : new ArenaPoint(v[0], v[1], v[2], (float) v[3], (float) v[4]);
    }
}
