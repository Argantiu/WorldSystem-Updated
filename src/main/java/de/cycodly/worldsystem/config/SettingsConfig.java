package de.cycodly.worldsystem.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import de.cycodly.worldsystem.WorldSystemPlugin;
import de.cycodly.worldsystem.wrapper.SystemWorld;

//maybe just merge this config with the WorldConfig
public class SettingsConfig {

    private static final HashMap<String, Long> BORDER_SIZES = new HashMap<>();
    private static File FILE;

    private SettingsConfig() {
    }

    // TODO rebuild this, as it's inperformant and not very beautiful code.
    // only load once, and then reload the things from the disk on command
    //@SuppressWarnings("deprecation")
    public static void editWorld(World w) {
        YamlConfiguration cfg = getConfig();

        SystemWorld sw = SystemWorld.getSystemWorld(w.getName());

        boolean shouldChange = cfg.getBoolean("worldborder.should_change", false);
        if (shouldChange) {
            long size = cfg.getLong("worldborder.normal", 1000);
            if (sw != null && sw.isLoaded()) {
                String worldname = w.getName();
                UUID uuid = UUID.fromString(worldname.substring(worldname.length() - 36));
                Player p = Bukkit.getPlayer(uuid);

                // Only edit worldborder size if owner is online
                if (p != null && p.isOnline()) {

                    // Check permissions
                    for (String string : BORDER_SIZES.keySet()) {
                        if (p.hasPermission(string) && size < BORDER_SIZES.get(string)) {
                            size = BORDER_SIZES.get(string);
                        }
                    }

                    w.getWorldBorder().setSize(size);
                }

                if (cfg.getBoolean("worldborder.center.as_spawn", true)) {
                    if (PluginConfig.useWorldSpawn()) {
                        w.getWorldBorder().setCenter(PluginConfig.getWorldSpawn(w));
                    } else {
                        w.getWorldBorder().setCenter(w.getSpawnLocation());
                    }
                } else {
                    Location loc = new Location(w, cfg.getDouble("worldborder.center.x", 0),
                            cfg.getDouble("worldborder.center.y", 20), cfg.getDouble("worldborder.center.z", 0));
                    w.getWorldBorder().setCenter(loc);
                }
                if (cfg.getBoolean("worldborder.center.as_home")) {
                    WorldConfig config = WorldConfig.getWorldConfig(w.getName());
                    if (config.getHome() != null)
                        w.getWorldBorder().setCenter(config.getHome());
                }
            }
        }

        // Fix for #17
        String diff = cfg.getString("difficulty");
        try {
            Difficulty difficulty = Difficulty.valueOf(diff.toUpperCase());
            w.setDifficulty(difficulty);
        } catch (IllegalArgumentException e) {
            Bukkit.getConsoleSender()
                    .sendMessage(PluginConfig.getPrefix() + "§cUnknown difficulty \"" + diff + "\" in settings.yml");
        }

        if (w.isGameRule("announceAdvancements"))
            w.setGameRuleValue("announceAdvancements", cfg.getString("announceAdvancements"));

        if (w.isGameRule("commandBlockOutput"))
            w.setGameRuleValue("commandBlockOutput", cfg.getString("commandBlockOutput"));

        if (w.isGameRule("disableElytraMovementCheck"))
            w.setGameRuleValue("disableElytraMovementCheck", cfg.getString("disableElytraMovementCheck"));

        if (w.isGameRule("doDaylightCycle"))
            w.setGameRuleValue("doDaylightCycle", cfg.getString("doDaylightCycle"));

        if (w.isGameRule("doEntityDrops"))
            w.setGameRuleValue("doEntityDrops", cfg.getString("doEntityDrops"));

        if (w.isGameRule("doFireTick"))
            w.setGameRuleValue("doFireTick", cfg.getString("doFireTick"));

        if (w.isGameRule("doLimitedCrafting"))
            w.setGameRuleValue("doLimitedCrafting", cfg.getString("doLimitedCrafting"));

        if (w.isGameRule("doMobLoot"))
            w.setGameRuleValue("doMobLoot", cfg.getString("doMobLoot"));

        if (w.isGameRule("doMobSpawning"))
            w.setGameRuleValue("doMobSpawning", cfg.getString("doMobSpawning"));

        if (w.isGameRule("doTileDrops"))
            w.setGameRuleValue("doTileDrops", cfg.getString("doTileDrops"));

        if (w.isGameRule("doWeatherCycle"))
            w.setGameRuleValue("doWeatherCycle", cfg.getString("doWeatherCycle"));

        if (w.isGameRule("gameLoopFunction"))
            w.setGameRuleValue("gameLoopFunction", cfg.getString("gameLoopFunction"));

        if (w.isGameRule("keepInventory"))
            w.setGameRuleValue("keepInventory", cfg.getString("keepInventory"));

        if (w.isGameRule("logAdminCommands"))
            w.setGameRuleValue("logAdminCommands", cfg.getString("logAdminCommands"));

        if (w.isGameRule("maxCommandChainLength"))
            w.setGameRuleValue("maxCommandChainLength", cfg.getString("maxCommandChainLength"));

        if (w.isGameRule("maxEntityCramming"))
            w.setGameRuleValue("maxEntityCramming", cfg.getString("maxEntityCramming"));

        if (w.isGameRule("mobGriefing"))
            w.setGameRuleValue("mobGriefing", cfg.getString("mobGriefing"));

        if (w.isGameRule("naturalRegeneration"))
            w.setGameRuleValue("naturalRegeneration", cfg.getString("naturalRegeneration"));

        if (w.isGameRule("randomTickSpeed"))
            w.setGameRuleValue("randomTickSpeed", cfg.getString("randomTickSpeed"));

        if (w.isGameRule("reducedDebugInfo"))
            w.setGameRuleValue("reducedDebugInfo", cfg.getString("reducedDebugInfo"));

        if (w.isGameRule("sendCommandFeedback"))
            w.setGameRuleValue("sendCommandFeedback", cfg.getString("sendCommandFeedback"));

        if (w.isGameRule("showDeathMessages"))
            w.setGameRuleValue("showDeathMessages", cfg.getString("showDeathMessages"));

        if (w.isGameRule("spawnRadius"))
            w.setGameRuleValue("spawnRadius", cfg.getString("spawnRadius"));

        if (w.isGameRule("spectatorsGenerateChunks"))
            w.setGameRuleValue("spectatorsGenerateChunks", cfg.getString("spectatorsGenerateChunks"));
    }

