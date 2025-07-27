package com.urlcustomdiscs;
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

        // /Help
        if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
            player.sendMessage("");
            player.sendMessage(ChatColor.YELLOW + "Usage of the command " + ChatColor.GOLD + "/customdisc:");
            player.sendMessage("");
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Create a custom music disc from a YouTube URL:");
            player.sendMessage(ChatColor.YELLOW + "/customdisc create " + ChatColor.GOLD + "<" + ChatColor.YELLOW + "URL" + ChatColor.GOLD + "> <" + ChatColor.YELLOW + "disc name" + ChatColor.GOLD + "> <" + ChatColor.YELLOW + "mono " + ChatColor.GOLD + "/ " + ChatColor.YELLOW + "stereo" + ChatColor.GOLD + ">");
            player.sendMessage(ChatColor.GRAY + "- mono: enables spatial audio (like played in a jukebox)");
            player.sendMessage(ChatColor.GRAY + "- stereo: plays the sound in the traditional way");
            player.sendMessage("");
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Give a custom music disc:");
            player.sendMessage(ChatColor.YELLOW + "/customdisc give " + ChatColor.GOLD + "<" + ChatColor.YELLOW + "disc name" + ChatColor.GOLD + ">");
            player.sendMessage("");
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Display custom music discs list:");
            player.sendMessage(ChatColor.YELLOW + "/customdisc list");
            player.sendMessage("");
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Delete a custom music disc:");
            player.sendMessage(ChatColor.YELLOW + "/customdisc delete " + ChatColor.GOLD + "<" + ChatColor.YELLOW + "disc name" + ChatColor.GOLD + ">");
            player.sendMessage("");
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Display custom music disc details in hand:");
            player.sendMessage(ChatColor.YELLOW + "/customdisc info");
            player.sendMessage("");
            player.sendMessage(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Other useful (vanilla) command:");
            player.sendMessage(ChatColor.AQUA + "/execute positioned ~ ~ ~ run playsound minecraft:customdisc." + ChatColor.DARK_AQUA + "<" + ChatColor.AQUA + "disc name" + ChatColor.DARK_AQUA + "> " + ChatColor.AQUA + "ambient @a");
            player.sendMessage(ChatColor.AQUA + "/stopsound @a * minecraft:customdisc." + ChatColor.DARK_AQUA + "<" + ChatColor.AQUA + "disc name" + ChatColor.DARK_AQUA + ">");
            player.sendMessage("");
            return true;
        }

        // Commande pour créer un disque custom
        if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            String url = args[1];
            String rawDiscName = args[2].replaceAll("[^a-zA-Z0-9_-]", "_");
            String audioType = args[3].toLowerCase(); // "mono" ou "stereo"

            try {
                new URL(url);
            } catch (MalformedURLException e) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "The provided URL is invalid.");
                return false;
            }

            final String displayName = rawDiscName;
            final String discName = displayName.toLowerCase();

            if (pluginUsageMode.equalsIgnoreCase("api")) {
                DiscJsonManager discManager = new DiscJsonManager(plugin);
                JSONObject discInfo = null;
                try {
                    discInfo = discManager.getOrCreateDisc(discName, displayName);
                } catch (IOException e) {
                    e.printStackTrace();
                } final JSONObject discInfoFinal = discInfo;

                if (plugin.getToken().isEmpty()) {
                    remoteApiClient.requestTokenFromRemoteServer(player, () -> {
                        remoteApiClient.createCustomDiscRemotely(player, url, discName, audioType, discInfoFinal, plugin.getToken());
                    });
                } else {
                    remoteApiClient.createCustomDiscRemotely(player, url, discName, audioType, discInfoFinal, plugin.getToken());
                }
                return true;
            } else if (pluginUsageMode.equalsIgnoreCase("self-hosted")) {
                // Créer les fichiers mp3 et ogg
                File musicFolder = new File(plugin.getDataFolder(), "music");
                if (!musicFolder.exists()) musicFolder.mkdirs();

                String mp3FileName = discName + ".mp3";
                File mp3File = new File(musicFolder, mp3FileName);

                String oggFileName = discName + ".ogg";
                File oggFile = new File(musicFolder, oggFileName);

                // Supprimer les fichiers existants s'ils sont déjà présents
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
                                "--audio-format", "mp3", "-o", mp3File.getAbsolutePath(), url);
                        Process ytDlpProcess = ytDlp.start();

                        // Lire la sortie de yt-dlp pour débogage
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

                        int ytDlpExitCode = ytDlpProcess.waitFor(); // Attendre la fin du téléchargement
                        ytDlpOutputThread.join(); // S'assurer que la lecture de la sortie est terminée

                        if (ytDlpExitCode == 0) {
                            // This plugin uses libraries from the FFmpeg project under the LGPLv2.1
                            // Convertir le fichier mp3 en ogg mono ou stereo
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

                            // Lire la sortie de ffmpeg pour débogage
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

                            // Lire les erreurs de ffmpeg pour débogage
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

                            int ffmpegExitCode = ffmpegProcess.waitFor(); // Attendre la fin de la conversion

                            if (ffmpegExitCode == 0) {
                                // Suppression du fichier .mp3 après conversion si nécessaire
                                if (mp3File.exists()) mp3File.delete();
                                player.sendMessage(ChatColor.GRAY + "Music downloaded and converted.");
                                plugin.getLogger().info("Music downloaded and converted.");

                                // Add .ogg to server resource pack zip
                                if (minecraftServerType.equals("local")) {
                                    addFileToZip(zipFilePath, oggFile, "assets/minecraft/sounds/custom/" + oggFile.getName());
                                    // Delete the .ogg file after update
                                    if (oggFile.delete()) {
                                        plugin.getLogger().info("Deleted Ogg file from music folder.");

                                        // Met à jour sounds.json
                                        updateSoundsJson(discName);
                                        // Créer et donner le disque personnalisé au joueur
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

                                // Parcourir tous les joueurs en ligne et leur envoyer le pack de ressources
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

        // Commande pour se give un disque existant
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            String discName = args[1].toLowerCase().replaceAll(" ", "_");
            giveCustomMusicDisc(player, discName);
            return true;
        }

        // Commande pour afficher la liste des disques
        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            JSONObject discData = DiscUtils.loadDiscData(discUuidFile);

            if (discData.isEmpty()) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "No custom music disc found. Create a disc first (/customdisc help).");
                return true;
            }
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "List of custom music discs:");

            // Trier les noms des disques par ordre alphabétique
            List<String> discNames = new ArrayList<>(discData.keySet());
            Collections.sort(discNames);
            for (String discName : discNames) {
                // Récupérer l'objet correspondant au disque
                JSONObject discInfo = discData.getJSONObject(discName);
                // Récupérer le displayName de chaque disque
                String displayName = discInfo.getString("displayName");
                // Créer le TextComponent en utilisant le displayName
                TextComponent discText = createDiscTextComponent(displayName);
                player.spigot().sendMessage(discText);
            }
            return true;
        }

        // Commande de suppression d'un disque
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

                remoteApiClient.deleteCustomDiscRemotely(player, discName, discInfoFinal, token);
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

        // Commande pour obtenir des informations sur le disque en main
        if (args.length == 1 && args[0].equalsIgnoreCase("info")) {
            ItemStack itemInHand = player.getInventory().getItemInMainHand();

            if (itemInHand.hasItemMeta()) {
                ItemMeta meta = itemInHand.getItemMeta();
                if (meta != null && meta.hasCustomModelData()) {
                    int customModelData = meta.getCustomModelData();

                    // Charger les données des disques depuis le fichier JSON
                    JSONObject discData = DiscUtils.loadDiscData(discUuidFile);
                    // Trouver le nom du disque à partir du CustomModelData
                    String discName = DiscUtils.getDiscNameFromCustomModelData(discData, customModelData);

                    if (discName != null) {
                        JSONObject discInfo = discData.getJSONObject(discName);
                        String displayName = discInfo.getString("displayName");
                        String discUUID = discInfo.getString("uuid");
                        String soundKey = "customdisc." + discName.toLowerCase().replaceAll(" ", "_");

                        // Envoyer les informations au joueur
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

        player.sendMessage(ChatColor.GOLD + "Usage: " + ChatColor.YELLOW + "/customdisc help");

        return false;
    }

    private void updateSoundsJson(String discName) {
        String jsonPathInZip = "assets/minecraft/sounds.json";
        try {
            // Créer un fichier temporaire pour extraire le JSON
            File tempJson = File.createTempFile("sounds", ".json");

            // Extraire sounds.json du ZIP
            extractFileFromZip(zipFilePath, jsonPathInZip, tempJson);

            // Lire ou créer le JSON
            JSONObject soundsJson = tempJson.exists()
                    ? new JSONObject(Files.readString(tempJson.toPath()))
                    : new JSONObject();

            // Ajouter le nouveau son
            String soundKey = "customdisc." + discName;

            if (!soundsJson.has(soundKey)) {
                JSONObject soundData = new JSONObject();
                soundData.put("sounds", new JSONArray().put(new JSONObject()
                        .put("name", "custom/" + discName)
                        .put("stream", true)));

                soundsJson.put(soundKey, soundData);

                // Sauvegarder les modifications dans le fichier temporaire
                Files.writeString(tempJson.toPath(), soundsJson.toString(4));

                // Remettre le fichier modifié dans le ZIP
                addFileToZip(zipFilePath, tempJson, jsonPathInZip);
            }

            // Supprimer le fichier temporaire
            tempJson.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createCustomMusicDisc(Player player, String discName, String displayName) {
        try {
            // Charger le fichier JSON des disques
            JSONObject discData;
            if (discUuidFile.exists()) {
                String content = Files.readString(discUuidFile.toPath());
                discData = new JSONObject(content);
            } else {
                discData = new JSONObject();
            }

            // Vérifier si le disque existe déjà dans le fichier JSON
            JSONObject discInfo;
            if (discData.has(discName)) { // Récupérer les informations existantes
                discInfo = discData.getJSONObject(discName);
            } else { // Générer un nouvel UUID pour le disque
                String newUUID = UUID.randomUUID().toString();
                discInfo = new JSONObject();
                discInfo.put("uuid", newUUID);

                // Calculer le customModelData à partir de l'UUID
                // Supprime les tirets de l'UUID, Prend les 8 premiers caractères hexadécimaux, Convertit cette partie de l'UUID en un nombre long, Garde un nombre positif dans la limite de int
                int customModelData = (int) (Long.parseLong(newUUID.replace("-", "").substring(0, 8), 16) & 0x7FFFFFFF);
                discInfo.put("customModelData", customModelData);

                discInfo.put("displayName", displayName);

                // Enregistrer les nouvelles informations dans le fichier JSON
                discData.put(discName, discInfo);
                Files.writeString(discUuidFile.toPath(), discData.toString(4));
            }

            // Créer le disque
            ItemStack disc = new ItemStack(Material.MUSIC_DISC_13);
            ItemMeta meta = disc.getItemMeta();

            if (meta != null) {
                // Définir le nom du disque
                meta.setDisplayName(ChatColor.GOLD + displayName);

                // Ajouter une description personnalisée
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Custom music disc: " + displayName); // Description avec le nom du disque
                meta.setLore(lore);

                // Utiliser le customModelData calculé
                int customModelData = discInfo.getInt("customModelData");
                meta.setCustomModelData(customModelData);

                // Masquer "C418 - 13"
                // cant (or rather lazy to create a new json just for that)

                // Appliquer les modifications à l'objet ItemStack
                disc.setItemMeta(meta);

                updateDiscModelJson(discName, customModelData);
                createCustomMusicDiscJson(discName);
            }

            // Ajouter le disque à l'inventaire du joueur
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
            // Fichier temporaire pour extraire et modifier le JSON
            File tempJson = File.createTempFile("music_disc_13", ".json");

            // Extraire music_disc_13.json du ZIP
            extractFileFromZip(zipFilePath, modelPathInZip, tempJson);

            // Lire ou créer le JSON
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

            // Vérifier si l'override existe déjà
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

                // Ajouter le fichier modifié dans le ZIP
                addFileToZip(zipFilePath, tempJson, modelPathInZip);
            }

            tempJson.delete(); // Nettoyage

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createCustomMusicDiscJson(String discName) {
        String modelPathInZip = "assets/minecraft/models/item/custom_music_disc_" + discName + ".json";
        try {
            // Créer un fichier temporaire JSON
            File tempJson = File.createTempFile("custom_music_disc_" + discName, ".json");

            // Créer le JSON pour le disque
            JSONObject discJson = new JSONObject();
            discJson.put("parent", "minecraft:item/generated");
            discJson.put("textures", new JSONObject().put("layer0", "minecraft:item/record_custom"));

            // Écrire le fichier JSON temporaire
            Files.writeString(tempJson.toPath(), discJson.toString(4));
            // Ajouter le JSON dans le ZIP
            addFileToZip(zipFilePath, tempJson, modelPathInZip);

            tempJson.delete(); // Nettoyage

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

            // Copier tous les fichiers existants du ZIP
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.getName().equals(entryPath)) { // Ne pas dupliquer si déjà présent
                    zos.putNextEntry(new ZipEntry(entry.getName()));
                    while ((length = zis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                }
                zis.closeEntry();
            }

            // Ajouter le nouveau fichier
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

            // Lire l'entrée du ZIP et écrire dans outputFile
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

            // Lire le fichier JSON des disques
            String content = Files.readString(discUuidFile.toPath());
            JSONObject discData = new JSONObject(content);

            // Vérifier si le disque existe
            if (!discData.has(discName)) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "The disc '" + discName + "' doesn't exist.");
                return;
            }

            JSONObject discInfo = discData.getJSONObject(discName);
            int customModelData = discInfo.getInt("customModelData");
            String displayName = discInfo.getString("displayName");

            // Créer le disque avec les mêmes propriétés que lors de la création
            ItemStack disc = new ItemStack(Material.MUSIC_DISC_13);
            ItemMeta meta = disc.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + displayName);
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Custom music disc: " + displayName);
                meta.setLore(lore);
                meta.setCustomModelData(customModelData);
                // Masquer "C418 - 13" => cant (or rather lazy to create a new json just for that)
                disc.setItemMeta(meta);
            }

            // Ajouter le disque à l'inventaire du joueur
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
            // Lire et modifier le fichier JSON des disques
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

            // Récupérer le displayName du disque
            JSONObject discInfo = discData.getJSONObject(discName);
            String displayName = discInfo.getString("displayName");

            // Supprimer l'entrée du disque
            discData.remove(discName);
            Files.writeString(discUuidFile.toPath(), discData.toString(4));

            // Supprimer l'entrée du son dans sounds.json
            File tempSoundsJson = File.createTempFile("sounds", ".json");
            extractFileFromZip(zipFilePath, "assets/minecraft/sounds.json", tempSoundsJson);

            JSONObject soundsJson = new JSONObject(Files.readString(tempSoundsJson.toPath()));
            soundsJson.remove(soundKey);
            Files.writeString(tempSoundsJson.toPath(), soundsJson.toString(4));
            addFileToZip(zipFilePath, tempSoundsJson, "assets/minecraft/sounds.json");
            tempSoundsJson.delete();

            // Supprimer l'override dans music_disc_13.json
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

            // Supprimer le fichier JSON du disque
            deleteFileFromZip(zipFilePath, customMusicDiscJsonPath);

            // Supprimer le fichier .ogg associé (optionnel, à décommenter si nécessaire)
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