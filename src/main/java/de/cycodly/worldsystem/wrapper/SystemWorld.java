
package de.cycodly.worldsystem.wrapper;

import com.google.common.base.Preconditions;
import de.cycodly.worldsystem.WorldSystemPlugin;
import de.cycodly.worldsystem.config.*;
import de.cycodly.worldsystem.events.WorldCreateEvent;
import de.cycodly.worldsystem.events.WorldLoadEvent;
import de.cycodly.worldsystem.events.WorldUnloadEvent;
import de.cycodly.worldsystem.util.PlayerPositions;
import de.cycodly.worldsystem.util.PlayerWrapper;
import de.cycodly.worldsystem.util.VersionUtil;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

/**
 * This class represents if a systemworld is loaded or not
 *
 * @author Butzlabben
 * @since 14.02.2018
 */
public class SystemWorld {

    private static final HashMap<String, SystemWorld> cached = new HashMap<>();
    private World w;
    private String worldname;
    private boolean unloading = false;
    private boolean creating = false;

    private BukkitTask unloadLaterTask;

    private SystemWorld(String worldname) {
        this.worldname = worldname;
        w = Bukkit.getWorld(worldname);
    }

    /**
     * This method is the online way to get a system world instance
     *
     * @param worldname as in minecraft
     * @return a systemworld instance if it is a systemworld or null if is not a
     *         systemworld
     * @throws NullPointerException worldname == null
     */
    public static SystemWorld getSystemWorld(String worldname) {
        Preconditions.checkNotNull(worldname, "worldname must not be null");
        if (cached.containsKey(worldname))
            return cached.get(worldname);
        SystemWorld sw = new SystemWorld(worldname);
        if (sw != null && sw.exists()) {
            cached.put(worldname, sw);
            return sw;
        }
        return null;
    }

    /**
     * @param w a world in bukkit, no matter if systemworld or not Trys to unload a
     *          systemworld later, with the given delay in the config
     */
    public static void tryUnloadLater(World w) {
        if (w != null)
            Bukkit.getScheduler().runTaskLater(WorldSystemPlugin.getInstance(), () -> {
                if (w.getPlayers().size() == 0) {
                    SystemWorld sw = SystemWorld.getSystemWorld(w.getName());
                    if (sw != null && sw.isLoaded())
                        sw.unloadLater(w);
                }
            }, 20);
    }

    public static boolean create(Player p, WorldTemplate template) {
        return create(p.getUniqueId(), template);
    }

