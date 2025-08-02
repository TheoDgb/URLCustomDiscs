package com.urlcustomdiscs;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

public class URLCustomDiscs extends JavaPlugin {
    private File discUuidFile;
    private File editResourcePackFolder;
    private String pluginUsageMode;
    private String apiBaseURL;
    private String token;
    private String downloadPackURL;
    private String downloadResourcePackURL;
    private String minecraftServerType;
    private String zipFilePath;
    private String uploadResourcePackURL;
    private final String os = System.getProperty("os.name").toLowerCase();
    private RemoteApiClient  remoteApiClient;
    private ResourcePackManager resourcePackManager;

    @Override
    public void onEnable() {
        getLogger().info("URLCustomDiscs enabled !");
        getLogger().info("Running on OS: " + getOperatingSystem());

        loadFiles(); // Charger ou créer les dossiers
        loadConfig(); // Charger ou créer le fichier de configuration

        remoteApiClient = new RemoteApiClient(this, getApiBaseURL());
        resourcePackManager = new ResourcePackManager(discUuidFile, editResourcePackFolder, downloadResourcePackURL, uploadResourcePackURL); // Initialiser l'instance de ResourcePackManager

        // downloadDependencies();

        Objects.requireNonNull(this.getCommand("customdisc")).setExecutor(new CommandURLCustomDiscs(this, remoteApiClient));

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
                        "# Configuration file for URLCustomDiscs plugin.\n" +
                                "\n" + "\n" +
                                "# If you want to use the preconfigured remote API dedicated to the plugin, fill in the pluginUsageMode field with 'api'.\n" +
                                "# If you want a personal installation and configuration, fill in the pluginUsageMode field with 'self-hosted'.\n" +
                                "pluginUsageMode: \"api\"\n" +
                                "\n" + "\n" +
                                "# ========== REMOTE API MODE CONFIGURATION ==========\n" +
                                "\n" +
                                "# API base URL used to make requests to the remote API.\n" +
                                "apiBaseURL: \"http://167.235.148.137:80\"\n" +
                                "\n" +
                                "# Unique token automatically generated for identification to the remote API when creating your first custom disc.\n" +
                                "token: \"\"\n" +
                                "\n" +
                                "# Download pack URL, automatically generated when creating your first custom disc, using the token to locate your resource pack.\n" +
                                "downloadPackURL: \"\"\n" +
                                "# Once your token and downloadPackURL have been generated, fill in the 'resource-pack=' field in your Minecraft server's 'server.properties' file, following the example below.\n" +
                                "# Example: resource-pack=YOUR_downloadPackURL\n" +
                                "# Then restart your Minecraft server.\n" +
                                "\n" +
                                "# Once you have filled in the 'resource-pack=' field and tested that it works, you can force players to install the resource pack by setting the 'require-resource-pack=' field to true in your Minecraft server's 'server.properties' file, following the example below.\n" +
                                "# Example: require-resource-pack=true\n" +
                                "# Then restart your Minecraft server.\n" +
                                "\n" + "\n" +
                                "# ========== SELF-HOSTED MODE CONFIGURATION ==========\n" +
                                "\n" +
                                "# downloadResourcePackURL: Download URL of the URLCustomDiscsPack.zip resource pack from your personal local web server to update it for players.\n" +
                                "# Example: http://11.111.11.1:80/URLCustomDiscsPack.zip\n" +
                                "downloadResourcePackURL: \"http://11.111.11.1:80/URLCustomDiscsPack.zip\"\n" +
                                "\n" + "\n" +
                                "# If you are using a locally-hosted Minecraft server, fill in the minecraftServerType field with 'local'.\n" +
                                "# If you are using an online-hosted Minecraft server, fill in the minecraftServerType field with 'online'.\n" +
                                "minecraftServerType: \"local\"\n" +
                                "\n" + "\n" +
                                "# If you are using a locally-hosted Minecraft server, fill in the zipFilePath field.\n" +
                                "\n" +
                                "# zipFilePath: Absolute path to the URLCustomDiscsPack.zip resource pack for editing.\n" +
                                "# Example for Windows: C:/Apache24/htdocs/URLCustomDiscsPack.zip\n" +
                                "# Example for Linux: /var/www/html/URLCustomDiscsPack.zip\n" +
                                "zipFilePath: \"C:/Apache24/htdocs/URLCustomDiscsPack.zip\"\n" +
                                "\n" + "\n" +
                                "# If you are using an online-hosted Minecraft server, fill in the uploadResourcePackURL field.\n" +
                                "\n" +
                                "# uploadResourcePackURL: Upload URL of the URLCustomDiscsPack.zip resource pack to replace the old one.\n" +
                                "# Example: http://11.111.11.1:80/upload.php\n" +
                                "uploadResourcePackURL: \"http://11.111.11.1:80/upload.php\"";
                try (FileWriter writer = new FileWriter(configFile)) {
                    writer.write(configContent);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        pluginUsageMode = config.getString("pluginUsageMode");
        apiBaseURL = config.getString("apiBaseURL");
        token = config.getString("token");
        downloadPackURL = config.getString("downloadPackURL");
        downloadResourcePackURL = config.getString("downloadResourcePackURL");
        minecraftServerType = config.getString("minecraftServerType");
        zipFilePath = config.getString("zipFilePath");
        uploadResourcePackURL = config.getString("uploadResourcePackURL");

        if (pluginUsageMode == null) {
            getLogger().warning("config.yml ERROR : The plugin usage mode is not defined.");
        }
        else if (downloadResourcePackURL == null) {
            getLogger().warning("config.yml ERROR : Download resource pack URL is not configured.");
        }
        else if (!Objects.equals(minecraftServerType, "local") && !Objects.equals(minecraftServerType, "online")) {
            getLogger().warning("config.yml ERROR : Minecraft server type is not configured.");
        }
        else if (Objects.equals(minecraftServerType, "local")) {
            if (Objects.equals(zipFilePath, "")) {
                getLogger().warning("config.yml ERROR : Resource pack file path is not configured.");
            }
        }
        else if (Objects.equals(minecraftServerType, "online")) {
            if (Objects.equals(uploadResourcePackURL, "")) {
                getLogger().warning("config.yml ERROR : Download resource pack URL is not configured.");
            }
        }
    }

    public String getOperatingSystem() { return os; }

    private void loadFiles() {
        discUuidFile = new File(getDataFolder(), "discs.json"); // Assign to class variable
        if (!discUuidFile.exists()) {
            try {
                discUuidFile.createNewFile();
                Files.writeString(discUuidFile.toPath(), "{}"); // Initialise un JSON vide
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        editResourcePackFolder = new File(getDataFolder(), "editResourcePack"); // Assign to class variable
        if (!editResourcePackFolder.exists()) {
            editResourcePackFolder.mkdirs();
        }

        File musicFolder = new File(getDataFolder(), "music");
        if (!musicFolder.exists()) musicFolder.mkdirs();
    }

    // Getters / Setters
    public String getPluginUsageMode() { return pluginUsageMode; }
    public String getApiBaseURL() { return apiBaseURL; }
    public String getToken() { return this.token != null ? this.token : ""; }
    public void setToken(String newToken) { this.token = newToken; }
    public String getDownloadPackURL() { return this.downloadPackURL != null ? this.downloadPackURL : ""; }
    public void setDownloadPackURL(String newDownloadPackURL) { this.downloadPackURL = newDownloadPackURL; }
    public String getDownloadResourcePackURL() { return downloadResourcePackURL; }
    public String getMinecraftServerType() { return minecraftServerType; }
    public String getZipFilePath() { return zipFilePath; }
    public ResourcePackManager getResourcePackManager() { return resourcePackManager; }
}
