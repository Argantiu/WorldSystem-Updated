/*
 * package de.butzlabben.world.wrapper;
 * 
 * import com.boydti.fawe.bukkit.wrapper.AsyncWorld;
 * //import com.boydti.fawe.util.TaskManager;
 * import com.fastasyncworldedit.core.util.TaskManager;
 * import org.bukkit.Bukkit;
 * import org.bukkit.Material;
 * import org.bukkit.WorldCreator;
 * import org.bukkit.block.Block;
 * 
 * import java.util.Objects;
 * 
 * /*
 * 
 * @author Butzlabben
 * 
 * @since 08.06.2018
 */
/*
 * public class AsyncCreatorAdapter implements CreatorAdapter {
 * 
 * // Create worlds async to close #16
 * 
 * @Override
 * public void create(WorldCreator creator, SystemWorld sw, Runnable r) {
 * 
 * // moved IMP to INSTANCE
 * TaskManager.INSTANCE.async(() -> {
 * AsyncWorld world;
 * if (Bukkit.getWorld(creator.name()) == null)
 * world = AsyncWorld.create(creator);
 * else
 * world =
 * AsyncWorld.wrap(Objects.requireNonNull(Bukkit.getWorld(creator.name())));
 * 
 * Block block = world.getBlockAt(0, -64, 0);
 * block.setType(Material.BEDROCK);
 * 
 * // When you are done
 * world.commit();
 * Bukkit.getWorlds().add(world);
 * if (sw != null)
 * sw.setCreating(false);
 * 
 * // Send the message
 * r.run();
 * });
 * }
 * 
 * }
 */