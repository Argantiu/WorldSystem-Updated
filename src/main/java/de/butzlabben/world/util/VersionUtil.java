package de.butzlabben.world.util;

import de.butzlabben.world.WorldSystem;
import java.util.logging.Level;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/**
 * @author Butzlabben
 * @since 14.08.2018
 */
public class VersionUtil {
    private static int version = 0;
    private static final Map<String, Integer> VERSION_MAP = Map.ofEntries(
        new SimpleEntry<>("1.21", 21),
        new SimpleEntry<>("1.20", 20),
        new SimpleEntry<>("1.19", 19),
        new SimpleEntry<>("1.18", 18),
        new SimpleEntry<>("1.17", 17),
        new SimpleEntry<>("1.16", 16),
        new SimpleEntry<>("1.15", 15),
        new SimpleEntry<>("1.14", 14),
        new SimpleEntry<>("1.13", 13),
        new SimpleEntry<>("1.12", 12),
        new SimpleEntry<>("1.11", 11),
        new SimpleEntry<>("1.10", 10),
        new SimpleEntry<>("1.9", 9),
        new SimpleEntry<>("1.8", 8),
        new SimpleEntry<>("1.7", 7),
        new SimpleEntry<>("1.6", 6),
        new SimpleEntry<>("1.5", 5),
        new SimpleEntry<>("1.4", 4),
        new SimpleEntry<>("1.3", 3)
    );

    public static int getVersion() {
        if (version == 0) {
            // Detect version
            String detectedVersion = Bukkit.getVersion();

            // Iterate over the map to find a match
            for (Map.Entry<String, Integer> entry : VERSION_MAP.entrySet()) {
                if (detectedVersion.contains(entry.getKey())) {
                    version = entry.getValue();
                    break;
                }
            }

            // Handle unknown version
            if (version == 0) {
                WorldSystem.logger().log(Level.SEVERE, "[WorldSystem] Unknown version: " + detectedVersion);
                WorldSystem.logger().log(Level.SEVERE, "[WorldSystem] Defaulting to version 1.12.2");
                version = 12;
            }
        }
        return version;
    }

    public static boolean isCancelled(BukkitTask task) {
        if (getVersion() <= 12)
            return false;
        return task.isCancelled();
    }
}