    /**
     * Trys to create a new systemworld with all entries etc. finally loads the
     * world
     *
     * @param uniqueID UUID of the player to create the world for
     * @return whether it succesfull or not
     */
    public static boolean create(UUID uniqueID, WorldTemplate template) {

        DependenceConfig dc = new DependenceConfig(uniqueID);

        String uuid = uniqueID.toString();
        int id = DependenceConfig.getHighestID() + 1;
        String worldname = "ID" + id + "-" + uuid;
        Player p = Bukkit.getPlayer(uniqueID);

        WorldCreator creator = template.generatorSettings.asWorldCreator(worldname);

        WorldCreateEvent event = new WorldCreateEvent(uniqueID, creator);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return false;

        dc.createNewEntry();

        String worlddir = PluginConfig.getWorlddir();
        File exampleworld = new File(template.getPath());
        if (new File(template.getPath() + "/uid.dat").exists()) {
            new File(template.getPath() + "/uid.dat").delete();
        }

        File newworld = new File(worlddir + "/" + worldname);

        if (exampleworld.isDirectory())
            try {
                FileUtils.copyDirectory(exampleworld, newworld);
            } catch (IOException e) {
                WorldSystemPlugin.logger().log(Level.SEVERE, "Couldn't create world for " + uuid);
                e.printStackTrace();
            }
        else
            newworld.mkdirs();

        WorldConfig.create(uniqueID, template);

        // Move World into Server dir
        File world = new File(worlddir + "/" + worldname);
        if (!world.exists()) {
            world = new File(Bukkit.getWorldContainer(), worldname);
        } else {
            if (new File(Bukkit.getWorldContainer(), worldname).exists()
                    && new File(PluginConfig.getWorlddir() + "/" + worldname).exists()) {
                try {
                    FileUtils.deleteDirectory(new File(Bukkit.getWorldContainer(), worldname));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                FileUtils.moveDirectoryToDirectory(world, Bukkit.getWorldContainer(), false);
            } catch (IOException e) {
                if (p != null && p.isOnline())
                    p.sendMessage(PluginConfig.getPrefix() + "§cError: " + e.getMessage());
                WorldSystemPlugin.logger().log(Level.SEVERE, "Couldn't load world of " + uuid);
                e.printStackTrace();
                return false;
            }
        }

        SystemWorld sw = SystemWorld.getSystemWorld(worldname);
        sw.setCreating(true);

        // Run in scheduler so method returns without delay
        new BukkitRunnable() {
            @Override
            public void run() {
                WorldSystemPlugin.getInstance().getAdapter().create(event.getWorldCreator(), sw, () -> {
                    // Fix for #16
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (p != null && p.isOnline()) {
                                p.sendMessage(MessageConfig.getWorldCreated());
                                SettingsConfig.getCommandsonGet().stream()
                                        .map(s -> s.replace("%player", p.getName()).replace("%world", sw.getName())
                                                .replace("%uuid", p.getUniqueId().toString()))
                                        .forEach(s -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s));
                            }
                        }
                    }.runTask(WorldSystemPlugin.getInstance());
                });
            }
        }.runTaskLater(WorldSystemPlugin.getInstance(), 1);

        return true;
    }

    /**
     * Trys to force-unload this world
     *
     * @param w associated world
     * @throws NullPointerException w == null
     */
    public void directUnload(World w) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(WorldSystemPlugin.getInstance(), () -> directUnload(w));
            return;
        }
        Preconditions.checkNotNull(w, "world must not be null");
        setUnloading(true);
        w.save();
        Chunk[] arrayOfChunk;
        int j = (arrayOfChunk = w.getLoadedChunks()).length;
        for (int i = 0; i < j; i++) {
            Chunk c = arrayOfChunk[i];
            c.unload();
        }

        WorldConfig config = WorldConfig.getWorldConfig(worldname);
        for (Player a : w.getPlayers()) {
            PlayerPositions.instance.saveWorldsPlayerLocation(a, config);
            a.teleport(PluginConfig.getSpawn(a));
            a.setGameMode(PluginConfig.getSpawnGamemode());
        }
        if (unloading) {
            if (Bukkit.unloadWorld(w, true)) {
                File worldinserver = new File(Bukkit.getWorldContainer(), worldname);
                File worlddir = new File(PluginConfig.getWorlddir());
                try {
                    FileUtils.moveDirectoryToDirectory(worldinserver, worlddir, false);
                    Bukkit.getWorlds().remove(w);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Trys to unload this world later, with the given delay in the config
     *
     * @param w the associated world
     * @throws NullPointerException w == null
     */
    private void unloadLater(World w) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(WorldSystemPlugin.getInstance(), () -> unloadLater(w));
            return;
        }

        // Do not start another unload task
        if (unloading) {
            return;
        }

        Preconditions.checkNotNull(w, "world must not be null");
        WorldUnloadEvent event = new WorldUnloadEvent(this);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;
        // Set unloading to true
        setUnloading(true);
        w.save();
        Chunk[] arrayOfChunk;
        int j = (arrayOfChunk = w.getLoadedChunks()).length;
        for (int i = 0; i < j; i++) {
            Chunk c = arrayOfChunk[i];
            c.unload();
        }
        for (Player a : w.getPlayers()) {
            a.teleport(PluginConfig.getSpawn(a));
            a.setGameMode(PluginConfig.getSpawnGamemode());
        }

        unloadLaterTask = Bukkit.getScheduler().runTaskLater(WorldSystemPlugin.getInstance(), () -> {
            // Still in world unloading process?
            if (unloading && w.getPlayers().size() == 0) {
                if (Bukkit.unloadWorld(w, true)) {
                    File worldinserver = new File(Bukkit.getWorldContainer(), worldname);
                    File worlddir = new File(PluginConfig.getWorlddir());
                    try {
                        FileUtils.moveDirectoryToDirectory(worldinserver, worlddir, false);
                        Bukkit.getWorlds().remove(w);
                        setUnloading(false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 20 * PluginConfig.getUnloadingTime());
    }

    /**
     * Trys to load this world, and messages the player about the process
     *
     * @param p the player to teleport on the world
     * @throws NullPointerException     if p is null
     * @throws IllegalArgumentException if player is not online
     */
    public void load(Player p) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(WorldSystemPlugin.getInstance(), () -> load(p));
            return;
        }
        Preconditions.checkNotNull(p, "player must not be null");
        Preconditions.checkArgument(p.isOnline(), "player must be online");

        if (creating) {
            p.sendMessage(MessageConfig.getWorldStillCreating());
            return;
        }

        WorldLoadEvent event = new WorldLoadEvent(p, this);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;

        setUnloading(false);

        p.sendMessage(MessageConfig.getSettingUpWorld());

        // Move World into Server dir
        String worlddir = PluginConfig.getWorlddir();
        File world = new File(worlddir + "/" + worldname);

        if (world.exists()) {
            // Check for duplicated worlds
            File propablyExistingWorld = new File(Bukkit.getWorldContainer(), worldname);
            if (propablyExistingWorld.exists()) {
                WorldSystemPlugin.logger().log(Level.SEVERE, "World " + worldname + " existed twice!");
                try {
                    FileUtils.deleteDirectory(propablyExistingWorld);
                } catch (IOException e) {
                    p.sendMessage(MessageConfig.getUnknownError());
                    e.printStackTrace();
                }
            }

            // Move world if exists
            try {
                FileUtils.moveDirectoryToDirectory(world, Bukkit.getWorldContainer(), false);
            } catch (IOException e) {
                WorldSystemPlugin.logger().log(Level.SEVERE, "Couldn't load world of " + p.getName());
                p.sendMessage(PluginConfig.getPrefix() + "§cError: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Check for old named worlds
        if (worldname.charAt(worldname.length() - 37) == ' ') {
            StringBuilder myName = new StringBuilder(worldname);
            myName.setCharAt(worldname.length() - 37, '-');
            world.renameTo(new File(Bukkit.getWorldContainer(), myName.toString()));
            worldname = myName.toString();
        }

        WorldCreator creator = new WorldCreator(worldname);

        String templateKey = WorldConfig.getWorldConfig(worldname).getTemplateKey();
        WorldTemplate template = WorldTemplateProvider.getInstance().getTemplate(templateKey);
        if (template == null)
            template = WorldTemplateProvider.getInstance().getTemplate(PluginConfig.getDefaultWorldTemplate());

        if (template != null)
            creator = template.generatorSettings.asWorldCreator(worldname);

        World w = Bukkit.getWorld(worldname);
        if (w == null)
            w = Bukkit.createWorld(creator);

        this.w = w;

        Bukkit.getScheduler().scheduleSyncDelayedTask(WorldSystemPlugin.getInstance(), new Runnable() {
            public void run() {
                teleportToWorldSpawn(p);
            }
        }, 10L);

        OfflinePlayer owner = PlayerWrapper.getOfflinePlayer(WorldConfig.getWorldConfig(worldname).getOwner());
        DependenceConfig dc = new DependenceConfig(owner);
        dc.setLastLoaded();
    }

    /**
     * @return if the world is loaded
     */
    public boolean isLoaded() {
        File worldAsDir = new File(Bukkit.getWorldContainer(), worldname + "/worldconfig.yml");
        World w = Bukkit.getWorld(worldname);
        return worldAsDir.exists() && w != null;
    }

    private boolean exists() {
        File worldAsDir = new File(Bukkit.getWorldContainer(), worldname + "/worldconfig.yml");
        if (worldAsDir.exists()) {
            return true;
        }
        worldAsDir = new File(PluginConfig.getWorlddir() + "/" + worldname + "/worldconfig.yml");
        return worldAsDir.exists();
    }

    /**
     * Teleports the player to the world with the given settings in the config
     *
     * @param p player to teleport
     * @throws NullPointerException     if player is null
     * @throws IllegalArgumentException if player is not online
     */
    public void teleportToWorldSpawn(Player p) {
        Preconditions.checkNotNull(p, "player must not be null");
        Preconditions.checkArgument(p.isOnline(), "player must be online");
        PlayerPositions positions = PlayerPositions.instance;

        if (creating) {
            p.sendMessage(MessageConfig.getWorldStillCreating());
            return;
        }

        setUnloading(false);
        w = Bukkit.getWorld(worldname);
        if (w == null)
            return;

        WorldConfig config = WorldConfig.getWorldConfig(worldname);
        if (config.getHome() != null) {
            p.teleport(positions.injectWorldsLocation(p, config, config.getHome()));
        } else {
            if (PluginConfig.useWorldSpawn()) {
                p.teleport(positions.injectWorldsLocation(p, config, PluginConfig.getWorldSpawn(w)));
            } else {
                p.teleport(positions.injectWorldsLocation(p, config, w.getSpawnLocation()));
            }
        }
        if (PluginConfig.isSurvival()) {
            p.setGameMode(GameMode.SURVIVAL);
        } else {
            p.setGameMode(GameMode.CREATIVE); 
        }

        OfflinePlayer owner = PlayerWrapper.getOfflinePlayer(WorldConfig.getWorldConfig(worldname).getOwner());
        DependenceConfig dc = new DependenceConfig(owner);
        dc.setLastLoaded();
    }

    /**
     * @return the world
     */
    public World getWorld() {
        return w;
    }

    public boolean isCreating() {
        return creating;
    }

    public void setCreating(boolean creating) {
        this.creating = creating;
    }

    public void setUnloading(boolean unloading) {
        this.unloading = unloading;

        // Cancel unload task if unloading is set to false
        if (!unloading && unloadLaterTask != null && !VersionUtil.isCancelled(unloadLaterTask)) {
            unloadLaterTask.cancel();
        }
    }

    /**
     * @return the worldname
     */
    public String getName() {
        return worldname;
    }
}