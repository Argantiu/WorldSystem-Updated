
package de.cycodly.worldsystem.wrapper;

import de.cycodly.worldsystem.WorldSystem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.popcraft.chunky.api.ChunkyAPI;
import org.popcraft.chunky.api.event.task.GenerationCompleteEvent;
import java.util.logging.Level;

import java.util.Objects;

public class AsyncCreatorAdapter implements ICreatorAdapter {

    private final WorldSystem worldSystem = WorldSystem.getInstance();
    private boolean generationComplete = false;

    // Create worlds async
    @Override
    public void create(WorldCreator creator, SystemWorld sw, Runnable r) {
        // Load ChunkyAPI service
        ChunkyAPI chunky = Bukkit.getServer().getServicesManager().load(ChunkyAPI.class);
        String worldName = creator.name();
        WorldCreator world;

        if (Bukkit.getWorld(worldName) == null && chunky.version() == 0) {
            // Start Chunky world generation task asynchronously
            Bukkit.getWorlds().add(creator.createWorld());
            WorldSystem.logger().log(Level.INFO,"World " + worldName + " starting Chunky generation...");
            chunky.startTask(worldName, "square", 0, 0, 100, 100, "concentric");

            // Set up callback when the generation is complete
            chunky.onGenerationComplete(event -> {
                // Once generation is complete, set the block and call the runnable
                WorldSystem.logger().log(Level.INFO, "World generation completed for " + worldName);
                Block block = Bukkit.getWorld(worldName).getBlockAt(0, -64, 0);
                block.setType(Material.BEDROCK);

                if (sw != null) {
                    sw.setCreating(false);
                }

                // Execute the Runnable after generation is complete
                Bukkit.getScheduler().runTask(worldSystem, r);
            });
        } else {
            WorldSystem.logger().log(Level.SEVERE, "World " + worldName + " already exists, no generation.");
            // If the world already exists, execute the Runnable immediately
            Bukkit.getScheduler().runTask(worldSystem, r);
        }
    }
}