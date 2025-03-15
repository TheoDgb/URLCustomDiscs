package com.mcytdisc;
import com.mcytdisc.utils.DiscUtils;

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

public class CommandMCytDisc implements CommandExecutor {

    private final MCytDISC plugin;
    private final File discUuidFile;
    private final String zipFilePath;
    private final String resourcePackURL;

    public CommandMCytDisc(MCytDISC plugin) {
        this.plugin = plugin;
        this.discUuidFile = new File(plugin.getDataFolder(), "discs.json");
        this.zipFilePath = plugin.getZipFilePath();
        this.resourcePackURL = plugin.getResourcePackURL();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return false;
        }

        Player player = (Player) sender;

        // /Help
        if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
            player.sendMessage("");
            player.sendMessage(ChatColor.YELLOW + "Usage of the command " + ChatColor.GOLD + "/mcytdisc:");
            player.sendMessage("");
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Create a custom disc from a YouTube URL:");
            player.sendMessage(ChatColor.YELLOW + "/mcytdisc create " + ChatColor.GOLD + "<" + ChatColor.YELLOW + "URL" + ChatColor.GOLD + "> <" + ChatColor.YELLOW + "disc name" + ChatColor.GOLD + "> <" + ChatColor.YELLOW + "mono " + ChatColor.GOLD + "/ " + ChatColor.YELLOW + "stereo" + ChatColor.GOLD + ">");
            player.sendMessage(ChatColor.GRAY + "- mono: enables spatial audio (like played in a jukebox)");
            player.sendMessage(ChatColor.GRAY + "- stereo: plays the sound in the traditional way");
            player.sendMessage("");
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Give a custom disc:");
            player.sendMessage(ChatColor.YELLOW + "/mcytdisc give " + ChatColor.GOLD + "<" + ChatColor.YELLOW + "disc name" + ChatColor.GOLD + ">");
            player.sendMessage("");
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Display custom discs list:");
            player.sendMessage(ChatColor.YELLOW + "/mcytdisc list");
            player.sendMessage("");
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Delete a custom disc:");
            player.sendMessage(ChatColor.YELLOW + "/mcytdisc delete " + ChatColor.GOLD + "<" + ChatColor.YELLOW + "disc name" + ChatColor.GOLD + ">");
            player.sendMessage("");
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Display custom disc details in hand:");
            player.sendMessage(ChatColor.YELLOW + "/mcytdisc info");
            player.sendMessage("");
            player.sendMessage(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Other useful (vanilla) command:");
            player.sendMessage(ChatColor.AQUA + "/execute positioned ~ ~ ~ run playsound minecraft:mcytdisc." + ChatColor.DARK_AQUA + "<" + ChatColor.AQUA + "disc name" + ChatColor.DARK_AQUA + "> " + ChatColor.AQUA + "ambiant @a");
            player.sendMessage(ChatColor.AQUA + "/stopsound @a * minecraft:mcytdisc." + ChatColor.DARK_AQUA + "<" + ChatColor.AQUA + "disc name" + ChatColor.DARK_AQUA + ">");
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
            player.sendMessage(ChatColor.GRAY + "Downloading YouTube music...");

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
                    // Télécharger l'audio
                    ProcessBuilder ytDlp = new ProcessBuilder("./yt-dlp.exe", "-f", "bestaudio[ext=m4a]/best",
                            "--audio-format", "mp3", "-o", mp3File.getAbsolutePath(), url);
                    Process ytDlpProcess = ytDlp.start();

                    // Lire la sortie de yt-dlp pour débogage
                    new Thread(() -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(ytDlpProcess.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                System.out.println("yt-dlp: " + line); // Affiche la sortie pour débogage
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();

                    ytDlpProcess.waitFor();  // Attendre la fin du téléchargement

                    // This plugin uses libraries from the FFmpeg project under the LGPLv2.1
                    // Convertir le fichier mp3 en ogg mono ou stereo
                    ProcessBuilder ffmpeg;
                    if (audioType.equals("mono")) {
                        ffmpeg = new ProcessBuilder("./FFmpeg/bin/ffmpeg.exe", "-i", mp3File.getAbsolutePath(), "-ac", "1", "-c:a", "libvorbis", oggFile.getAbsolutePath());
                    } else if (audioType.equals("stereo")) {
                        ffmpeg = new ProcessBuilder("./FFmpeg/bin/ffmpeg.exe", "-i", mp3File.getAbsolutePath(), "-c:a", "libvorbis", oggFile.getAbsolutePath());
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
                                System.out.println("FFmpeg stdout: " + line); // Affiche la sortie pour débogage
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
                                System.err.println("ffmpeg stderr: " + line); // Affiche les erreurs pour débogage
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();

                    int ffmpegExitCode = ffmpegProcess.waitFor();  // Attendre la fin de la conversion

                    if (ffmpegExitCode == 0) {
                        // Suppression du fichier .mp3 après conversion si nécessaire
                        if (mp3File.exists()) mp3File.delete();
                        player.sendMessage(ChatColor.GRAY + "Music downloaded and converted to .ogg!");

                        // Ajouter le .ogg au resourcepack zip
                        addFileToZip("C:/Apache24/Apache24/htdocs/MCytDiscPack.zip", oggFile, "assets/minecraft/sounds/custom/" + oggFile.getName());
                        player.sendMessage(ChatColor.GRAY + "Music added to the server resource pack!");

                        // Met à jour sounds.json
                        updateSoundsJson(discName);

                        // Créer et donner le disque personnalisé au joueur
                        createCustomDisc(player, discName, displayName);

                        // Parcourir tous les joueurs en ligne et leur envoyer le pack de ressources
                        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                            onlinePlayer.setResourcePack(resourcePackURL);
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error converting to .ogg.");
                    }
                } catch (IOException | InterruptedException e) {
                    player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error downloading or converting music.");
                    e.printStackTrace();
                }
            }).start();
            return true;
        }

