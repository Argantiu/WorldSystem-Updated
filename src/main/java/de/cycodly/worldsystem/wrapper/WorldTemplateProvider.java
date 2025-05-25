package de.cycodly.worldsystem.wrapper;

import de.cycodly.worldsystem.config.PluginConfig;
import de.cycodly.worldsystem.WorldSystemPlugin;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;

/**
 * @author Butzlabben
 * @since 16.12.2018
 */
public class WorldTemplateProvider {

    private static final WorldTemplateProvider INSTANCE = new WorldTemplateProvider();
    private final HashMap<String, WorldTemplate> TEMPLATES = new HashMap<>();

    private WorldTemplateProvider() {
        reload();
    }

    public void reload() {
        TEMPLATES.clear();

        // Get the templates section
        ConfigurationSection section = PluginConfig.getConfig().getConfigurationSection("worldtemplates.templates");

        // If templates section doesn't exist, create default template
        if (section == null) {
            WorldSystemPlugin.logger().info("No templates section found in config.yml. Creating default template...");

            // Create worldsources directory if it doesn't exist
            File sources = new File("plugins/WorldSystem/worldsources");
            if (!sources.exists()) {
                sources.mkdirs();
            }

            // Create default template directory
            File defaultTemplate = new File(sources, "template_default");
            if (!defaultTemplate.exists()) {
                defaultTemplate.mkdirs();
            }

            // Update config.yml with default template
            YamlConfiguration config = PluginConfig.getConfig();
            config.set("worldtemplates.templates.1.name", "template_default");
            try {
                config.save(new File("plugins/WorldSystem/config.yml"));
                WorldSystemPlugin.logger().info("Created default template configuration");

                // Reload the section after creating it
                section = config.getConfigurationSection("worldtemplates.templates");
            } catch (IOException e) {
                WorldSystemPlugin.logger().log(Level.SEVERE, "Failed to save default template configuration", e);
                return;
            }
        }

        // Load templates from config
        for (String key : section.getKeys(false)) {
            String name = section.getString(key + ".name");
            String permission = null;
            if (section.isString(key + ".permission"))
                permission = section.getString(key + ".permission");

            int cost = -1;
            // Get money for money util if needed
            if (section.isInt(key + ".cost"))
                cost = section.getInt(key + ".cost");

            GeneratorSettings settings = new GeneratorSettings();
            if (section.contains(key + ".generator")) {
                ConfigurationSection gSection = section.getConfigurationSection(key + ".generator");
                long seed = gSection.getLong("seed", 0);
                String env = gSection.getString("environment");
                String type = gSection.getString("type");
                String plugin = gSection.getString("plugin");
                settings = new GeneratorSettings(seed, getEnvironment(env), getWorldType(type), plugin);
            }

            TEMPLATES.put(name, new WorldTemplate(name, permission, cost, settings));
        }
    }

    public static WorldTemplateProvider getInstance() {
        return INSTANCE;
    }

    public WorldTemplate getTemplate(String key) {
        return TEMPLATES.get(key);
    }

    public Collection<WorldTemplate> getTemplates() {
        return TEMPLATES.values();
    }

    private World.Environment getEnvironment(String env) {
        if (env == null)
            return null;
        try {
            return World.Environment.valueOf(env);
        } catch (Exception ignored) {
        }
        return null;
    }

    private WorldType getWorldType(String type) {
        if (type == null)
            return null;
        try {
            return WorldType.valueOf(type);
        } catch (Exception ignored) {
        }
        return null;
    }
}
