package com.urlcustomdiscs;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

public class URLCustomDiscs extends JavaPlugin {

    private String zipFilePath;
    private String resourcePackURL;
    private final String os = System.getProperty("os.name").toLowerCase();

    @Override
    public void onEnable() {
        getLogger().info("URLCustomDiscs enabled !");
        getLogger().info("Running on OS: " + getOperatingSystem());

        // Charger ou créer le fichier de configuration
        loadConfig();

        // downloadDependencies();

        Objects.requireNonNull(this.getCommand("customdisc")).setExecutor(new CommandURLCustomDiscs(this));

        getServer().getPluginManager().registerEvents(new JukeboxListener(this), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("URLCustomDiscs disabled !");
    }

    private void loadConfig() {
        File configFile = new File(getDataFolder(), "config.yml");

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Si le fichier de configuration n'existe pas, le créer avec des valeurs par défaut
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                String configContent =
                        "# Configuration file for URLCustomDiscs plugin\n" +
                                "\n" +
                                "# zipFilePath: Absolute path to the URLCustomDiscsPack.zip resource pack for editing.\n" +
                                "# Example for Windows: C:/Apache24/htdocs/URLCustomDiscsPack.zip\n" +
                                "# Example for Linux: /var/www/html/URLCustomDiscsPack.zip\n" +
                                "zipFilePath: \"C:/Apache24/htdocs/URLCustomDiscsPack.zip\"\n" +
                                "\n" +
                                "# resourcePackURL: Download URL of the URLCustomDiscsPack.zip resource pack to update it for players.\n" +
                                "# Example: http://11.11.11.111:80/URLCustomDiscsPack.zip\n" +
                                "resourcePackURL: \"http://11.11.11.111:80/URLCustomDiscsPack.zip\"";
                try (FileWriter writer = new FileWriter(configFile)) {
                    writer.write(configContent);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        zipFilePath = config.getString("zipFilePath");
        resourcePackURL = config.getString("resourcePackURL");
    }

    public String getOperatingSystem() { return os; }

    public String getZipFilePath() {
        return zipFilePath;
    }

    public String getResourcePackURL() {
        return resourcePackURL;
    }
}