        // Commande pour se give un disque existant
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            String discName = args[1].toLowerCase().replaceAll(" ", "_");
            giveCustomDisc(player, discName);
            return true;
        }

        // Commande pour afficher la liste des disques
        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            JSONObject discData = DiscUtils.loadDiscData(discUuidFile);

            if (discData.isEmpty()) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "No custom disc found. Create a disc first (/mcytdisc help).");
                return true;
            }
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "List of custom discs:");

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
            deleteCustomDisc(player, discName);
            return true;
        }

        // Commande pour obtenir des informations sur le disque en main
        if (args.length == 1 && args[0].equalsIgnoreCase("info")) {
            ItemStack itemInHand = player.getInventory().getItemInMainHand();

            if (itemInHand != null && itemInHand.hasItemMeta()) {
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
                        String soundKey = "mcytdisc." + discName.toLowerCase().replaceAll(" ", "_");

                        // Envoyer les informations au joueur
                        player.sendMessage(ChatColor.GRAY + "Disc played: " + ChatColor.GOLD + discName);
                        player.sendMessage(ChatColor.GRAY + "Display name: " + ChatColor.GOLD + displayName);
                        player.sendMessage(ChatColor.GRAY + "UUID: " + ChatColor.GOLD + discUUID);
                        player.sendMessage(ChatColor.GRAY + "CustomModelData: " + ChatColor.GOLD + customModelData);
                        player.sendMessage(ChatColor.GRAY + "SoundKey: " + ChatColor.GOLD + soundKey);

                    } else {
                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "No custom disc found with this CustomModelData.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You must be holding a custom disc.");
                }
            } else {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You must be holding a custom disc.");
            }
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "Usage: " + ChatColor.YELLOW + "/mcytdisc help");

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
            String soundKey = "mcytdisc." + discName;

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

    private void createCustomDisc(Player player, String discName, String displayName) {
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
                createCustomDiscJson(discName);
            }

            // Ajouter le disque à l'inventaire du joueur
            player.getInventory().addItem(disc);
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
            newOverride.put("model", "item/custom_disc_" + discName);

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

    private void createCustomDiscJson(String discName) {
        String modelPathInZip = "assets/minecraft/models/item/custom_disc_" + discName + ".json";
        try {
            // Créer un fichier temporaire JSON
            File tempJson = File.createTempFile("custom_disc_" + discName, ".json");

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

        // Remplace l'ancien ZIP par le nouveau
        if (zipFile.delete() && tempZipFile.renameTo(zipFile)) {
            System.out.println("ZIP updated!");
        } else {
            System.out.println("Error replacing ZIP.");
        }

        // Supprime le fichier original après l'ajout dans le ZIP
        if (fileToAdd.delete()) {
            System.out.println("Deleted .ogg file from music folder.");
        } else {
            System.out.println("Error deleting .ogg file.");
        }
    }

    private void extractFileFromZip(String zipFilePath, String fileInZip, File outputFile) {
        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            ZipEntry entry = zipFile.getEntry(fileInZip);
            if (entry == null) {
                System.out.println("The file " + fileInZip + " doesn't exist in the ZIP.");
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

    private void giveCustomDisc(Player player, String discName) {
        try {
            if (!discUuidFile.exists()) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "No custom disc found. Create a disc first (/mcytdisc help).");
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
            player.sendMessage(ChatColor.GRAY + "The disc '" + displayName + "' has been added to your inventory!");
        } catch (IOException e) {
            e.printStackTrace();
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error recovering disc.");
        }
    }

    private void deleteCustomDisc(Player player, String discName) {
        String soundKey = "mcytdisc." + discName;
        String customDiscJsonPath = "assets/minecraft/models/item/custom_disc_" + discName + ".json";
        String oggFilePath = "assets/minecraft/sounds/custom/" + discName + ".ogg";
        try {
            // Lire et modifier le fichier JSON des disques
            if (!discUuidFile.exists()) {
                player.sendMessage(ChatColor.RED + "No custom discs found.");
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
                if (overrides.getJSONObject(i).getString("model").equals("item/custom_disc_" + discName)) {
                    overrides.remove(i);
                    break;
                }
            }

            Files.writeString(tempModelJson.toPath(), modelJson.toString(4));
            addFileToZip(zipFilePath, tempModelJson, "assets/minecraft/models/item/music_disc_13.json");
            tempModelJson.delete();

            // Supprimer le fichier JSON du disque
            deleteFileFromZip(zipFilePath, customDiscJsonPath);

            // Supprimer le fichier .ogg associé (optionnel, à décommenter si nécessaire)
            deleteFileFromZip(zipFilePath, oggFilePath);

            player.sendMessage(ChatColor.GREEN + "The disc '" + displayName + "' has been deleted successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            player.sendMessage(ChatColor.RED + "Error deleting the custom disc.");
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
            System.out.println("Deleted " + filePath + " from ZIP.");
        } else {
            System.out.println("Error deleting " + filePath + " from ZIP.");
        }
    }

    private TextComponent createDiscTextComponent(String displayName) {
        TextComponent discText = new TextComponent(displayName);
        discText.setColor(net.md_5.bungee.api.ChatColor.GOLD);
        discText.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/mcytdisc give " + displayName));
        discText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.YELLOW + "Click to get this disc!")));
        return discText;
    }
}