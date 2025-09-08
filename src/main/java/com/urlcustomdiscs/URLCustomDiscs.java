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
    private final String os = System.getProperty("os.name").toLowerCase();
    private String minecraftServerVersion;

    private File audioToSendFolder;
    private File binFolder;
    private File editResourcePackFolder;
    private File tempAudioFolder;
    private File tempUnpackedFolder;
    private File editOnlyModeReferenceResourcePackFolder;

    private String pluginUsageMode;
    private String apiBaseURL;
    private String token;
    private String apiDownloadResourcePackURL;
    private Boolean localYtDlp;
    private String downloadResourcePackURL;
    private String resourcePackAccessMode;
    private String zipFileAbsolutePath;
    private String uploadResourcePackURL;
    private String editOnlyModeZipFilePath;
    private String duplicatedZipFilePath;

    @Override
    public void onEnable() {
        getLogger().info("URLCustomDiscs enabled !");
        getLogger().info("Running on OS: " + getOperatingSystem());

        // Retrieve the Minecraft server version
        String fullVersion = getServer().getBukkitVersion(); // "1.21.4-R0.1-SNAPSHOT"
        minecraftServerVersion = fullVersion.split("-")[0];  // "1.21.4"
        getLogger().info("Detected Minecraft server version: " + minecraftServerVersion);

        if (minecraftServerVersion == null) {
            minecraftServerVersion = "1.21.4";
            getLogger().warning("Failed to parse Minecraft version from Bukkit version. Defaulting to: " + minecraftServerVersion);
            getLogger().warning("If your Minecraft server is under this version, your API-managed resource pack will not be compatible with your version.");
        }

        loadConfig(); // Load or create the configuration file
        loadFiles(); // Load or create folders

        if (!"api".equalsIgnoreCase(pluginUsageMode) &&
                !"self-hosted".equalsIgnoreCase(pluginUsageMode) &&
                !"edit-only".equalsIgnoreCase(pluginUsageMode)) {
            getLogger().severe("Invalid pluginUsageMode in config.yml: '" + pluginUsageMode + "'");
            getLogger().severe("Plugin will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        OS os = getOperatingSystemEnum();
        if ("api".equalsIgnoreCase(pluginUsageMode) && localYtDlp) {
            new YtDlpSetup(this, os).setup();
        }
        if ("self-hosted".equalsIgnoreCase(pluginUsageMode) || "edit-only".equalsIgnoreCase(pluginUsageMode)) {
            new YtDlpSetup(this, os).setup();
            new FFmpegSetup(this, os).setup();
        }

        RemoteApiClient remoteApiClient = new RemoteApiClient(this, getApiBaseURL());
        SelfHostedManager selfHostedManager = new SelfHostedManager(this, os);

        Objects.requireNonNull(this.getCommand("customdisc")).setExecutor(new CommandURLCustomDiscs(this, os, remoteApiClient, selfHostedManager));

        // Detect the ProtocolLib plugin to enable custom Now Playing toasts
        boolean protocolLibEnabled = getServer().getPluginManager().isPluginEnabled("ProtocolLib");
        if (protocolLibEnabled) {
            getLogger().info("ProtocolLib detected! Custom Now Playing toasts will be available.");
            getServer().getPluginManager().registerEvents(new JukeboxListener(this, true), this);
        } else {
            getLogger().warning("ProtocolLib is not installed! The plugin will work normally, but custom Now Playing toasts will not be available.");
            getServer().getPluginManager().registerEvents(new JukeboxListener(this, false), this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("URLCustomDiscs disabled!");
    }

    public enum OS { WINDOWS, LINUX, OTHER }
    public OS getOperatingSystemEnum() {
        String name = System.getProperty("os.name").toLowerCase();
        if (name.contains("win")) return OS.WINDOWS;
        if (name.contains("nux") || name.contains("nix")) return OS.LINUX;
        return OS.OTHER;
    }

    private void loadConfig() {
        File configFile = new File(getDataFolder(), "config.yml");

        // Create the plugins/URLCustomDiscs folder
        getDataFolder().mkdirs();

        // Create the plugins/URLCustomDiscs/config.yml file
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                String configContent =
                        "# Configuration file for URLCustomDiscs plugin.\n" +
                                "\n" + "\n" +

                                "# ========== PLUGIN USAGE MODE ==========\n" +
                                "\n" +
                                "# If you want to use the preconfigured remote API dedicated to the plugin, fill in the pluginUsageMode field with 'api'.\n" +
                                "# If you want to use a personal installation and configuration, fill in the pluginUsageMode field with 'self-hosted'.\n" +
                                "# If you only want the URLCustomDiscsPack.zip resource pack to be updated locally in your server files and duplicated to a custom path with a custom name, fill in the pluginUsageMode field with 'edit-only'.\n" +
                                "# Note: Edit-Only mode can be useful if you already have an HTTP server serving another resource pack, and if you have a plugin that allows you to merge your current resource pack with the URLCustomDiscsPack.zip resource pack.\n" +
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
                                "# Download URL of the resource pack, automatically generated when creating your first custom disc, which uses the token to locate your resource pack.\n" +
                                "apiDownloadResourcePackURL: \"\"\n" +
                                "# Once your token and apiDownloadResourcePackURL have been generated, fill in the 'resource-pack=' field in your Minecraft server's 'server.properties' file, following the example below.\n" +
                                "# Example: resource-pack=YOUR_apiDownloadResourcePackURL\n" +
                                "# Then restart your Minecraft server.\n" +
                                "\n" +
                                "# Once you have filled in the 'resource-pack=' field and tested that it works, you can now force players to install the resource pack by setting the 'require-resource-pack=' field to true in your Minecraft server's 'server.properties' file, following the example below.\n" +
                                "# Example: require-resource-pack=true\n" +
                                "# Then restart your Minecraft server.\n" +
                                "\n" + "\n" +
                                "# localYtDlp: If set to true, uses the yt-dlp tool, automatically downloaded according to the operating system of your Minecraft server, to download audio directly from YouTube.\n" +
                                "# This allows you to continue using YouTube URLs even if the remote API is blocked by YouTube or other websites like soundcloud.\n" +
                                "# Not recommended for shared Minecraft hosting providers (such as Shockbyte), as they usually block yt-dlp execution or are already IP-banned.\n" +
                                "# You must restart your server after changing this option for it to take effect.\n" +
                                "localYtDlp: false\n" +
                                "\n" + "\n" +

                                "# ========== SELF-HOSTED MODE CONFIGURATION ==========\n" +
                                "\n" +
                                "# Resource pack access mode on the HTTP server that determines how the resource pack is retrieved and updated.\n" +
                                "# Fill in the resourcePackAccessMode field with 'local' or 'online' depending on your setup and preference.\n" +
                                "# - 'local': Access the resource pack on the HTTP server via an absolute path.\n" +
                                "# - 'online': Download and upload the resource pack on the HTTP server via HTTP requests.\n" +
                                "# Available options for the two possible configurations:\n" +
                                "# - If your Minecraft server and HTTP server are on the same machine, you can use either 'local' or 'online'.\n" +
                                "# - If your Minecraft server and HTTP server are on different machines, you must use 'online'.\n" +
                                "resourcePackAccessMode: \"local\"\n" +
                                "\n" + "\n" +
                                "# If you are using the local resource pack access mode, fill in the zipFileAbsolutePath field.\n" +
                                "\n" +
                                "# Absolute path to the URLCustomDiscsPack.zip resource pack, hosted on the HTTP server, for editing.\n" +
                                "# Example for Windows: C:/Apache24/htdocs/URLCustomDiscsPack.zip\n" +
                                "# Example for Linux: /var/www/html/URLCustomDiscsPack.zip\n" +
                                "zipFileAbsolutePath: \"C:/Apache24/htdocs/URLCustomDiscsPack.zip\"\n" +
                                "\n" + "\n" +
                                "# If you are using the online resource pack access mode, fill in both the downloadResourcePackURL and uploadResourcePackURL fields.\n" +
                                "\n" +
                                "# Download URL of the URLCustomDiscsPack.zip resource pack from the HTTP server, used to retrieve and edit the resource pack.\n" +
                                "# Example: http://11.111.11.1:80/URLCustomDiscsPack.zip\n" +
                                "downloadResourcePackURL: \"http://11.111.11.1:80/URLCustomDiscsPack.zip\"\n" +
                                "\n" +
                                "# Upload URL of the URLCustomDiscsPack.zip resource pack to the HTTP server, used to update the resource pack.\n" +
                                "# Example: http://11.111.11.1:80/upload.php\n" +
                                "uploadResourcePackURL: \"http://11.111.11.1:80/upload.php\"\n" +
                                "\n" + "\n" +

                                "# ========== EDIT-ONLY MODE CONFIGURATION ==========\n" +
                                "\n" +
                                "# Path (including the custom filename) to the duplicated URLCustomDiscsPack.zip resource pack, relative to the plugins folder, for merging or other use.\n" +
                                "# Example: OtherPlugin/merge_folder/PackToMerge.zip\n" +
                                "duplicatedZipFilePath: \"URLCustomDiscs/edit-only_mode_reference_resource_pack/duplicated_resource_pack/DuplicatedURLCustomDiscsPack.zip\"\n"
                        ;
                try (FileWriter writer = new FileWriter(configFile)) {
                    writer.write(configContent);
                }
            } catch (IOException e) {
                this.getLogger().severe("Exception: " + e.getMessage());
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        pluginUsageMode = config.getString("pluginUsageMode");
        apiBaseURL = config.getString("apiBaseURL");
        token = config.getString("token");
        apiDownloadResourcePackURL = config.getString("apiDownloadResourcePackURL");
        localYtDlp = config.getBoolean("localYtDlp");
        downloadResourcePackURL = config.getString("downloadResourcePackURL");
        resourcePackAccessMode = config.getString("resourcePackAccessMode");
        zipFileAbsolutePath = config.getString("zipFileAbsolutePath");
        uploadResourcePackURL = config.getString("uploadResourcePackURL");
        duplicatedZipFilePath = config.getString("duplicatedZipFilePath");

        if (pluginUsageMode == null) {
            getLogger().warning("config.yml ERROR : The plugin usage mode is not defined.");
        }
        if (downloadResourcePackURL == null) {
            getLogger().warning("config.yml ERROR : Download resource pack URL is not configured.");
        }
        if (!Objects.equals(resourcePackAccessMode, "local") && !Objects.equals(resourcePackAccessMode, "online")) {
            getLogger().warning("config.yml ERROR : Minecraft server type is not configured.");
        }
        if (Objects.equals(resourcePackAccessMode, "local")) {
            if (Objects.equals(zipFileAbsolutePath, "")) {
                getLogger().warning("config.yml ERROR : Absolute resource pack file path is not configured.");
            }
        }
        if (Objects.equals(resourcePackAccessMode, "online")) {
            if (Objects.equals(uploadResourcePackURL, "")) {
                getLogger().warning("config.yml ERROR : Download resource pack URL is not configured.");
            }
        }
        if ("edit-only".equalsIgnoreCase(pluginUsageMode)) {
            if (Objects.equals(duplicatedZipFilePath, "")) {
                getLogger().warning("config.yml ERROR : Duplicated resource pack file path is not configured.");
            } else if (duplicatedZipFilePath != null && !duplicatedZipFilePath.toLowerCase().endsWith(".zip")) {
                getLogger().warning("config.yml ERROR : Duplicated resource pack file path must point to a .zip file.");
            }
        }
    }

    public String getOperatingSystem() { return os; }

    private void loadFiles() {
        File discUuidFile = new File(getDataFolder(), "discs.json"); // Assign to class variable
        if (!discUuidFile.exists()) {
            try {
                discUuidFile.createNewFile();
                Files.writeString(discUuidFile.toPath(), "{}"); // Initialize an empty JSON
            } catch (IOException e) {
                this.getLogger().severe("Exception: " + e.getMessage());
            }
        }

        audioToSendFolder = new File(getDataFolder(), "audio_to_send");
        if (!audioToSendFolder.exists()) audioToSendFolder.mkdir();

        binFolder = new File(getDataFolder(), "bin");
        if (!binFolder.exists()) binFolder.mkdir();

        if ("self-hosted".equalsIgnoreCase(pluginUsageMode) || "edit-only".equalsIgnoreCase(pluginUsageMode)) {
            editResourcePackFolder = new File(getDataFolder(), "edit_resource_pack");
            if (!editResourcePackFolder.exists()) editResourcePackFolder.mkdirs();

            tempAudioFolder = new File(editResourcePackFolder, "temp_audio");
            if (!tempAudioFolder.exists()) tempAudioFolder.mkdirs();

            tempUnpackedFolder = new File(editResourcePackFolder, "temp_unpacked");
            if (!tempUnpackedFolder.exists()) tempUnpackedFolder.mkdirs();
        }

        if ("edit-only".equalsIgnoreCase(pluginUsageMode)) {
            editOnlyModeReferenceResourcePackFolder = new File(getDataFolder(), "edit-only_mode_reference_resource_pack");
            if (!editOnlyModeReferenceResourcePackFolder.exists()) editOnlyModeReferenceResourcePackFolder.mkdirs();

            editOnlyModeZipFilePath = new File(editOnlyModeReferenceResourcePackFolder, "URLCustomDiscsPack.zip").getAbsolutePath();

            File editOnlyModeDuplicatedResourcePackFolder = new File(editOnlyModeReferenceResourcePackFolder, "duplicated_resource_pack");
            if (!editOnlyModeDuplicatedResourcePackFolder.exists()) editOnlyModeDuplicatedResourcePackFolder.mkdirs();
        }
    }

    // Getters / Setters
    public String getMinecraftServerVersion() { return minecraftServerVersion; }
    public String getPluginUsageMode() { return pluginUsageMode; }
    public File getAudioToSendFolder() { return audioToSendFolder; }
    public File getBinFolder() { return binFolder; }
    public File getEditResourcePackFolder() { return editResourcePackFolder; }
    public File getTempAudioFolder() { return tempAudioFolder; }
    public File getTempUnpackedFolder() { return tempUnpackedFolder; }
    public File getAudioFolder() {
        if ("api".equalsIgnoreCase(pluginUsageMode) && getLocalYtDlp()) {
            return audioToSendFolder;
        } else if ("self-hosted".equalsIgnoreCase(pluginUsageMode) || "edit-only".equalsIgnoreCase(pluginUsageMode)) {
            return tempAudioFolder;
        } else {
            throw new IllegalStateException("Unknown pluginUsageMode: " + pluginUsageMode);
        }
    }
    public String getApiBaseURL() { return apiBaseURL; }
    public String getToken() { return this.token != null ? this.token : ""; }
    public void setToken(String newToken) { this.token = newToken; }
    public String getApiDownloadResourcePackURL() { return this.apiDownloadResourcePackURL != null ? this.apiDownloadResourcePackURL : ""; }
    public void setApiDownloadResourcePackURL(String newApiDownloadResourcePackURL) { this.apiDownloadResourcePackURL = newApiDownloadResourcePackURL; }
    public Boolean getLocalYtDlp() { return localYtDlp != null ? localYtDlp : false; }
    public String getDownloadResourcePackURL() { return downloadResourcePackURL; }
    public String getUploadResourcePackURL() { return uploadResourcePackURL; }
    public String getResourcePackAccessMode() { return resourcePackAccessMode; }
    public String getZipFileAbsolutePath() { return zipFileAbsolutePath; }

    // Edit-Only mode
    public File getEditOnlyModeReferenceResourcePackFolder() { return editOnlyModeReferenceResourcePackFolder; }
    public String getEditOnlyModeZipFilePath() { return editOnlyModeZipFilePath; }
    public String getDuplicatedZipFilePath() { return duplicatedZipFilePath; }
}