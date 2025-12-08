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

public class CommandURLCustomDiscs implements CommandExecutor {

    private final URLCustomDiscs plugin;
    private final URLCustomDiscs.OS os;
    private final RemoteApiClient remoteApiClient;
    private final SelfHostedManager selfHostedManager;
    private final File discUuidFile;
    private final String pluginUsageMode;
    private final PermissionManager permissionManager;

    public CommandURLCustomDiscs(URLCustomDiscs plugin, URLCustomDiscs.OS os, RemoteApiClient remoteApiClient, SelfHostedManager selfHostedManager) {
        this.plugin = plugin;
        this.os = os;
        this.remoteApiClient = remoteApiClient;
        this.selfHostedManager = selfHostedManager;
        this.discUuidFile = new File(plugin.getDataFolder(), "discs.json");
        this.pluginUsageMode = plugin.getPluginUsageMode();
        this.permissionManager = new PermissionManager(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return false;
        }

        // Admin commands (OP-only)
        if (args.length >= 2 && args[0].equalsIgnoreCase("admin")) {
            if (!permissionManager.isOp(player)) {
                player.sendMessage(ChatColor.RED + "You can't do that.");
                return true;
            }

            if (args[1].equalsIgnoreCase("on")) {
                permissionManager.setCreationEnabled(true);
                player.sendMessage(ChatColor.GREEN + "Disc creation enabled.");
                return true;
            }

            if (args[1].equalsIgnoreCase("off")) {
                permissionManager.setCreationEnabled(false);
                player.sendMessage(ChatColor.GREEN + "Disc creation disabled.");
                return true;
            }

            if (args.length == 3 && args[1].equalsIgnoreCase("limit")) {
                if (args[2].equalsIgnoreCase("unlimited")) {
                    permissionManager.setMaxDiscsPerUser(-1);
                    player.sendMessage(ChatColor.GREEN + "Disc limit removed.");
                } else {
                    try {
                        int limit = Integer.parseInt(args[2]);
                        permissionManager.setMaxDiscsPerUser(limit);
                        player.sendMessage(ChatColor.GREEN + "Disc limit set to " + limit + ".");
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Invalid number.");
                    }
                }
                return true;
            }

            if (args.length == 3 && args[1].equalsIgnoreCase("reset")) {
                String discName = args[2].toLowerCase();
                try {
                    DiscJsonManager discManager = new DiscJsonManager(plugin);
                    JSONObject discInfo = discManager.getDisc(discName);
                    
                    if (discInfo == null || discInfo.isEmpty()) {
                        player.sendMessage(ChatColor.RED + "Disc not found.");
                        return true;
                    }

                    permissionManager.resetDiscOwnership(discInfo);
                    discManager.saveDisc(discName, discInfo);
                    player.sendMessage(ChatColor.GREEN + "Ownership reset for disc: " + discName);
                } catch (IOException e) {
                    player.sendMessage(ChatColor.RED + "Error resetting disc.");
                }
                return true;
            }

            if (args.length == 4 && args[1].equalsIgnoreCase("transfer")) {
                String discName = args[2].toLowerCase();
                String newOwner = args[3];
                
                try {
                    DiscJsonManager discManager = new DiscJsonManager(plugin);
                    JSONObject discInfo = discManager.getDisc(discName);
                    
                    if (discInfo == null || discInfo.isEmpty()) {
                        player.sendMessage(ChatColor.RED + "Disc not found.");
                        return true;
                    }

                    permissionManager.transferOwnership(discInfo, newOwner);
                    discManager.saveDisc(discName, discInfo);
                    player.sendMessage(ChatColor.GREEN + "Disc transferred to " + newOwner + ".");
                } catch (IOException e) {
                    player.sendMessage(ChatColor.RED + "Error transferring disc.");
                }
                return true;
            }

            player.sendMessage(ChatColor.RED + "Unknown admin command.");
            return true;
        }

        // Share command
        if (args.length == 4 && args[0].equalsIgnoreCase("share")) {
            String discName = args[1].toLowerCase();
            String targetPlayer = args[2];
            String permString = args[3].toLowerCase();

            try {
                DiscJsonManager discManager = new DiscJsonManager(plugin);
                JSONObject discInfo = discManager.getDisc(discName);
                
                if (discInfo == null || discInfo.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Disc not found.");
                    return true;
                }

                if (!permissionManager.canManage(player, discInfo)) {
                    player.sendMessage(ChatColor.RED + "You can't do that.");
                    return true;
                }

                String[] perms;
                if (permString.equals("all")) {
                    perms = new String[]{"use", "give", "delete"};
                } else if (permString.equals("use") || permString.equals("give") || permString.equals("delete")) {
                    perms = new String[]{permString};
                } else {
                    player.sendMessage(ChatColor.RED + "Invalid permission type.");
                    return true;
                }

                permissionManager.shareDisc(discInfo, targetPlayer, perms);
                discManager.saveDisc(discName, discInfo);
                player.sendMessage(ChatColor.GREEN + "Shared disc with " + targetPlayer + ".");
            } catch (IOException e) {
                player.sendMessage(ChatColor.RED + "Error sharing disc.");
            }
            return true;
        }

        // Unshare command
        if (args.length == 3 && args[0].equalsIgnoreCase("unshare")) {
            String discName = args[1].toLowerCase();
            String targetPlayer = args[2];

            try {
                DiscJsonManager discManager = new DiscJsonManager(plugin);
                JSONObject discInfo = discManager.getDisc(discName);
                
                if (discInfo == null || discInfo.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Disc not found.");
                    return true;
                }

                if (!permissionManager.canManage(player, discInfo)) {
                    player.sendMessage(ChatColor.RED + "You can't do that.");
                    return true;
                }

                permissionManager.unshareDisc(discInfo, targetPlayer);
                discManager.saveDisc(discName, discInfo);
                player.sendMessage(ChatColor.GREEN + "Unshared disc from " + targetPlayer + ".");
            } catch (IOException e) {
                player.sendMessage(ChatColor.RED + "Error unsharing disc.");
            }
            return true;
        }

        // Owners command
        if (args.length == 2 && args[0].equalsIgnoreCase("owners")) {
            String discName = args[1].toLowerCase();

            try {
                DiscJsonManager discManager = new DiscJsonManager(plugin);
                JSONObject discInfo = discManager.getDisc(discName);
                
                if (discInfo == null || discInfo.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Disc not found.");
                    return true;
                }

                String owner = discInfo.optString("owner", "None");
                player.sendMessage(ChatColor.GOLD + "Owner: " + ChatColor.WHITE + owner);

                JSONObject shared = discInfo.optJSONObject("shared");
                if (shared != null && shared.length() > 0) {
                    player.sendMessage(ChatColor.GOLD + "Shared with:");
                    for (String sharedPlayer : shared.keySet()) {
                        player.sendMessage(ChatColor.WHITE + "- " + sharedPlayer);
                    }
                }
            } catch (IOException e) {
                player.sendMessage(ChatColor.RED + "Error getting disc info.");
            }
            return true;
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
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Share/Unshare disc:");
            player.sendMessage(ChatColor.YELLOW + "/customdisc share " + ChatColor.GOLD + "<disc> <player> <use|give|delete|all>");
            player.sendMessage(ChatColor.YELLOW + "/customdisc unshare " + ChatColor.GOLD + "<disc> <player>");
            player.sendMessage(ChatColor.YELLOW + "/customdisc owners " + ChatColor.GOLD + "<disc>");
            player.sendMessage("");
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Show details of the custom music disc you're holding:");
            player.sendMessage(ChatColor.YELLOW + "/customdisc info");
            player.sendMessage("");
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Update Deno and yt-dlp dependencies:");
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
            // Check permission
            if (!permissionManager.canCreate(player)) {
                String reason = permissionManager.getCreationBlockedReason(player);
                player.sendMessage(ChatColor.RED + (reason != null ? reason : "You can't do that."));
                return true;
            }

            String input = args[1];
            String rawDiscName = args[2].replaceAll("[^a-zA-Z0-9_-]", "_");
            String audioType = args[3].toLowerCase();

            player.sendMessage(ChatColor.GRAY + "Processing audio...");

            try {
                new URL(input);

                if (("api".equalsIgnoreCase(pluginUsageMode) && plugin.getLocalYtDlp())
                        || "self-hosted".equalsIgnoreCase(pluginUsageMode)
                        || "edit-only".equalsIgnoreCase(pluginUsageMode)) {

                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        YtDlpManager ytDlpManager = new YtDlpManager(plugin, os);
                        File mp3File = new File(plugin.getAudioFolder(), rawDiscName + ".mp3");
                        boolean downloaded = ytDlpManager.downloadAudioWithYtDlp(input, mp3File);

                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (!downloaded || !mp3File.exists()) {
                                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Failed to download audio from the URL using yt-dlp.");
                                player.sendMessage(ChatColor.GRAY + "Attempting to update Deno and yt-dlp...");

                                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                    new DenoSetup(plugin, os).setup();
                                    new YtDlpSetup(plugin, os).setup();

                                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                        boolean retried = ytDlpManager.downloadAudioWithYtDlp(input, mp3File);

                                        Bukkit.getScheduler().runTask(plugin, () -> {
                                            if (!retried || !mp3File.exists()) {
                                                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Download failed even after updating yt-dlp.");
                                                return;
                                            }
                                            player.sendMessage(ChatColor.GREEN + "Audio downloaded after updating yt-dlp.");
                                            continueDiscCreation(player, mp3File.getName(), rawDiscName, audioType);
                                        });
                                    });
                                });
                            } else {
                                continueDiscCreation(player, mp3File.getName(), rawDiscName, audioType);
                            }
                        });
                    });

                } else {
                    continueDiscCreation(player, input, rawDiscName, audioType);
                }
                return true;

            } catch (MalformedURLException e) {
                File localMp3 = new File(plugin.getAudioToSendFolder(), input);
                if (localMp3.exists() && localMp3.isFile() && input.toLowerCase().endsWith(".mp3")) {

                    if ("api".equalsIgnoreCase(pluginUsageMode)) {
                        long maxSize = 12L * 1024L * 1024L;
                        if (localMp3.length() > maxSize) {
                            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "The audio file exceeds the maximum allowed size of 12MB.");
                            return true;
                        }
                        try {
                            Mp3File mp3file = new Mp3File(localMp3);
                            long durationSeconds = mp3file.getLengthInSeconds();
                            if (durationSeconds > 300) {
                                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "The audio file exceeds the maximum allowed length of 5 minutes.");
                                return true;
                            }
                        } catch (Exception ex) {
                            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Unable to read the duration of the audio file.");
                            return true;
                        }
                    } else if ("self-hosted".equalsIgnoreCase(pluginUsageMode) || "edit-only".equalsIgnoreCase(pluginUsageMode)) {
                        File destFile = new File(plugin.getTempAudioFolder(), input);
                        try {
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
            
            try {
                DiscJsonManager discManager = new DiscJsonManager(plugin);
                JSONObject discInfo = discManager.getDisc(discName);
                
                if (discInfo == null || discInfo.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Disc not found.");
                    return true;
                }

                if (!permissionManager.canGive(player, discInfo)) {
                    player.sendMessage(ChatColor.RED + "You can't do that.");
                    return true;
                }
            } catch (IOException e) {
                player.sendMessage(ChatColor.RED + "Error checking permissions.");
                return true;
            }

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

            List<String> discNames = new ArrayList<>(discData.keySet());
            Collections.sort(discNames);
            for (String discName : discNames) {
                JSONObject discInfo = discData.getJSONObject(discName);
                String displayName = discInfo.getString("displayName");
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

            if (discInfo == null || discInfo.isEmpty()) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Custom disc '" + discName + "' does not exist.");
                return true;
            }

            if (!permissionManager.canDelete(player, discInfo)) {
                player.sendMessage(ChatColor.RED + "You can't do that.");
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

                    JSONObject discData = DiscUtils.loadDiscData(discUuidFile);
                    String discName = DiscUtils.getDiscNameFromCustomModelData(discData, customModelData);

                    if (discName != null) {
                        JSONObject discInfo = discData.getJSONObject(discName);
                        String displayName = discInfo.getString("displayName");
                        String discUUID = discInfo.getString("uuid");
                        String soundKey = "customdisc." + discName.toLowerCase().replaceAll(" ", "_");

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

        // Command to update Deno and yt-dlp dependencies
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

                player.sendMessage(ChatColor.GRAY + "Checking for Deno and yt-dlp updates...");
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    DenoSetup denoSetup = new DenoSetup(plugin, os);
                    try {
                        denoSetup.setup();
                        player.sendMessage(ChatColor.GREEN + "Deno update check finished. See console for details.");
                    } catch (Exception e) {
                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Failed to update Deno: " + e.getMessage());
                    }

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
            discInfo = discManager.getOrCreateDisc(discName, displayName, player.getName());
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

            String content = Files.readString(discUuidFile.toPath());
            JSONObject discData = new JSONObject(content);

            if (!discData.has(discName)) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "The disc '" + discName + "' doesn't exist.");
                return;
            }

            JSONObject discInfo = discData.getJSONObject(discName);
            int customModelData = discInfo.getInt("customModelData");
            String displayName = discInfo.getString("displayName");

            ItemStack disc = new ItemStack(Material.MUSIC_DISC_13);
            ItemMeta meta = disc.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + displayName);
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Custom music disc: " + displayName);
                meta.setLore(lore);
                meta.setCustomModelData(customModelData);
