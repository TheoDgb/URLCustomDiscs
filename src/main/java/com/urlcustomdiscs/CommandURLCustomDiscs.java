package com.urlcustomdiscs;
import com.mpatric.mp3agic.Mp3File;
import com.urlcustomdiscs.utils.DiscUtils;

import com.urlcustomdiscs.utils.YtDlpUtils;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.*;
import java.util.UUID;

public class CommandURLCustomDiscs implements CommandExecutor {

    private final URLCustomDiscs plugin;
    private final RemoteApiClient remoteApiClient;
    private final File discUuidFile;
    private final String os;
    private final String pluginUsageMode;
    private final String downloadResourcePackURL;
    private final String minecraftServerType;
    private final String zipFilePath;

    public CommandURLCustomDiscs(URLCustomDiscs plugin, RemoteApiClient remoteApiClient) {
        this.plugin = plugin;
        this.remoteApiClient = remoteApiClient;
        this.discUuidFile = new File(plugin.getDataFolder(), "discs.json");
        this.os = plugin.getOperatingSystem();
        this.pluginUsageMode = plugin.getPluginUsageMode();
        this.downloadResourcePackURL = plugin.getDownloadResourcePackURL();
        this.minecraftServerType = plugin.getMinecraftServerType();
        this.zipFilePath = plugin.getZipFilePath();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return false;
        }

        // Help command
        if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
            player.sendMessage("");
            player.sendMessage(ChatColor.YELLOW + "Usage of the command " + ChatColor.GOLD + "/customdisc" + ChatColor.YELLOW + ":");
            player.sendMessage("");
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Create a custom music disc from a YouTube URL or local MP3 file:");
            player.sendMessage(ChatColor.YELLOW + "/customdisc create " + ChatColor.GOLD + "<" + ChatColor.YELLOW + "URL" + ChatColor.GOLD + " OR " + ChatColor.YELLOW + "audio_name.mp3" + ChatColor.GOLD + "> <" + ChatColor.YELLOW + "disc_name" + ChatColor.GOLD + "> <" + ChatColor.YELLOW + "mono" + ChatColor.GOLD + " / " + ChatColor.YELLOW + "stereo" + ChatColor.GOLD + ">");
            player.sendMessage(ChatColor.GRAY + "- mono: enables spatial audio (as when played in a jukebox)");
            player.sendMessage(ChatColor.GRAY + "- stereo: plays the audio in the traditional way");
            player.sendMessage(ChatColor.GRAY + "Instructions for local MP3 files (admin-only):");
            player.sendMessage(ChatColor.GRAY + "- Place your MP3 file inside the audio_to_send folder in the plugin directory");
            player.sendMessage(ChatColor.GRAY + "- Rename the MP3 file to a simple name with no spaces and no special characters.");
            player.sendMessage(ChatColor.GRAY + "- Don't forget to include the .mp3 extension in the audio_name.mp3 field.");
            player.sendMessage("");
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Give yourself a custom music disc:");
            player.sendMessage(ChatColor.YELLOW + "/customdisc give " + ChatColor.GOLD + "<" + ChatColor.YELLOW + "disc_name" + ChatColor.GOLD + ">");
            player.sendMessage("");
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Show the list of custom music discs (clickable names):");
            player.sendMessage(ChatColor.YELLOW + "/customdisc list");
            player.sendMessage("");
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Delete a custom music disc:");
            player.sendMessage(ChatColor.YELLOW + "/customdisc delete " + ChatColor.GOLD + "<" + ChatColor.YELLOW + "disc_name" + ChatColor.GOLD + ">");
            player.sendMessage("");
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Show details of the custom music disc you're holding:");
            player.sendMessage(ChatColor.YELLOW + "/customdisc info");
            player.sendMessage("");
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Update the yt-dlp dependency:");
            player.sendMessage(ChatColor.YELLOW + "/customdisc update");
            player.sendMessage("");
            player.sendMessage(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Other useful vanilla commands:");
            player.sendMessage(ChatColor.AQUA + "/execute positioned ~ ~ ~ run playsound minecraft:customdisc." + ChatColor.DARK_AQUA + "<" + ChatColor.AQUA + "disc_name" + ChatColor.DARK_AQUA + "> " + ChatColor.AQUA + "ambient @a");
            player.sendMessage("");
            player.sendMessage(ChatColor.AQUA + "/stopsound @a * minecraft:customdisc." + ChatColor.DARK_AQUA + "<" + ChatColor.AQUA + "disc_name" + ChatColor.DARK_AQUA + ">");
            player.sendMessage("");
            return true;
        }

