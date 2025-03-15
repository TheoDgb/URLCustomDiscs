package com.mcytdisc;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

public class MCytDISC extends JavaPlugin {

    private String zipFilePath;
    private String resourcePackURL;

    @Override
    public void onEnable() {
        getLogger().info("MCytDISC enabled !");

        // Charger ou créer le fichier de configuration
        loadConfig();

        Objects.requireNonNull(this.getCommand("mcytdisc")).setExecutor(new CommandMCytDisc(this));

        getServer().getPluginManager().registerEvents(new JukeboxListener(this), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("MCytDISC disabled !");
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
                        "# Configuration file for MCytDISC plugin\n" +
                                "\n" +
                                "# zipFilePath: Absolute path to the MCytDiscPack.zip resource pack for editing.\n" +
                                "# Example: C:/Apache24/htdocs/MCytDiscPack.zip\n" +
                                "zipFilePath: \"C:/Apache24/htdocs/MCytDiscPack.zip\"\n" +
                                "\n" +
                                "# resourcePackURL: Download URL of the MCytDiscPack.zip resource pack to update it for players.\n" +
                                "# Example: http://11.11.11.111:80/MCytDiscPack.zip\n" +
                                "resourcePackURL: \"http://11.11.11.111:80/MCytDiscPack.zip\"";
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

    public String getZipFilePath() {
        return zipFilePath;
    }

    public String getResourcePackURL() {
        return resourcePackURL;
    }
}