    private static YamlConfiguration getConfig() {
        try {
            return YamlConfiguration
                    .loadConfiguration(new InputStreamReader(new FileInputStream(FILE), StandardCharsets.UTF_8));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void checkConfig() {
        File FILE = new File(WorldSystemPlugin.getInstance().getDataFolder(), "settings.yml");
        SettingsConfig.FILE = FILE;
        if (!FILE.exists()) {
            try {
                // Create parent directories if they don't exist
                if (!FILE.getParentFile().exists()) {
                    FILE.getParentFile().mkdirs();
                }

                // Try to get the config from resources
                InputStream in = JavaPlugin.getPlugin(WorldSystemPlugin.class).getResource("settings.yml");
                if (in != null) {
                    Files.copy(in, FILE.toPath());
                    in.close();
                } else {
                    // Create default settings if resource not found
                    WorldSystemPlugin.logger().log(Level.WARNING,
                            "Could not find settings.yml in resources, creating default settings");
                    YamlConfiguration config = new YamlConfiguration();

                    // World border settings
                    config.set("worldborder.should_change", true);
                    config.set("worldborder.normal", 1000);
                    config.set("worldborder.center.as_spawn", true);
                    config.set("worldborder.center.as_home", false);
                    config.set("worldborder.center.x", 0);
                    config.set("worldborder.center.y", 64);
                    config.set("worldborder.center.z", 0);
                    config.set("worldborder.ranks.ws.big", 2000);
                    config.set("worldborder.ranks.ws.large", 5000);

                    // Game rules
                    config.set("difficulty", "NORMAL");
                    config.set("announceAdvancements", "true");
                    config.set("commandBlockOutput", "true");
                    config.set("disableElytraMovementCheck", "false");
                    config.set("doDaylightCycle", "true");
                    config.set("doEntityDrops", "true");
                    config.set("doFireTick", "true");
                    config.set("doLimitedCrafting", "false");
                    config.set("doMobLoot", "true");
                    config.set("doMobSpawning", "true");
                    config.set("doTileDrops", "true");
                    config.set("doWeatherCycle", "true");
                    config.set("keepInventory", "false");
                    config.set("logAdminCommands", "true");
                    config.set("maxCommandChainLength", "65536");
                    config.set("maxEntityCramming", "24");
                    config.set("mobGriefing", "true");
                    config.set("naturalRegeneration", "true");
                    config.set("randomTickSpeed", "3");
                    config.set("reducedDebugInfo", "false");
                    config.set("sendCommandFeedback", "true");
                    config.set("showDeathMessages", "true");
                    config.set("spawnRadius", "10");
                    config.set("spectatorsGenerateChunks", "true");

                    // Commands to execute when getting a world
                    config.set("commands_on_get", new ArrayList<String>());

                    config.save(FILE);
                }
            } catch (IOException e) {
                WorldSystemPlugin.logger().log(Level.SEVERE, "Wasn't able to create settings.yml", e);
                return;
            }
        }

        // Load world border settings
        YamlConfiguration cfg = getConfig();
        if (cfg != null && cfg.isConfigurationSection("worldborder.ranks")) {

            for (String s : cfg.getConfigurationSection("worldborder.ranks").getKeys(true)) {
                if (cfg.isInt("worldborder.ranks." + s) || cfg.isLong("worldborder.ranks." + s))
                    BORDER_SIZES.put(s, cfg.getLong("worldborder.ranks." + s));
            }
        }
    }

    /**
     * @return the commands specified in settings.yml on /ws get
     */
    public static List<String> getCommandsonGet() {
        YamlConfiguration cfg = getConfig();
        return cfg.getStringList("commands_on_get");
    }
}