        // Command to create a custom disc
        if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            String input = args[1];
            String rawDiscName = args[2].replaceAll("[^a-zA-Z0-9_-]", "_");
            String audioType = args[3].toLowerCase(); // "mono" ou "stereo"

            String audio;

            try {
                // Checks if input is a valid URL
                new URL(input);

                // If local yt-dlp is enabled, the mp3 is downloaded
                if (plugin.getLocalYtDlp()) {
                    YtDlpUtils ytDlpUtils = new YtDlpUtils(plugin);
                    File mp3File = new File(plugin.getDataFolder(), "audio_to_send/" + rawDiscName + ".mp3");
                    boolean downloaded = ytDlpUtils.downloadAudioWithYtDlp(input, mp3File);

                    if (!downloaded || !mp3File.exists()) {
                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Failed to download audio from the URL using local yt-dlp.");
                        player.sendMessage(ChatColor.GRAY + "Attempting to update yt-dlp...");

                        new YtDlpSetup(plugin).setup();

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            boolean retried = ytDlpUtils.downloadAudioWithYtDlp(input, mp3File);
                            if (!retried || !mp3File.exists()) {
                                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Download failed even after updating yt-dlp.");
                                return;
                            }
                            player.sendMessage(ChatColor.GREEN + "Audio downloaded after updating yt-dlp.");
                        }, 250L); // 15 secondes (20 ticks = 1 sec)
                        return true;
                    }

