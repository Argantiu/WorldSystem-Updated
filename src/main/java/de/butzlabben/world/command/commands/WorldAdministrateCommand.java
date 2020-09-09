package de.butzlabben.world.command.commands;

import com.boydti.fawe.configuration.file.YamlConfiguration;
import de.butzlabben.world.WorldSystem;
import de.butzlabben.world.config.DependenceConfig;
import de.butzlabben.world.config.MessageConfig;
import de.butzlabben.world.config.PluginConfig;
import de.butzlabben.world.config.WorldConfig;
import de.butzlabben.world.event.WorldAddmemberEvent;
import de.butzlabben.world.event.WorldDeleteEvent;
import de.butzlabben.world.event.WorldRemovememberEvent;
import de.butzlabben.world.util.PlayerPositions;
import de.butzlabben.world.wrapper.SystemWorld;
import de.butzlabben.world.wrapper.WorldPlayer;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class WorldAdministrateCommand {

    public boolean delMemberCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
        if (args.length < 2) {
            p.sendMessage(MessageConfig.getWrongUsage().replaceAll("%usage", "/ws delmember <Player>"));
            return false;
        }

        DependenceConfig dc = new DependenceConfig(p);
        if (!dc.hasWorld()) {
            p.sendMessage(MessageConfig.getNoWorldOwn());
            return false;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer a = Bukkit.getOfflinePlayer(args[1]);
        WorldConfig wc = WorldConfig.getWorldConfig(dc.getWorldname());
        if (a == null) {
            p.sendMessage(MessageConfig.getNotRegistered().replaceAll("%player", args[1]));
            return false;
        } else if (!wc.isMember(a.getUniqueId())) {
            p.sendMessage(MessageConfig.getNoMemberOwn());
            return false;
        }
        WorldRemovememberEvent event = new WorldRemovememberEvent(a.getUniqueId(), dc.getWorldname(), p);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return false;

        if (a.isOnline()) {
            Player t = (Player) a;
            if (t.getWorld().getName().equals(new DependenceConfig(p).getWorldname())) {
                t.teleport(PluginConfig.getSpawn(t));
                t.setGameMode(PluginConfig.getSpawnGamemode());
            }
        }

        wc.removeMember(a.getUniqueId());
        try {
            wc.save();
        } catch (IOException e) {
            p.sendMessage(MessageConfig.getUnknownError());
            e.printStackTrace();
        }
        p.sendMessage(MessageConfig.getMemberRemoved().replaceAll("%player", a.getName()));
            return true;
        } else {
            sender.sendMessage("No Console"); //TODO Get Config
            return false;
        }
    }

    public boolean deleteCommand(CommandSender sender, Command command, String label, String[] args) {
        CommandSender cs = sender;

        if (args.length < 2) {
            cs.sendMessage(MessageConfig.getWrongUsage().replaceAll("%usage", "/ws delete <Player>"));
            return false;
        }

        DependenceConfig dc = new DependenceConfig(args[1]);
        if (!dc.hasWorld()) {
            cs.sendMessage(MessageConfig.getNoWorldOther());
            return false;
        }

        String worldname = dc.getWorldname();
        SystemWorld sw = SystemWorld.getSystemWorld(worldname);
        WorldDeleteEvent event = new WorldDeleteEvent(cs, sw);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return false;

        if (sw != null && sw.isLoaded())
            sw.directUnload(Bukkit.getWorld(worldname));

        WorldConfig config = WorldConfig.getWorldConfig(worldname);
        // Delete unnecessary positions
        PlayerPositions.instance.deletePositions(config);

        Bukkit.getScheduler().runTaskLater(WorldSystem.getInstance(), () -> {
            OfflinePlayer op = dc.getOwner();

            String uuid = op.getUniqueId().toString();
            File dir = new File(PluginConfig.getWorlddir() + "/" + worldname);
            if (!dir.exists())
                dir = new File(Bukkit.getWorldContainer(), worldname);
            if (dir.exists())
                try {
                    FileUtils.deleteDirectory(dir);
                } catch (Exception e) {
                    cs.sendMessage(MessageConfig.getUnknownError());
                    e.printStackTrace();
                }
            File dconfig = new File("plugins//WorldSystem//dependence.yml");
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dconfig);
            cfg.set("Dependences." + uuid + ".ID", null);
            cfg.set("Dependences." + uuid + ".ActualName", null);
            cfg.set("Dependences." + uuid, null);
            try {
                cfg.save(dconfig);
            } catch (Exception e) {
                cs.sendMessage(MessageConfig.getUnknownError());
                e.printStackTrace();
            }
            cs.sendMessage(MessageConfig.getDeleteWorldOther().replaceAll("%player", op.getName()));
            if (op.isOnline()) {
                Player p1 = Bukkit.getPlayer(op.getUniqueId());
                p1.sendMessage(MessageConfig.getDeleteWorldOwn());
            }
        }, 10);
        return true;
    }

    public boolean addMemberCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
        if (args.length < 2) {
            p.sendMessage(MessageConfig.getWrongUsage().replaceAll("%usage", "/ws addmember <Player>"));
            return false;
        }

        DependenceConfig dc = new DependenceConfig(p);
        if (!dc.hasWorld()) {
            p.sendMessage(MessageConfig.getNoWorldOwn());
            return false;
        }
        @SuppressWarnings("deprecation")
        OfflinePlayer a = Bukkit.getOfflinePlayer(args[1]);
        WorldConfig wc = WorldConfig.getWorldConfig(dc.getWorldname());
        if (a == null) {
            p.sendMessage(MessageConfig.getNotRegistered().replaceAll("%player", args[1]));
            return false;
        } else if (wc.isMember(a.getUniqueId())) {
            p.sendMessage(MessageConfig.getAlreadyMember());
            return false;
        }

        WorldAddmemberEvent event = new WorldAddmemberEvent(a.getUniqueId(), dc.getWorldname(), p);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return false;

        wc.addMember(a.getUniqueId());
        try {
            wc.save();
        } catch (IOException e) {
            p.sendMessage(MessageConfig.getUnknownError());
            e.printStackTrace();
        }
        p.sendMessage(MessageConfig.getMemberAdded().replaceAll("%player", a.getName()));
            return true;
        } else {
            sender.sendMessage("No Console"); //TODO Get Config
            return false;
        }
    }

    public boolean toggleTeleportCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (args.length < 2) {
                p.sendMessage(MessageConfig.getWrongUsage().replaceAll("%usage", "/ws toggletp <Player>"));
                return false;
            }

            DependenceConfig dc = new DependenceConfig(p);
            if (!dc.hasWorld()) {
                p.sendMessage(MessageConfig.getNoWorldOwn());
                return false;
            }
            @SuppressWarnings("deprecation")
            OfflinePlayer a = Bukkit.getOfflinePlayer(args[1]);
            WorldConfig wc = WorldConfig.getWorldConfig(dc.getWorldname());
            if (!wc.isMember(a.getUniqueId())) {
                p.sendMessage(MessageConfig.getNoMemberOwn());
                return false;
            }
            WorldPlayer wp = new WorldPlayer(a, dc.getWorldname());
            if (wp.isOwnerofWorld()) {
                p.sendMessage(PluginConfig.getPrefix() + "§cYou are the owner");
                return false;
            }
            if (wp.toggleTeleport()) {
                p.sendMessage(MessageConfig.getToggleTeleportEnabled().replaceAll("%player", a.getName()));
            } else {
                p.sendMessage(MessageConfig.getToggleTeleportDisabled().replaceAll("%player", a.getName()));
            }
            return true;
        } else {
            sender.sendMessage("No Console"); //TODO Get Config
            return false;
        }
    }

    public boolean toggleGamemodeCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
        if (args.length < 2) {
            p.sendMessage(MessageConfig.getWrongUsage().replaceAll("%usage", "/ws togglegm <Player>"));
            return false;
        }

        DependenceConfig dc = new DependenceConfig(p);
        if (!dc.hasWorld()) {
            p.sendMessage(MessageConfig.getNoWorldOwn());
            return false;
        }
        @SuppressWarnings("deprecation")
        OfflinePlayer a = Bukkit.getOfflinePlayer(args[1]);
        WorldConfig wc = WorldConfig.getWorldConfig(dc.getWorldname());
        if (!wc.isMember(a.getUniqueId())) {
            p.sendMessage(MessageConfig.getNoMemberOwn());
            return false;
        }
        WorldPlayer wp = new WorldPlayer(a, dc.getWorldname());
        if (wp.isOwnerofWorld()) {
            p.sendMessage(PluginConfig.getPrefix() + "§cYou are the owner");
            return false;
        }
        if (wp.toggleGamemode()) {
            p.sendMessage(MessageConfig.getToggleGameModeEnabled().replaceAll("%player", a.getName()));
        } else {
            p.sendMessage(MessageConfig.getToggleGameModeDisabled().replaceAll("%player", a.getName()));
        }
            return true;
        } else {
            sender.sendMessage("No Console"); //TODO Get Config
            return false;
        }
    }

    public boolean toggleWorldeditCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (args.length < 2) {
                p.sendMessage(MessageConfig.getWrongUsage().replaceAll("%usage", "/ws togglewe <Player>"));
                return false;
            }

            DependenceConfig dc = new DependenceConfig(p);
            if (!dc.hasWorld()) {
                p.sendMessage(MessageConfig.getNoWorldOwn());
                return false;
            }
            @SuppressWarnings("deprecation")
            OfflinePlayer a = Bukkit.getOfflinePlayer(args[1]);
            WorldConfig wc = WorldConfig.getWorldConfig(dc.getWorldname());
            if (!wc.isMember(a.getUniqueId())) {
                p.sendMessage(MessageConfig.getNoMemberOwn());
                return false;
            }
            WorldPlayer wp = new WorldPlayer(a, dc.getWorldname());
            if (wp.isOwnerofWorld()) {
                p.sendMessage(PluginConfig.getPrefix() + "§cYou are the owner");
                return false;
            }
            if (wp.toggleWorldEdit()) {
                p.sendMessage(MessageConfig.getToggleWorldeditEnabled().replaceAll("%player", a.getName()));
            } else {
                p.sendMessage(MessageConfig.getToggleWorldeditDisabled().replaceAll("%player", a.getName()));
            }
            return true;
        } else {
            sender.sendMessage("No Console"); //TODO Get Config
            return false;
        }
    }

    public boolean toggleBuildCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
            if (args.length < 2) {
                p.sendMessage(MessageConfig.getWrongUsage().replaceAll("%usage", "/ws togglebuild <Player>"));
                return false;
            }

            DependenceConfig dc = new DependenceConfig(p);
            if (!dc.hasWorld()) {
                p.sendMessage(MessageConfig.getNoWorldOwn());
                return false;
            }
            @SuppressWarnings("deprecation")
            OfflinePlayer a = Bukkit.getOfflinePlayer(args[1]);
            WorldConfig wc = WorldConfig.getWorldConfig(dc.getWorldname());
            if (!wc.isMember(a.getUniqueId())) {
                p.sendMessage(MessageConfig.getNoMemberOwn());
                return false;
            }
            WorldPlayer wp = new WorldPlayer(a, dc.getWorldname());
            if (wp.isOwnerofWorld()) {
                p.sendMessage(PluginConfig.getPrefix() + "§cYou are the owner");
                return false;
            }
            if (wp.toggleBuild()) {
                p.sendMessage(MessageConfig.getToggleBuildEnabled().replaceAll("%player", a.getName()));
            } else {
                p.sendMessage(MessageConfig.getToggleBuildDisabled().replaceAll("%player", a.getName()));
            }
                return true;
            } else {
                sender.sendMessage("No Console"); //TODO Get Config
                return false;
            }
        }
}
