package com.urlcustomdiscs;
import com.mpatric.mp3agic.Mp3File;
import com.urlcustomdiscs.utils.DiscUtils;

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
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.io.*;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.*;

public class CommandURLCustomDiscs implements CommandExecutor {

    private final URLCustomDiscs plugin;
    private final URLCustomDiscs.OS os;
    private final RemoteApiClient remoteApiClient;
    private final SelfHostedManager selfHostedManager;
    private final File discUuidFile;
    private final String pluginUsageMode;

    public CommandURLCustomDiscs(URLCustomDiscs plugin, URLCustomDiscs.OS os, RemoteApiClient remoteApiClient, SelfHostedManager selfHostedManager) {
        this.plugin = plugin;
        this.os = os;
        this.remoteApiClient = remoteApiClient;
        this.selfHostedManager = selfHostedManager;
        this.discUuidFile = new File(plugin.getDataFolder(), "discs.json");
        this.pluginUsageMode = plugin.getPluginUsageMode();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
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
            player.sendMessage(ChatColor.YELLOW + "/customdisc updatedep");
            player.sendMessage("");
            player.sendMessage(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Other useful vanilla commands:");
            player.sendMessage(ChatColor.AQUA + "/playsound minecraft:customdisc." + ChatColor.DARK_AQUA + "<" + ChatColor.AQUA + "disc_name" + ChatColor.DARK_AQUA + "> " + ChatColor.AQUA + "ambient @a ~ ~ ~ 1 1");
            player.sendMessage("");
            player.sendMessage(ChatColor.AQUA + "/stopsound @a * minecraft:customdisc." + ChatColor.DARK_AQUA + "<" + ChatColor.AQUA + "disc_name" + ChatColor.DARK_AQUA + ">");
            player.sendMessage("");
            return true;
        }

        // Command to create a custom disc
        if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            String input = args[1];
            String rawDiscName = args[2].replaceAll("[^a-zA-Z0-9_-]", "_");
            String audioType = args[3].toLowerCase(); // "mono" or "stereo"

            player.sendMessage(ChatColor.GRAY + "Processing audio...");

            try { // For URL

                // Checks if input is a valid URL
                new URL(input);

                // Download the mp3 for api mode, if local yt-dlp is enabled
                // Download the mp3 for self-hosted and edit-only modes
                if (("api".equalsIgnoreCase(pluginUsageMode) && plugin.getLocalYtDlp())
                        || "self-hosted".equalsIgnoreCase(pluginUsageMode)
                        || "edit-only".equalsIgnoreCase(pluginUsageMode)) {

                    // Download the MP3 file from the URL in the audio_to_send folder (api) or edit_resource_pack/temp_audio folder (self-hosted and edit-only)
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        YtDlpManager ytDlpManager = new YtDlpManager(plugin, os);
                        File mp3File = new File(plugin.getAudioFolder(), rawDiscName + ".mp3");
                        boolean downloaded = ytDlpManager.downloadAudioWithYtDlp(input, mp3File);

                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (!downloaded || !mp3File.exists()) {
                                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Failed to download audio from the URL using yt-dlp.");
                                player.sendMessage(ChatColor.GRAY + "Attempting to update yt-dlp...");

                                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                    // Update yt-dlp
                                    new YtDlpSetup(plugin, os).setup();

                                    // Retry downloading the MP3 file from the URL using yt-dlp after updating yt-dlp
                                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                        boolean retried = ytDlpManager.downloadAudioWithYtDlp(input, mp3File);

                                        Bukkit.getScheduler().runTask(plugin, () -> {
                                            if (!retried || !mp3File.exists()) {
                                                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Download failed even after updating yt-dlp.");
                                                return;
                                            }
                                            player.sendMessage(ChatColor.GREEN + "Audio downloaded after updating yt-dlp.");

                                            // Pass the downloaded MP3 file name
                                            continueDiscCreation(player, mp3File.getName(), rawDiscName, audioType);
                                        });
                                    });
                                });
                            }
                            continueDiscCreation(player, mp3File.getName(), rawDiscName, audioType);
                        });
                    });

                } else {
                    // The URL is kept as is
                    continueDiscCreation(player, input, rawDiscName, audioType);
                }
                return true;

            } catch (MalformedURLException e) { // For MP3 file
                // If it is not a URL, check if it is an MP3 file in the audio_to_send folder
                File localMp3 = new File(plugin.getAudioToSendFolder(), input);
                if (localMp3.exists() && localMp3.isFile() && input.toLowerCase().endsWith(".mp3")) {

                    if ("api".equalsIgnoreCase(pluginUsageMode)) {
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
                    } else if ("self-hosted".equalsIgnoreCase(pluginUsageMode) || "edit-only".equalsIgnoreCase(pluginUsageMode)) {
                        File destFile = new File(plugin.getTempAudioFolder(), input);
                        try { // Move the MP3 file from the audio_to_send folder to the edit_resource_pack/temp_audio folder
                            Files.move(localMp3.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e1) {
                            plugin.getLogger().severe("Exception: " + e.getMessage());
                            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Failed to move the MP3 file to temp_audio.");
                            return true;
                        }
                    }

                    continueDiscCreation(player, input, rawDiscName, audioType);
                } else {
                    player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Invalid input: not a valid URL or .mp3 file in the audio_to_send folder.");
                    player.sendMessage(ChatColor.GOLD + "Usage: " + ChatColor.YELLOW + "/customdisc help");
                }
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
            JSONObject discData = DiscUtils.loadDiscData(discUuidFile);
            if (discData.isEmpty()) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "No custom disc found. Create a custom disc first (/customdisc help).");
                return true;
            }

            String discName = args[1].toLowerCase();

            DiscJsonManager discManager = new DiscJsonManager(plugin);
            JSONObject discInfo = null;
            try {
                discInfo = discManager.getDisc(discName);
            } catch (IOException e) {
                plugin.getLogger().severe("Exception: " + e.getMessage());
            }

            // Check if the custom disc exists
            if (discInfo == null || discInfo.isEmpty()) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Custom disc '" + discName + "' does not exist.");
                return true;
            }

            final JSONObject discInfoFinal = discInfo;

            if (pluginUsageMode.equalsIgnoreCase("api")) {
                String token = plugin.getToken();
                if (token == null || token.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "No token configured. Please register your server first by creating a custom disc.");
                    return true;
                }

                String minecraftServerVersion = plugin.getMinecraftServerVersion();

                remoteApiClient.deleteCustomDiscRemotely(player, discName, discInfoFinal, token, minecraftServerVersion);
                return true;
            } else if ("self-hosted".equalsIgnoreCase(pluginUsageMode) || "edit-only".equalsIgnoreCase(pluginUsageMode)) {
                selfHostedManager.deleteCustomDisc(player, discName, discInfoFinal);
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Invalid plugin usage mode: " + pluginUsageMode + ". Please set the plugin usage mode to 'api', 'self-hosted' or 'edit-only' in the config.yml file.");
                return true;
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
        if (args.length == 1 && args[0].equalsIgnoreCase("updatedep")) {
            if (("api".equalsIgnoreCase(pluginUsageMode) && !plugin.getLocalYtDlp())) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD +
                        "Error: local yt-dlp is disabled. " +
                        "This feature is required to download audio from URLs using the server's local yt-dlp installation instead of the remote API. " +
                        "To enable it, open the config.yml file, set 'localYtDlp: true', and restart the server.");
                return true;
            } else if (("api".equalsIgnoreCase(pluginUsageMode) && plugin.getLocalYtDlp())
                    || "self-hosted".equalsIgnoreCase(pluginUsageMode)
                    || "edit-only".equalsIgnoreCase(pluginUsageMode)) {

                player.sendMessage(ChatColor.GRAY + "Checking for yt-dlp updates...");

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    YtDlpSetup ytDlpSetup = new YtDlpSetup(plugin, os);
                    try {
                        ytDlpSetup.setup();
                        player.sendMessage(ChatColor.GREEN + "yt-dlp update check finished. See console for details.");
                    } catch (Exception e) {
                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Failed to update yt-dlp: " + e.getMessage());
                    }
                });
                return true;
            }
        }

        player.sendMessage(ChatColor.GOLD + "Usage: " + ChatColor.YELLOW + "/customdisc help");
        return true;
    }

    private void continueDiscCreation(Player player, String finalAudioIdentifier, String displayName, String audioType) {
        final String discName = displayName.toLowerCase();

        DiscJsonManager discManager = new DiscJsonManager(plugin);
        JSONObject discInfo = null;
        try {
            discInfo = discManager.getOrCreateDisc(discName, displayName);
        } catch (IOException e) {
            plugin.getLogger().severe("Exception: " + e.getMessage());
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error creating disc information.");
        }
        final JSONObject discInfoFinal = discInfo;

        String minecraftServerVersion = plugin.getMinecraftServerVersion();

        if (pluginUsageMode.equalsIgnoreCase("api")) {
            if (plugin.getToken().isEmpty()) {
                remoteApiClient.requestTokenFromRemoteServer(player, minecraftServerVersion, () ->
                        remoteApiClient.createCustomDiscRemotely(player, finalAudioIdentifier, discName, audioType, discInfoFinal, plugin.getToken(), minecraftServerVersion));
            } else {
                remoteApiClient.createCustomDiscRemotely(player, finalAudioIdentifier, discName, audioType, discInfoFinal, plugin.getToken(), minecraftServerVersion);
            }
        } else if ("self-hosted".equalsIgnoreCase(pluginUsageMode) || "edit-only".equalsIgnoreCase(pluginUsageMode)) {
            selfHostedManager.createCustomDisc(player, finalAudioIdentifier, discName, audioType, discInfoFinal);
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
                // Hide "C418 - 13" => cant (or rather lazy to create a new json just for this)
                disc.setItemMeta(meta);
            }

            //Add the disc to the player's inventory
            player.getInventory().addItem(disc);
            player.sendMessage(ChatColor.GRAY + "Custom disc " + ChatColor.GOLD + displayName + ChatColor.GRAY + " added to your inventory.");
        } catch (IOException e) {
            plugin.getLogger().severe("Exception: " + e.getMessage());
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error recovering the custom disc.");
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