package de.cycodly.worldsystem;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;

import de.cycodly.worldsystem.commands.CommandRegistry;
import de.cycodly.worldsystem.config.DependenceConfig;
import de.cycodly.worldsystem.config.GuiConfig;
import de.cycodly.worldsystem.config.MessageConfig;
import de.cycodly.worldsystem.config.PluginConfig;
import de.cycodly.worldsystem.config.SettingsConfig;
import de.cycodly.worldsystem.listener.BlockListener;
import de.cycodly.worldsystem.listener.CommandListener;
import de.cycodly.worldsystem.listener.PlayerListener;
import de.cycodly.worldsystem.listener.WorldEditListener;
import de.cycodly.worldsystem.listener.WorldInitSkipSpawn;
import de.cycodly.worldsystem.util.PapiExtension;
import de.cycodly.worldsystem.util.PlayerPositions;
import de.cycodly.worldsystem.util.VersionUtil;
import de.cycodly.worldsystem.database.DataProvider;
import de.cycodly.worldsystem.wrapper.ICreatorAdapter;
import de.cycodly.worldsystem.wrapper.SystemWorld;
import de.cycodly.worldsystem.wrapper.AsyncCreatorAdapter;

/**
 * @author Butzlabben
 * @author Jubeki
 * @author CrazyCloudCraft
 * @version 2.2.0.1
 * @since 10.07.2017
 */
public class WorldSystem extends JavaPlugin {
    private static final int BSTATS_ID = 25205;
    private static boolean ABOVE_V13 = false;
    private final String PLUGINVERSION = this.getDescription().getVersion();
    private ICreatorAdapter CREATOR_INSTANCE;

    public static void createConfigs() {
        File folder = getInstance().getDataFolder();
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // Create directories
        File sources = new File(folder + "/worldsources");
        File languages = new File(folder + "/languages");
    
        if (!sources.exists()) {
            sources.mkdirs();
        }
        if (!languages.exists()) {
            languages.mkdirs();
        } 
        // Create files
        File guiYML = new File(folder, "gui.yml");
        File configYML = new File(folder, "config.yml");
        File dependenceYML = new File(folder, "dependence.yml");

        //Important: initialize config builders first before usage (or "FILE" will be null)
        PluginConfig.checkConfig(configYML);
        GuiConfig.checkConfig(guiYML);
        SettingsConfig.checkConfig();
        YamlConfiguration confg = YamlConfiguration.loadConfiguration(configYML);

        String[] langYML = { "en", "de", "hu", "nl", "pl", "es", "ru", "fi", "ja", "zh", "fr", PluginConfig.getLanguage() };
        for (String lang : langYML) {
            MessageConfig.checkConfig(new File(languages, lang + ".yml"));
        }
    
        if (!dependenceYML.exists()) {
            try {
                dependenceYML.createNewFile();
                confg.set("HighestID", 0);
                confg.save(dependenceYML);
            } catch (IOException e) {
                WorldSystem.logger().log(Level.SEVERE, "Wasn't able to create DependenceConfig");
                e.printStackTrace();
            }
            new DependenceConfig();
        }

        // Create worlds directory if specified in config
        String worlds = PluginConfig.getWorlddir();
        if (worlds != null && !worlds.isEmpty()) {
            File worldsDir = new File(worlds);
            if (!worldsDir.exists()) {
                worldsDir.mkdirs();
            }
        }
    }

    public static WorldSystem getInstance() {
        return JavaPlugin.getPlugin(WorldSystem.class);
    }

    public static Logger logger() {
        return WorldSystem.getPlugin(WorldSystem.class).getLogger();
    }

    @Override
    public void onEnable() {

        getCommand("ws").setExecutor(new CommandRegistry());
        getCommand("ws").setTabCompleter(new CommandRegistry());

        // Set right version
        if (VersionUtil.getVersion() >= 13) {
            ABOVE_V13 = true;
        }
        createConfigs();

        // Establish database connection
        DataProvider.instance.util.connect();

        // Check if tables exist and create them if necessary. Fix for #34
        PlayerPositions.instance.checkTables();

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerListener(), this);
        pm.registerEvents(new BlockListener(), this);
        pm.registerEvents(new CommandListener(), this);
        pm.registerEvents(new WorldInitSkipSpawn(), this);
        if (pm.getPlugin("WorldEdit") != null)
            pm.registerEvents(new WorldEditListener(), this);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new WorldCheckerRunnable(), 20 * 5,
                20 * PluginConfig.getLagCheckPeriod());

        if (PluginConfig.useGC()) {
            Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new GCRunnable(), 20 * 5,
                    20 * PluginConfig.getGCPeriod());
        }

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (World w : Bukkit.getWorlds()) {
                SystemWorld.tryUnloadLater(w);
            }
        }, 20 * 60 * 2, 20 * 60 * 2);

        // System.setProperty("bstats.relocatecheck", "false");
        Metrics metrics = new Metrics(this, BSTATS_ID);
        metrics.addCustomChart(new SingleLineChart("worlds", DependenceConfig::getHighestID));

        if (Bukkit.getPluginManager().getPlugin("Chunky") != null && PluginConfig.loadWorldsASync()) {

            Bukkit.getConsoleSender().sendMessage(PluginConfig.getPrefix() + "Found Chunky! Worlds now will be created asynchronously");
            CREATOR_INSTANCE = new AsyncCreatorAdapter();

        } else {
            
            CREATOR_INSTANCE = (c, sw, r) -> {
                Bukkit.getWorlds().add(c.createWorld());
                if (sw != null)
                    sw.setCreating(false);
                r.run();
            };
        }

        // Remove old worlds option #28
        if (PluginConfig.shouldDelete()) {
            Bukkit.getConsoleSender().sendMessage(PluginConfig.getPrefix()
                    + "Searching for old worlds to delete if not loaded for " + PluginConfig.deleteAfter() + " days");
            DependenceConfig.checkWorlds();
        }

        Bukkit.getConsoleSender()
                .sendMessage(PluginConfig.getPrefix() + "Successfully enabled WorldSystem v" + PLUGINVERSION);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    new PapiExtension().register();
                    Bukkit.getConsoleSender()
                            .sendMessage(PluginConfig.getPrefix() + "Successfully enabled placeholders");
                } else {
                    Bukkit.getConsoleSender().sendMessage(
                            PluginConfig.getPrefix() + "PlaceholderAPI not found. No placeholders registered");
                }
            }
        }.runTaskLater(this, 20L); // Delay of 20 ticks (1 second)
    }

    @Override
    public void onDisable() {
        for (World w : Bukkit.getWorlds()) {
            SystemWorld sw = SystemWorld.getSystemWorld(w.getName());
            if (sw != null && sw.isLoaded()) {
                sw.directUnload(w);
            }
        }

        // Close database connection
        DataProvider.instance.util.close();

        Bukkit.getConsoleSender()
                .sendMessage(PluginConfig.getPrefix() + "Successfully disabled WorldSystem v" + PLUGINVERSION);
    }

    public ICreatorAdapter getAdapter() {
        return CREATOR_INSTANCE;
    }
}
