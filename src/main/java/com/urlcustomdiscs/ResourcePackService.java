package com.urlcustomdiscs;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class ResourcePackService {
    private final URLCustomDiscs plugin;
    private final String pluginUsageMode;

    public ResourcePackService(URLCustomDiscs plugin) {
        this.plugin = plugin;
        this.pluginUsageMode = plugin.getPluginUsageMode();
    }

    public boolean addDiscToResourcePack(Player player, File oggFile, String discName, JSONObject discInfo) {
        String resourcePackAccessMode = plugin.getResourcePackAccessMode();
        String displayName = discInfo.getString("displayName");
        int customModelData = discInfo.getInt("customModelData");

        if ("self-hosted".equalsIgnoreCase(pluginUsageMode)) {
            if ("local".equals(resourcePackAccessMode)) {
                return handleLocalAddDisc(player, oggFile, discName, displayName, customModelData);
            } else if ("online".equals(resourcePackAccessMode)) {
                return handleOnlineAddDisc(player, oggFile, discName, displayName, customModelData);
            } else {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Invalid Minecraft server type: " + resourcePackAccessMode + ". Please set the Minecraft server type to 'local' or 'online' in the config.yml file.");
                return false;
            }
        } else if ("edit-only".equalsIgnoreCase(pluginUsageMode)) {
            return handleLocalAddDisc(player, oggFile, discName, displayName, customModelData);
        }
        return false;
    }

    private boolean handleLocalAddDisc(Player player, File oggFile, String discName, String displayName, int customModelData) {
        ResourcePackManager rpm = new ResourcePackManager(plugin);
        File resourcePackZip;
        if ("self-hosted".equalsIgnoreCase(pluginUsageMode)) {
            resourcePackZip = new File(plugin.getZipFileAbsolutePath());
        } else if ("edit-only".equalsIgnoreCase(pluginUsageMode)) {
            resourcePackZip = new File(plugin.getEditOnlyModeZipFilePath());
        } else {
            throw new IllegalStateException("Unsupported pluginUsageMode: " + pluginUsageMode);
        }

        File tempUnpackedFolder = plugin.getTempUnpackedFolder();
        String minecraftServerVersion = plugin.getMinecraftServerVersion();

        try {
            // Unzip the resource pack in the tempUnpackedFolder
            rpm.unzipResourcePack(resourcePackZip, tempUnpackedFolder);

            // Add the Ogg file in the resource pack
            rpm.addOggFileToResourcePack(oggFile, tempUnpackedFolder, discName);

            // Add a custom disc entry to sounds.json
            rpm.addDiscEntryToSoundsJson(tempUnpackedFolder, discName);

            // Add a custom disc entry to music_disc_13.json
            rpm.addDiscEntryToMusicDisc13ModelJson(tempUnpackedFolder, discName, customModelData, minecraftServerVersion);

            // Create the custom disc JSON
            rpm.createCustomDiscModelJson(tempUnpackedFolder, discName, minecraftServerVersion);

            // Rezip the modified resource pack
            rpm.rezipResourcePack(tempUnpackedFolder, resourcePackZip);

            player.sendMessage(ChatColor.GREEN + "Custom disc " + ChatColor.GOLD + displayName + ChatColor.GREEN + " created.");
            return true;

        } catch (IOException e) {
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error updating the locally-hosted resource pack with a new custom disc.");
            plugin.getLogger().severe("Error updating the locally-hosted resource pack with a new custom disc: "  + e.getMessage());
            throw new RuntimeException(e);
        } finally { // Cleanup
            deleteFolderContents(tempUnpackedFolder, false);
            oggFile.delete();
        }
    }

    private boolean handleOnlineAddDisc(Player player, File oggFile, String discName, String displayName, int customModelData) {
        ResourcePackManager rpm = new ResourcePackManager(plugin);
        File editResourcePackFolder = plugin.getEditResourcePackFolder();
        File tempUnpackedFolder = plugin.getTempUnpackedFolder();
        String downloadResourcePackUrl = plugin.getDownloadResourcePackURL();
        String minecraftServerVersion = plugin.getMinecraftServerVersion();
        String uploadResourcePackUrl = plugin.getUploadResourcePackURL();

        File downloadedZip = null;

        try {
            // Download the resource pack
            downloadedZip = rpm.downloadResourcePack(editResourcePackFolder, downloadResourcePackUrl);

            // Unzip the downloaded resource pack
            rpm.unzipResourcePack(downloadedZip, tempUnpackedFolder);

            // Add the Ogg file in the resource pack
            rpm.addOggFileToResourcePack(oggFile, tempUnpackedFolder, discName);

            // Add a custom disc entry to sounds.json
            rpm.addDiscEntryToSoundsJson(tempUnpackedFolder, discName);

            // Add a custom disc entry to music_disc_13.json
            rpm.addDiscEntryToMusicDisc13ModelJson(tempUnpackedFolder, discName, customModelData, minecraftServerVersion);

            // Create the custom disc JSON
            rpm.createCustomDiscModelJson(tempUnpackedFolder, discName, minecraftServerVersion);

            // Rezip the modified resource pack
            rpm.rezipResourcePack(tempUnpackedFolder, downloadedZip);

            // Upload back to the HTTP server
            if (!rpm.uploadResourcePack(downloadedZip, uploadResourcePackUrl)) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error uploading the resource pack to the HTTP server.");
                return false;
            }

            player.sendMessage(ChatColor.GREEN + "Custom disc " + ChatColor.GOLD + displayName + ChatColor.GREEN + " created.");
            return true;

        } catch (IOException e) {
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error updating the online-hosted resource pack with a new custom disc.");
            plugin.getLogger().severe("Error updating the online-hosted resource pack with a new custom disc: " + e.getMessage());
            throw new RuntimeException(e);
        } finally { // Cleanup
            deleteFolderContents(tempUnpackedFolder, false);
            oggFile.delete();
            downloadedZip.delete();
        }
    }

    public boolean removeDiscFromResourcePack(Player player, String discName, JSONObject discInfo) {
        String resourcePackAccessMode = plugin.getResourcePackAccessMode();
        String displayName = discInfo.getString("displayName");
        int customModelData = discInfo.getInt("customModelData");

        if ("self-hosted".equalsIgnoreCase(pluginUsageMode)) {
            if ("local".equals(resourcePackAccessMode)) {
                return handleLocalRemoveDisc(player, discName, displayName, customModelData);
            } else if ("online".equals(resourcePackAccessMode)) {
                return handleOnlineRemoveDisc(player, discName, displayName, customModelData);
            }
        } else if ("edit-only".equalsIgnoreCase(pluginUsageMode)) {
            return handleLocalRemoveDisc(player, discName, displayName, customModelData);
        }
        return false;
    }

    private boolean handleLocalRemoveDisc(Player player, String discName, String displayName, int customModelData) {
        ResourcePackManager rpm = new ResourcePackManager(plugin);
        File resourcePackZip;
        if ("self-hosted".equalsIgnoreCase(pluginUsageMode)) {
            resourcePackZip = new File(plugin.getZipFileAbsolutePath());
        } else if ("edit-only".equalsIgnoreCase(pluginUsageMode)) {
            resourcePackZip = new File(plugin.getEditOnlyModeZipFilePath());
        } else {
            throw new IllegalStateException("Unsupported pluginUsageMode: " + pluginUsageMode);
        }

        File tempUnpackedFolder = plugin.getTempUnpackedFolder();
        String minecraftServerVersion = plugin.getMinecraftServerVersion();

        try {
            // Unzip the resource pack in the tempUnpackedFolder
            rpm.unzipResourcePack(resourcePackZip, tempUnpackedFolder);

            // Remove the Ogg file in the resource pack
            rpm.removeOggFileFromResourcePack(tempUnpackedFolder, discName);

            // Remove a custom disc entry from sounds.json
            rpm.removeDiscEntryFromSoundsJson(tempUnpackedFolder, discName);

            // Remove a custom disc entry to music_disc_13.json
            rpm.removeDiscEntryToMusicDisc13ModelJson(tempUnpackedFolder, customModelData, minecraftServerVersion);

            // Delete the custom disc JSON
            rpm.deleteCustomDiscModelJson(tempUnpackedFolder, discName);

            // Rezip the modified resource pack
            rpm.rezipResourcePack(tempUnpackedFolder, resourcePackZip);

            player.sendMessage(ChatColor.GREEN + "Custom disc " + ChatColor.GOLD + displayName + ChatColor.GREEN + " deleted.");
            return true;

        } catch (IOException e) {
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error updating the locally-hosted resource pack with a custom disc removal.");
            plugin.getLogger().severe("Error updating the locally-hosted resource pack with a custom disc removal: "  + e.getMessage());
            throw new RuntimeException(e);
        } finally { // Cleanup
            deleteFolderContents(tempUnpackedFolder, false);
        }
    }

    private boolean handleOnlineRemoveDisc(Player player, String discName, String displayName, int customModelData) {
        ResourcePackManager rpm = new ResourcePackManager(plugin);
        File editResourcePackFolder = plugin.getEditResourcePackFolder();
        File tempUnpackedFolder = plugin.getTempUnpackedFolder();
        String downloadResourcePackUrl = plugin.getDownloadResourcePackURL();
        String minecraftServerVersion = plugin.getMinecraftServerVersion();
        String uploadResourcePackUrl = plugin.getUploadResourcePackURL();

        File downloadedZip = null;

        try {
            // Download the resource pack
            downloadedZip = rpm.downloadResourcePack(editResourcePackFolder, downloadResourcePackUrl);

            // Unzip the downloaded resource pack
            rpm.unzipResourcePack(downloadedZip, tempUnpackedFolder);

            // Remove the Ogg file in the resource pack
            rpm.removeOggFileFromResourcePack(tempUnpackedFolder, discName);

            // Remove a custom disc entry from sounds.json
            rpm.removeDiscEntryFromSoundsJson(tempUnpackedFolder, discName);

            // Remove a custom disc entry to music_disc_13.json
            rpm.removeDiscEntryToMusicDisc13ModelJson(tempUnpackedFolder, customModelData, minecraftServerVersion);

            // Delete the custom disc JSON
            rpm.deleteCustomDiscModelJson(tempUnpackedFolder, discName);

            // Rezip the modified resource pack
            rpm.rezipResourcePack(tempUnpackedFolder, downloadedZip);

            // Upload back to the HTTP server
            if (!rpm.uploadResourcePack(downloadedZip, uploadResourcePackUrl)) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error uploading the resource pack to the HTTP server.");
                return false;
            }

            player.sendMessage(ChatColor.GREEN + "Custom disc " + ChatColor.GOLD + displayName + ChatColor.GREEN + " deleted.");
            return true;

        } catch (IOException e) {
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error updating the online-hosted resource pack with a custom disc removal.");
            plugin.getLogger().severe("Error updating the online-hosted resource pack with a custom disc removal: " + e.getMessage());
            throw new RuntimeException(e);
        } finally { // Cleanup
            deleteFolderContents(tempUnpackedFolder, false);
            downloadedZip.delete();
        }
    }

    private void deleteFolderContents(File folder, boolean deleteFolderItself) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolderContents(file, true); // Delete the contents of the folder recursively
                } else {
                    file.delete();
                }
            }
        }
        if (deleteFolderItself) {
            folder.delete();
        }
    }
}