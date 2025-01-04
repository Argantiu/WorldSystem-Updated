package de.butzlabben.world.util;

import com.google.common.base.Preconditions;
//import com.sk89q.worldedit.bukkit.BukkitPlayer;
import de.butzlabben.world.config.PluginConfig;
import de.butzlabben.world.util.database.DatabaseProvider;
import de.butzlabben.world.util.database.DatabaseUtil;

//import org.apache.commons.lang3.ObjectUtils.Null;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class PlayerWrapper {

    private static final DatabaseUtil util = DatabaseProvider.instance.util;

    static {
        checkTables();
    }

    public static void checkTables() {
        if (!util.isConnectionAvailable())
            return;

        String uuidTableName = PluginConfig.getUUIDTableName();
        try {
            PreparedStatement ps = util.prepareStatement("CREATE TABLE IF NOT EXISTS " + uuidTableName +
                    " ( `uuid` VARCHAR(36) NOT NULL , `name` VARCHAR(36) NOT NULL , " +
                    " PRIMARY KEY (`name`))");

            util.executeUpdate(ps);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateDatabase(Player player) {
        if (!util.isConnectionAvailable())
            return;

        Preconditions.checkNotNull(player);
        String uuidTableName = PluginConfig.getUUIDTableName();

        try {
            PreparedStatement ps = util.prepareStatement("REPLACE INTO " + uuidTableName +
                    " (uuid, name) VALUES (?, ?)");
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, player.getName());

            util.executeUpdate(ps);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private static UUID getUUID(String name) {
        String uuidTableName = PluginConfig.getUUIDTableName();

        try {
            PreparedStatement ps = util.prepareStatement("SELECT * FROM " + uuidTableName + " WHERE name=?");

            ps.setString(1, name);
            ResultSet rs = util.executeQuery(ps);
            if (!rs.next())
                return null;

            return UUID.fromString(rs.getString("uuid"));

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static OfflinePlayer getOfflinePlayer(String name) {
        Player player = Bukkit.getPlayer(name); // Check if the player is currently online
        if (player != null) {
            // Return the OfflinePlayer using the online player's UUID
            return Bukkit.getOfflinePlayer(player.getUniqueId());
        }

        // If the player is not online, attempt to get the UUID from the database
        UUID uuid = PlayerWrapper.getUUID(name);
        if (uuid != null) {
            return Bukkit.getOfflinePlayer(uuid);
        }

        // If the UUID is not in the database, return null or handle accordingly
        return null;
    }

    /*
     * public static OfflinePlayer getOfflinePlayer(String name) {
     * Player player = Bukkit.getPlayer(name);
     * if(player != null)
     * return Bukkit.getOfflinePlayer(player.getUniqueId());
     * 
     * UUID uuid = PlayerWrapper.getUUID(name);
     * if(uuid != null)
     * return Bukkit.getOfflinePlayer(uuid);
     * 
     * return Bukkit.getOfflinePlayer(name);
     * }
     */

    public static OfflinePlayer getOfflinePlayer(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid);
    }

}
