package com.urlcustomdiscs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class SelfHostedManager {
    private final URLCustomDiscs plugin;
    private final URLCustomDiscs.OS os;
    private final String pluginUsageMode;

    public SelfHostedManager(URLCustomDiscs plugin, URLCustomDiscs.OS os) {
        this.plugin = plugin;
        this.os = os;
        this.pluginUsageMode = plugin.getPluginUsageMode();
    }

    public void createCustomDisc(Player player, String mp3FileName, String discName, String audioType, JSONObject discInfo) {
        // Convert the MP3 file to Ogg Vorbis in the edit_resource_pack/temp_audio folder
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            FFmpegManager ffmpegManager = new FFmpegManager(plugin, os);
            File oggFile = new File(plugin.getTempAudioFolder(), discName + ".ogg");
            boolean converted = ffmpegManager.convertAudioWithFFmpeg(mp3FileName, oggFile, audioType);

            if (!converted || !oggFile.exists()) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Failed to convert the MP3 audio to Ogg Vorbis using FFmpeg.")
                );
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(ChatColor.GREEN + "Audio downloaded and converted.")
            );

            // Delete the MP3 file in the temp_audio folder
            File mp3File = new File(plugin.getTempAudioFolder(), mp3FileName);
            if (mp3File.exists()) mp3File.delete();

            // Update the resource pack with the new custom disc
            ResourcePackService resourcePackService = new ResourcePackService(plugin);
            if (!resourcePackService.addDiscToResourcePack(player, oggFile, discName, discInfo)) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error adding the custom disc to the resource pack.")
                );
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                if ("self-hosted".equalsIgnoreCase(pluginUsageMode)) {
                    // Generate the custom disc in memory and add it to the player's inventory
                    DiscFactory.giveCustomDiscToPlayer(player, discInfo);

                    // Browse all online players and send them the resource pack
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        onlinePlayer.setResourcePack(plugin.getDownloadResourcePackURL());
                    }
                } else if ("edit-only".equalsIgnoreCase(pluginUsageMode)) {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        // Duplicate the updated resource pack to a custom path with a custom name
                        duplicateResourcePack(player);
                    });
                }
            });
        });
    }

    public void deleteCustomDisc(Player player, String discName, JSONObject discInfo) {
        // Delete a custom disc from the resource pack
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ResourcePackService resourcePackService = new ResourcePackService(plugin);
            if (!resourcePackService.removeDiscFromResourcePack(player, discName, discInfo)) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error deleting the custom disc from the resource pack."));
                return;
            }

            try { // Remove the disc entry from the local disc JSON file
                DiscJsonManager discManager = new DiscJsonManager(plugin);
                discManager.deleteDisc(discName);

                if ("edit-only".equalsIgnoreCase(pluginUsageMode)) {
                    // Duplicate the updated resource pack to a custom path with a custom name
                    duplicateResourcePack(player);
                }
            } catch (IOException ex) {
                plugin.getLogger().warning("Failed to clean up local disc entry: " + ex.getMessage());
            }
        });
    }

    // Duplicate the updated resource pack to a custom path with a custom name
    public void duplicateResourcePack(Player player) {
        File sourceZip = new File(plugin.getEditOnlyModeReferenceResourcePackFolder(), "URLCustomDiscsPack.zip");
        File targetZip = new File(plugin.getDataFolder().getParentFile(), plugin.getDuplicatedZipFilePath());

        try {
            // Ensure the target folder exists
            if (!targetZip.getParentFile().exists()) {
                targetZip.getParentFile().mkdirs();
            }

            // Copy the source file to the target file
            Files.copy(sourceZip.toPath(), targetZip.toPath(), StandardCopyOption.REPLACE_EXISTING);

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(ChatColor.GREEN + "Resource pack duplicated successfully to:");
                player.sendMessage(ChatColor.LIGHT_PURPLE + targetZip.getPath());
            });
        } catch (IOException e) {
            plugin.getLogger().severe("Exception: " + e.getMessage());
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Failed to duplicate the resource pack: " + e.getMessage()));
        }
    }
}