                    audio = mp3File.getName(); // Pass the downloaded MP3 file name
                } else {
                    // The URL is kept as is
                    audio = input;
                }
            } catch (MalformedURLException e) {
                // If it is not a URL we check if it is an MP3 file in the audio_to_send folder
                File localMp3 = new File(plugin.getDataFolder(), "audio_to_send/" + input);
                if (localMp3.exists() && localMp3.isFile() && input.toLowerCase().endsWith(".mp3")) {
                    // Check audio file size
                    long maxSize = 12L * 1024L * 1024L; // 12 MB
                    if (localMp3.length() > maxSize) {
                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "The audio file exceeds the maximum allowed size of 12MB.");
                        return true;
                    }
                    // Check audio file duration with MP3agic
                    try {
                        Mp3File mp3file = new Mp3File(localMp3);
                        long durationSeconds = mp3file.getLengthInSeconds();
                        if (durationSeconds > 300) { // 5 minutes
                            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "The audio file exceeds the maximum allowed length of 5 minutes.");
                            return true;
                        }
                    } catch (Exception ex) {
                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Unable to read the duration of the audio file.");
                        return true;
                    }
                    audio = input;
                } else {
                    player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Invalid input: not a valid URL or .mp3 file in the audio_to_send folder.");
                    player.sendMessage(ChatColor.GOLD + "Usage: " + ChatColor.YELLOW + "/customdisc help");
                    return true;
                }
            }

            final String finalAudioIdentifier = audio;
            final String displayName = rawDiscName;
            final String discName = displayName.toLowerCase();

            if (pluginUsageMode.equalsIgnoreCase("api")) {
                DiscJsonManager discManager = new DiscJsonManager(plugin);
                JSONObject discInfo = null;
                try {
                    discInfo = discManager.getOrCreateDisc(discName, displayName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                final JSONObject discInfoFinal = discInfo;

                String minecraftServerVersion = plugin.getMinecraftServerVersion();

                if (plugin.getToken().isEmpty()) {
                    remoteApiClient.requestTokenFromRemoteServer(player, minecraftServerVersion, () -> {
                        remoteApiClient.createCustomDiscRemotely(player, finalAudioIdentifier, discName, audioType, discInfoFinal, plugin.getToken(), minecraftServerVersion);
                    });
                } else {
                    remoteApiClient.createCustomDiscRemotely(player, finalAudioIdentifier, discName, audioType, discInfoFinal, plugin.getToken(), minecraftServerVersion);
                }
                return true;
            } else if (pluginUsageMode.equalsIgnoreCase("self-hosted")) {
                // Check if the audio is an mp3 file path (not a URL)
                if (audio.toLowerCase().endsWith(".mp3")) {
                    player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Self-hosted mode doesn't support creating disc from local MP3 file.");
                    return true;
                }

                // Create mp3 and ogg files
                File musicFolder = new File(plugin.getDataFolder(), "music");
                if (!musicFolder.exists()) musicFolder.mkdirs();

                String mp3FileName = discName + ".mp3";
                File mp3File = new File(musicFolder, mp3FileName);

                String oggFileName = discName + ".ogg";
                File oggFile = new File(musicFolder, oggFileName);

                // Delete files if they already exist
                if (mp3File.exists()) mp3File.delete();
                if (oggFile.exists()) oggFile.delete();

                new Thread(() -> {
                    try {
                        // Selecting executables based on OS
                        String ytDlpExecutable = "";
                        String ffmpegExecutable = "";
                        if (os.contains("win")) { // Windows
                            ytDlpExecutable = "./plugins/URLCustomDiscs/yt-dlp.exe";
                            ffmpegExecutable = "./plugins/URLCustomDiscs/FFmpeg/bin/ffmpeg.exe";
                        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) { // Linux (and macOS CURRENTLY NOT SUPPORTED)
                            ytDlpExecutable = "./plugins/URLCustomDiscs/yt-dlp";
                            ffmpegExecutable = "./plugins/URLCustomDiscs/FFmpeg/bin/ffmpeg";
                        }

                        // Check for the existence of yt-dlp and FFmpeg
                        File ytDlpFile = new File(ytDlpExecutable);
                        File ffmpegFile = new File(ffmpegExecutable);
                        if (!ytDlpFile.exists()) {
                            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error: yt-dlp was not found. Make sure you have installed it in your_mc_server_folder/plugins/URLCustomDiscs/");
                            return;
                        }
                        if (!ffmpegFile.exists()) {
                            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error: FFmpeg was not found. Make sure you have installed it in your_mc_server_folder/plugins/URLCustomDiscs/");
                            return;
                        }

                        // Download audio
                        player.sendMessage(ChatColor.GRAY + "Downloading URL music to MP3...");
                        plugin.getLogger().info("Downloading URL music to MP3...");
                        ProcessBuilder ytDlp = new ProcessBuilder(ytDlpExecutable, "-f", "bestaudio[ext=m4a]/best",
                                "--audio-format", "mp3", "-o", mp3File.getAbsolutePath(), finalAudioIdentifier);
                        Process ytDlpProcess = ytDlp.start();

                        // Read yt-dlp output for debugging
                        Thread ytDlpOutputThread = new Thread(() -> {
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(ytDlpProcess.getInputStream()))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    plugin.getLogger().info("yt-dlp: " + line); // Affiche la sortie pour débogage
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                        ytDlpOutputThread.start();

                        int ytDlpExitCode = ytDlpProcess.waitFor(); // Wait for the download to complete
                        ytDlpOutputThread.join(); // Ensure output reading is complete

                        if (ytDlpExitCode == 0) {
                            // Convert mp3 file to ogg mono or stereo
                            player.sendMessage(ChatColor.GRAY + "Converting MP3 to Ogg...");
                            plugin.getLogger().info("Converting MP3 to Ogg...");
                            ProcessBuilder ffmpeg;
                            if (audioType.equals("mono")) {
                                ffmpeg = new ProcessBuilder(ffmpegExecutable, "-i", mp3File.getAbsolutePath(), "-ac", "1", "-c:a", "libvorbis", oggFile.getAbsolutePath());
                            } else if (audioType.equals("stereo")) {
                                ffmpeg = new ProcessBuilder(ffmpegExecutable, "-i", mp3File.getAbsolutePath(), "-c:a", "libvorbis", oggFile.getAbsolutePath());
                            } else {
                                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Invalid audio type, use 'mono' or 'stereo'.");
                                return;
                            }
                            Process ffmpegProcess = ffmpeg.start();

                            // Read ffmpeg output for debugging
                            new Thread(() -> {
                                try (BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()))) {
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        plugin.getLogger().info("FFmpeg stdout: " + line); // Affiche la sortie pour débogage
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }).start();

                            // Read ffmpeg errors for debugging
                            new Thread(() -> {
                                try (BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpegProcess.getErrorStream()))) {
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        plugin.getLogger().info("ffmpeg stderr: " + line); // Affiche les erreurs pour débogage
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }).start();

                            int ffmpegExitCode = ffmpegProcess.waitFor(); // Wait for the conversion to complete

                            if (ffmpegExitCode == 0) {
                                // Delete the .mp3 file after conversion
                                if (mp3File.exists()) mp3File.delete();
                                player.sendMessage(ChatColor.GRAY + "Music downloaded and converted.");
                                plugin.getLogger().info("Music downloaded and converted.");

                                // Add .ogg to server resource pack zip
                                if (minecraftServerType.equals("local")) {
                                    addFileToZip(zipFilePath, oggFile, "assets/minecraft/sounds/custom/" + oggFile.getName());
                                    // Delete the .ogg file after update
                                    if (oggFile.delete()) {
                                        plugin.getLogger().info("Deleted Ogg file from music folder.");

                                        // Update sounds.json
                                        updateSoundsJson(discName);
                                        // Create and give the personalized disc to the player
                                        createCustomMusicDisc(player, discName, displayName);
                                    } else {
                                        plugin.getLogger().severe("Error deleting Ogg file from music folder.");
                                    }
                                } else if (minecraftServerType.equals("online")) {
                                    ResourcePackManager rpm = plugin.getResourcePackManager(); // Récupérer l'instance existante

                                    // Download the server resource pack
                                    if (rpm.downloadResourcePack()) {
                                        player.sendMessage(ChatColor.GRAY + "Server resource pack downloaded.");

                                        // Add music.ogg
                                        if (rpm.addFileToResourcePack(oggFile, "assets/minecraft/sounds/custom/" + oggFile.getName())) {
                                            player.sendMessage(ChatColor.GRAY + "Ogg file added to the server resource pack.");

                                            // Delete the .ogg file from the music folder after update
                                            if (oggFile.delete()) {
                                                plugin.getLogger().info("Ogg file deleted from music folder.");

                                                // Create custom disc (updateSoundsJson / updateDiscModelJson / createCustomMusicDiscJson)
                                                if (rpm.createCustomMusicDisc(player, discName, displayName)) {
                                                    player.sendMessage(ChatColor.GRAY + "Custom disc " + ChatColor.GOLD + displayName + ChatColor.GRAY + " created.");

                                                    // Upload the server resource pack
                                                    if (rpm.uploadResourcePack()) {
                                                        player.sendMessage(ChatColor.GRAY + "Server resource pack uploaded.");
                                                    } else {
                                                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error uploading the server resource pack.");
                                                    }
                                                } else {
                                                    player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error creating the custom disc.");
                                                }
                                            } else {
                                                plugin.getLogger().severe("Error deleting Ogg file from music folder.");
                                            }
                                        } else {
                                            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error adding the music to the server resource pack.");
                                        }
                                    } else {
                                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error downloading the server resource pack.");
                                    }
                                }

                                // Browse all online players and send them the resource pack
                                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                                    onlinePlayer.setResourcePack(downloadResourcePackURL);
                                }
                            } else {
                                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error converting to Ogg with FFmpeg.");
                                plugin.getLogger().severe("FFmpeg exited with code: " + ffmpegExitCode);
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error downloading the MP3 file with yt-dlp.");
                            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "An error may have occurred, please try again.");
                            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "If the error persists, your yt-dlp may be outdated, and a new version may be available: https://github.com/yt-dlp/yt-dlp?tab=readme-ov-file#installation");
                            plugin.getLogger().severe("yt-dlp exited with code: " + ytDlpExitCode);
                        }
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error downloading or converting music.");
                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Make sure yt-dlp has the correct execute permissions.");
                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "If your Minecraft server is hosted online, make sure your hosting provider allows the execution of binary files.");
                    }
                }).start();
                return true;
            }
        }

        // Command to give yourself a custom disc
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            String discName = args[1].toLowerCase().replaceAll(" ", "_");
            giveCustomMusicDisc(player, discName);
            return true;
        }

        // Command to show the list of custom discs
        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            JSONObject discData = DiscUtils.loadDiscData(discUuidFile);

            if (discData.isEmpty()) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "No custom music disc found. Create a disc first (/customdisc help).");
                return true;
            }
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "List of custom music discs:");

            // Sort disc names alphabetically
            List<String> discNames = new ArrayList<>(discData.keySet());
            Collections.sort(discNames);
            for (String discName : discNames) {
                // Retrieve the object corresponding to the disc
                JSONObject discInfo = discData.getJSONObject(discName);
                // Retrieve the displayName of each disc
                String displayName = discInfo.getString("displayName");
                // Create the TextComponent using the displayName
                TextComponent discText = createDiscTextComponent(displayName);
                player.spigot().sendMessage(discText);
            }
            return true;
        }

        // Command to delete a custom disc
        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            String discName = args[1].toLowerCase().replaceAll(" ", "_");
            if (pluginUsageMode.equalsIgnoreCase("api")) {
                DiscJsonManager discManager = new DiscJsonManager(plugin);
                JSONObject discInfo = null;
                try {
                    discInfo = discManager.getDisc(discName);
                } catch (IOException e) {
                    e.printStackTrace();
                } final JSONObject discInfoFinal = discInfo;

                String token = plugin.getToken();
                if (token == null || token.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "No token configured. Please register your server first by creating a custom disc.");
                    return true;
                }

                String minecraftServerVersion = plugin.getMinecraftServerVersion();

                remoteApiClient.deleteCustomDiscRemotely(player, discName, discInfoFinal, token, minecraftServerVersion);
                return true;
            } else if (pluginUsageMode.equalsIgnoreCase("self-hosted")) {
                if (minecraftServerType.equals("local")) {
                    deleteCustomMusicDisc(player, discName);
                    return true;
                } else if (minecraftServerType.equals("online")) {
                    ResourcePackManager rpm = plugin.getResourcePackManager(); // Récupérer l'instance existante

                    // Download the server resource pack
                    if (rpm.downloadResourcePack()) {
                        player.sendMessage(ChatColor.GRAY + "Server resource pack downloaded.");

                        // Delete a custom music disc
                        if (rpm.deleteCustomDiscFromResourcePack(player, discName)) {
                            return true;
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error downloading the server resource pack.");
                    }
                }
            }
        }

        // Command to get information about the disc in hand
        if (args.length == 1 && args[0].equalsIgnoreCase("info")) {
            ItemStack itemInHand = player.getInventory().getItemInMainHand();

            if (itemInHand.hasItemMeta()) {
                ItemMeta meta = itemInHand.getItemMeta();
                if (meta != null && meta.hasCustomModelData()) {
                    int customModelData = meta.getCustomModelData();

                    // Load disc data from JSON file
                    JSONObject discData = DiscUtils.loadDiscData(discUuidFile);
                    // Find the disc name from the CustomModelData
                    String discName = DiscUtils.getDiscNameFromCustomModelData(discData, customModelData);

                    if (discName != null) {
                        JSONObject discInfo = discData.getJSONObject(discName);
                        String displayName = discInfo.getString("displayName");
                        String discUUID = discInfo.getString("uuid");
                        String soundKey = "customdisc." + discName.toLowerCase().replaceAll(" ", "_");

                        // Send information to the player
                        player.sendMessage(ChatColor.GRAY + "Disc played: " + ChatColor.GOLD + discName);
                        player.sendMessage(ChatColor.GRAY + "Display name: " + ChatColor.GOLD + displayName);
                        player.sendMessage(ChatColor.GRAY + "UUID: " + ChatColor.GOLD + discUUID);
                        player.sendMessage(ChatColor.GRAY + "CustomModelData: " + ChatColor.GOLD + customModelData);
                        player.sendMessage(ChatColor.GRAY + "SoundKey: " + ChatColor.GOLD + soundKey);

                    } else {
                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "No custom music disc found with this CustomModelData.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You must be holding a custom music disc.");
                }
            } else {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You must be holding a custom music disc.");
            }
            return true;
        }

        // Command to update the yt-dlp dependency
        if (args.length == 1 && args[0].equalsIgnoreCase("update")) {
            if (!plugin.getLocalYtDlp()) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD +
                        "Error: local yt-dlp is disabled. " +
                        "This feature is required to download audio from URLs using the server's local yt-dlp installation instead of the remote API. " +
                        "To enable it, open the config.yml file, set 'localYtDlp: true', and restart the server.");
                return true;
            }

            player.sendMessage(ChatColor.GRAY + "Checking for yt-dlp updates...");

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                YtDlpSetup ytDlpSetup = new YtDlpSetup(plugin);
                try {
                    ytDlpSetup.setup();
                    player.sendMessage(ChatColor.GREEN + "yt-dlp update check finished. See console for details.");
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Failed to update yt-dlp: " + e.getMessage());
                }
            });
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "Usage: " + ChatColor.YELLOW + "/customdisc help");
        return true;
    }







    // Have fun with everything that's left below, I won't touch it anymore XD (FOR SELF-HOSTED CONFIGURATION UNDER VERSION 1.21.4 EXCLUDED)
    // Hi btw!

    private void updateSoundsJson(String discName) {
        String jsonPathInZip = "assets/minecraft/sounds.json";
        try {
            // Create a temporary file to extract the JSON
            File tempJson = File.createTempFile("sounds", ".json");

            // Extract sounds.json from the ZIP
            extractFileFromZip(zipFilePath, jsonPathInZip, tempJson);

            // Read or create JSON
            JSONObject soundsJson = tempJson.exists()
                    ? new JSONObject(Files.readString(tempJson.toPath()))
                    : new JSONObject();

            // Add the new sound
            String soundKey = "customdisc." + discName;

            if (!soundsJson.has(soundKey)) {
                JSONObject soundData = new JSONObject();
                soundData.put("sounds", new JSONArray().put(new JSONObject()
                        .put("name", "custom/" + discName)
                        .put("stream", true)));

                soundsJson.put(soundKey, soundData);

                // Save changes to the temporary file
                Files.writeString(tempJson.toPath(), soundsJson.toString(4));

                // Put the modified file back into the ZIP
                addFileToZip(zipFilePath, tempJson, jsonPathInZip);
            }

            // Delete the temporary file
            tempJson.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createCustomMusicDisc(Player player, String discName, String displayName) {
        try {
            // Load the JSON file from the discs
            JSONObject discData;
            if (discUuidFile.exists()) {
                String content = Files.readString(discUuidFile.toPath());
                discData = new JSONObject(content);
            } else {
                discData = new JSONObject();
            }

            // Check if disc already exists in JSON file
            JSONObject discInfo;
            if (discData.has(discName)) { // Retrieve existing information
                discInfo = discData.getJSONObject(discName);
            } else { // Generate a new UUID for the disc
                String newUUID = UUID.randomUUID().toString();
                discInfo = new JSONObject();
                discInfo.put("uuid", newUUID);

                // Calculate customModelData from UUID
                // Remove dashes from the UUID, Takes the first 8 hexadecimal characters, Converts that part of the UUID to a long number, Keeps a positive number within the limit of int
                int customModelData = (int) (Long.parseLong(newUUID.replace("-", "").substring(0, 8), 16) & 0x7FFFFFFF);
                discInfo.put("customModelData", customModelData);

                discInfo.put("displayName", displayName);

                // Save the new information to the JSON file
                discData.put(discName, discInfo);
                Files.writeString(discUuidFile.toPath(), discData.toString(4));
            }

            // Create the disc
            ItemStack disc = new ItemStack(Material.MUSIC_DISC_13);
            ItemMeta meta = disc.getItemMeta();

            if (meta != null) {
                // Set disc name
                meta.setDisplayName(ChatColor.GOLD + displayName);

                // Add a custom description
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Custom music disc: " + displayName); // Description avec le nom du disque
                meta.setLore(lore);

                // Use the calculated customModelData
                int customModelData = discInfo.getInt("customModelData");
                meta.setCustomModelData(customModelData);

                // Hide "C418 - 13"
                // cant (or rather lazy to create a new json just for that)

                // Apply changes to the ItemStack object
                disc.setItemMeta(meta);

                updateDiscModelJson(discName, customModelData);
                createCustomMusicDiscJson(discName);
            }

            // Add the disc to the player's inventory
            player.getInventory().addItem(disc);
            player.sendMessage(ChatColor.GRAY + "Custom disc " + ChatColor.GOLD + displayName + ChatColor.GRAY + " created.");
        } catch (IOException e) {
            e.printStackTrace();
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error creating disc.");
        }
    }

    private void updateDiscModelJson(String discName, int customModelData) {
        String modelPathInZip = "assets/minecraft/models/item/music_disc_13.json";
        try {
            // Temporary file to extract and modify the JSON
            File tempJson = File.createTempFile("music_disc_13", ".json");

            // Extract music_disc_13.json from the ZIP
            extractFileFromZip(zipFilePath, modelPathInZip, tempJson);

            // Read or create JSON
            JSONObject modelJson = tempJson.exists()
                    ? new JSONObject(Files.readString(tempJson.toPath()))
                    : new JSONObject();

            if (!modelJson.has("overrides")) {
                modelJson.put("overrides", new JSONArray());
            }

            JSONArray overrides = modelJson.getJSONArray("overrides");
            JSONObject newOverride = new JSONObject();
            newOverride.put("predicate", new JSONObject().put("custom_model_data", customModelData));
            newOverride.put("model", "item/custom_music_disc_" + discName);

            // Check if override already exists
            boolean exists = false;
            for (int i = 0; i < overrides.length(); i++) {
                if (overrides.getJSONObject(i).getJSONObject("predicate").getInt("custom_model_data") == customModelData) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                overrides.put(newOverride);
                Files.writeString(tempJson.toPath(), modelJson.toString(4));

                // Add the modified file to the ZIP
                addFileToZip(zipFilePath, tempJson, modelPathInZip);
            }

            tempJson.delete(); // Cleaning

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createCustomMusicDiscJson(String discName) {
        String modelPathInZip = "assets/minecraft/models/item/custom_music_disc_" + discName + ".json";
        try {
            // Create a temporary JSON file
            File tempJson = File.createTempFile("custom_music_disc_" + discName, ".json");

            // Create JSON for disc
            JSONObject discJson = new JSONObject();
            discJson.put("parent", "minecraft:item/generated");
            discJson.put("textures", new JSONObject().put("layer0", "minecraft:item/record_custom"));

            // Write the temporary JSON file
            Files.writeString(tempJson.toPath(), discJson.toString(4));
            // Add the JSON to the ZIP
            addFileToZip(zipFilePath, tempJson, modelPathInZip);

            tempJson.delete(); // Cleaning

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void addFileToZip(String zipFilePath, File fileToAdd, String entryPath) {
        File tempZipFile = new File(zipFilePath + ".tmp");
        File zipFile = new File(zipFilePath);
        try (
                FileInputStream fis = new FileInputStream(fileToAdd);
                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempZipFile));
                ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))
        ) {
            ZipEntry entry;
            byte[] buffer = new byte[1024];
            int length;

            // Copy all existing files from the ZIP
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.getName().equals(entryPath)) { // Do not duplicate if already exists
                    zos.putNextEntry(new ZipEntry(entry.getName()));
                    while ((length = zis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                }
                zis.closeEntry();
            }

            // Add the new file
            zos.putNextEntry(new ZipEntry(entryPath));
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
            zos.closeEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Replace the old server resource pack by the new one
        if (zipFile.delete() && tempZipFile.renameTo(zipFile)) {
            plugin.getLogger().info("Server resource pack updated.");
        } else {
            plugin.getLogger().severe("Error replacing the server resource pack.");
        }
    }

    private void extractFileFromZip(String zipFilePath, String fileInZip, File outputFile) {
        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            ZipEntry entry = zipFile.getEntry(fileInZip);
            if (entry == null) {
                plugin.getLogger().info("The file " + fileInZip + " doesn't exist in the ZIP.");
                return;
            }

            // Read input from ZIP and write to outputFile
            try (InputStream inputStream = zipFile.getInputStream(entry);
                 FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void giveCustomMusicDisc(Player player, String discName) {
        try {
            if (!discUuidFile.exists()) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "No custom music disc found. Create a disc first (/customdisc help).");
                return;
            }

            // Read JSON file from discs
            String content = Files.readString(discUuidFile.toPath());
            JSONObject discData = new JSONObject(content);

            // Check if the disk exists
            if (!discData.has(discName)) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "The disc '" + discName + "' doesn't exist.");
                return;
            }

            JSONObject discInfo = discData.getJSONObject(discName);
            int customModelData = discInfo.getInt("customModelData");
            String displayName = discInfo.getString("displayName");

            // Create the disc with the same properties as when it was created
            ItemStack disc = new ItemStack(Material.MUSIC_DISC_13);
            ItemMeta meta = disc.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + displayName);
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Custom music disc: " + displayName);
                meta.setLore(lore);
                meta.setCustomModelData(customModelData);
                // Hide "C418 - 13" => cant (or rather lazy to create a new json just for that)
                disc.setItemMeta(meta);
            }

            //Add the disc to the player's inventory
            player.getInventory().addItem(disc);
            player.sendMessage(ChatColor.GRAY + "Custom disc " + ChatColor.GOLD + displayName + ChatColor.GRAY + " added to your inventory.");
        } catch (IOException e) {
            e.printStackTrace();
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error recovering the custom disc.");
        }
    }

    private void deleteCustomMusicDisc(Player player, String discName) {
        String soundKey = "customdisc." + discName;
        String customMusicDiscJsonPath = "assets/minecraft/models/item/custom_music_disc_" + discName + ".json";
        String oggFilePath = "assets/minecraft/sounds/custom/" + discName + ".ogg";
        try {
            // Read and modify the discs JSON file
            if (!discUuidFile.exists()) {
                player.sendMessage(ChatColor.RED + "No custom music disc found.");
                return;
            }

            String content = Files.readString(discUuidFile.toPath());
            JSONObject discData = new JSONObject(content);

            if (!discData.has(discName)) {
                player.sendMessage(ChatColor.RED + "The disc '" + discName + "' doesn't exist.");
                return;
            }

            // Retrieve the disk displayName
            JSONObject discInfo = discData.getJSONObject(discName);
            String displayName = discInfo.getString("displayName");

            // Delete the entry from disc
            discData.remove(discName);
            Files.writeString(discUuidFile.toPath(), discData.toString(4));

            // Remove the sound entry from sounds.json
            File tempSoundsJson = File.createTempFile("sounds", ".json");
            extractFileFromZip(zipFilePath, "assets/minecraft/sounds.json", tempSoundsJson);

            JSONObject soundsJson = new JSONObject(Files.readString(tempSoundsJson.toPath()));
            soundsJson.remove(soundKey);
            Files.writeString(tempSoundsJson.toPath(), soundsJson.toString(4));
            addFileToZip(zipFilePath, tempSoundsJson, "assets/minecraft/sounds.json");
            tempSoundsJson.delete();

            // Remove override in music_disc_13.json
            File tempModelJson = File.createTempFile("music_disc_13", ".json");
            extractFileFromZip(zipFilePath, "assets/minecraft/models/item/music_disc_13.json", tempModelJson);

            JSONObject modelJson = new JSONObject(Files.readString(tempModelJson.toPath()));
            JSONArray overrides = modelJson.getJSONArray("overrides");

            for (int i = 0; i < overrides.length(); i++) {
                if (overrides.getJSONObject(i).getString("model").equals("item/custom_music_disc_" + discName)) {
                    overrides.remove(i);
                    break;
                }
            }

            Files.writeString(tempModelJson.toPath(), modelJson.toString(4));
            addFileToZip(zipFilePath, tempModelJson, "assets/minecraft/models/item/music_disc_13.json");
            tempModelJson.delete();

            // Delete the JSON file from disc
            deleteFileFromZip(zipFilePath, customMusicDiscJsonPath);

            // Delete the associated .ogg file
            deleteFileFromZip(zipFilePath, oggFilePath);

            player.sendMessage(ChatColor.GRAY + "Custom disc " + ChatColor.GOLD + displayName + ChatColor.GRAY + " deleted.");
        } catch (IOException e) {
            e.printStackTrace();
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error deleting the custom disc.");
        }
    }

    private void deleteFileFromZip(String zipFilePath, String filePath) {
        File tempZipFile = new File(zipFilePath + ".tmp");
        File zipFile = new File(zipFilePath);

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
             ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempZipFile))) {

            ZipEntry entry;
            byte[] buffer = new byte[1024];
            int length;

            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.getName().equals(filePath)) {
                    zos.putNextEntry(new ZipEntry(entry.getName()));
                    while ((length = zis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (zipFile.delete() && tempZipFile.renameTo(zipFile)) {
            plugin.getLogger().info("Deleted " + filePath + " from ZIP.");
        } else {
            plugin.getLogger().severe("Error deleting " + filePath + " from ZIP.");
        }
    }

    private TextComponent createDiscTextComponent(String displayName) {
        TextComponent discText = new TextComponent(displayName);
        discText.setColor(net.md_5.bungee.api.ChatColor.GOLD);
        discText.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/customdisc give " + displayName));
        discText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.YELLOW + "Click to get this disc!")));
        return discText;
    }
}
