package de.butzlabben.world.util;

import de.butzlabben.world.WorldSystem;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
//import org.jetbrains.annotations.NotNull;
import de.butzlabben.world.config.DependenceConfig;
import de.butzlabben.world.config.WorldConfig;
import de.butzlabben.world.wrapper.SystemWorld;
import de.butzlabben.world.wrapper.WorldPlayer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PapiExtension extends PlaceholderExpansion {

    private final WorldSystem worldSystem = WorldSystem.getInstance();

    @Override
    public String getAuthor() {
        return "Cycodly";
    }

    @Override
    public String getIdentifier() {
        return "worldsystem";
    }

    @Override
    public String getVersion() {
        return worldSystem.getDescription().getVersion();
    }
    /*
     * @Override
     * public String onRequest(OfflinePlayer player, String params) {
     * 
     * if(params.equalsIgnoreCase("name")) {
     * return player == null ? null : player.getName();
     * }
     * 
     * if(params.equalsIgnoreCase("placeholder1")) {
     * return "Ich bin ne Placeholder du lappn";
     * }
     * 
     * if(params.equalsIgnoreCase("placeholder2")) {
     * return "Placeholder Text 2";
     * }
     * 
     * return null;
     * }
     */

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        DependenceConfig config = new DependenceConfig(player);

        String[] args = params.split("_");
        if (args[0].equalsIgnoreCase("world") && args[1].equalsIgnoreCase("member")) {

            Player playerOnline = player.getPlayer();
            if (playerOnline == null) {
                return "Can't find playername.";
            }
            WorldConfig wc = WorldConfig.getWorldConfig(playerOnline.getWorld().getName());
            List<String> members = new ArrayList<>(wc.getMembersWithNames().values());
            int member;
            try {
                member = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "No placeholder named\"" + getIdentifier() + "_" + params + "\" is known");
            }
            return members.get(member);
        }

        switch (params) {
            case "has_world":
                return new DependenceConfig(player).hasWorld() + "";
            // @@ -48,16 +69,16 @@ public String onRequest(OfflinePlayer p, String params) {
            case "is_creator":
                WorldPlayer user = new WorldPlayer(Objects.requireNonNull(Bukkit.getPlayer(player.getUniqueId())));
                if (!user.isOnSystemWorld())
                    return "false";
                return user.isOwnerofWorld() + "";
            case "player_world_name":
                if (!config.hasWorld())
                    return "none";
                else
                    return config.getWorldname();
            case "world_loaded":
                if (!config.hasWorld())
                    return "none";
                return Objects.requireNonNull(SystemWorld.getSystemWorld(config.getWorldname())).isLoaded() + "";
            case "display_world_name":
                if (!player.isOnline()) {
                    if (!config.hasWorld()) {
                        Player p1 = player.getPlayer();
                        if (p1 != null && p1.isOnline())
                            return Objects.requireNonNull(p1.getLocation().getWorld()).getName();
                        return "none";
                    }
                    return config.getOwner().getName();
                } else {
                    World world = ((Player) player).getWorld();
                    if (WorldConfig.exists(world.getName()))
                        return WorldConfig.getWorldConfig(world.getName()).getOwnerName();
                    return world.getName();
                }
            default:
                throw new IllegalArgumentException(
                        "No placeholder named\"" + getIdentifier() + "_" + params + "\" is known");
        }
    }
}