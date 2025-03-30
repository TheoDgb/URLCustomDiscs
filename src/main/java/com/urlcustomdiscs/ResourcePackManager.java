package com.urlcustomdiscs;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.bukkit.Bukkit.getLogger;

public class ResourcePackManager {
    private final File discUuidFile;
    private final File editResourcePackFolder;
    private final File resourcePackFile;
    private final String downloadResourcePackURL;
    private final String uploadResourcePackURL;

    public ResourcePackManager(File discUuidFile, File editResourcePackFolder, String downloadResourcePackURL, String uploadResourcePackURL) {
        this.discUuidFile = discUuidFile;
        this.editResourcePackFolder = editResourcePackFolder;
        this.resourcePackFile = new File(editResourcePackFolder, "URLCustomDiscsPack.zip");
        this.downloadResourcePackURL = downloadResourcePackURL;
        this.uploadResourcePackURL = uploadResourcePackURL;
    }

    public File getResourcePackFile() {
        return resourcePackFile;
    }

    public boolean downloadResourcePack() {
        getLogger().info("Downloading resource pack...");
        if (!editResourcePackFolder.exists()) {
            editResourcePackFolder.mkdirs();
        }

        File destinationFile = new File(editResourcePackFolder, "URLCustomDiscsPack.zip");

        try (BufferedInputStream in = new BufferedInputStream(new URL(downloadResourcePackURL).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(destinationFile)) {

            HttpURLConnection connection = (HttpURLConnection) new URL(downloadResourcePackURL).openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                getLogger().severe("Failed to download: " + connection.getResponseMessage());
                return false;
            }

            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }

            getLogger().info("Server resource pack downloaded: " + destinationFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean uploadResourcePack() {
        if (!resourcePackFile.exists()) {
            getLogger().severe("Server resource pack file not found.");
            return false;
        }

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(uploadResourcePackURL).openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream outputStream = connection.getOutputStream();
                 FileInputStream fileInputStream = new FileInputStream(resourcePackFile);
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true)) {

                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                        .append(resourcePackFile.getName()).append("\"\r\n");
                writer.append("Content-Type: application/zip\r\n\r\n");
                writer.flush();

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();

                writer.append("\r\n--").append(boundary).append("--\r\n");
                writer.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                getLogger().info("Server resource pack uploaded.");

                InputStream responseStream = connection.getInputStream();
                Scanner s = new Scanner(responseStream).useDelimiter("\\A");
                String response = s.hasNext() ? s.next() : "";
                getLogger().warning("Server response: " + response);

                if (resourcePackFile.delete()) {
                    getLogger().info("Edited server resource pack deleted in plugin file.");
                } else {
                    getLogger().severe("Error deleting the edited server resource pack in plugin file.");
                }

                return true;
            } else {
                getLogger().severe("Error uploading the server resource pack: " + connection.getResponseMessage());
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }



    public boolean createCustomMusicDisc(Player player, String discName, String displayName) {
        try {
            JSONObject discData = discUuidFile.exists()
                    ? new JSONObject(Files.readString(discUuidFile.toPath()))
                    : new JSONObject();

            JSONObject discInfo;
            if (discData.has(discName)) {
                discInfo = discData.getJSONObject(discName);
            } else {
                String newUUID = UUID.randomUUID().toString();
                discInfo = new JSONObject();
                discInfo.put("uuid", newUUID);
                // Calculer le customModelData à partir de l'UUID
                // Supprime les tirets de l'UUID, Prend les 8 premiers caractères hexadécimaux, Convertit cette partie de l'UUID en un nombre long, Garde un nombre positif dans la limite de int
                int customModelData = (int) (Long.parseLong(newUUID.replace("-", "").substring(0, 8), 16) & 0x7FFFFFFF);
                discInfo.put("customModelData", customModelData);
                discInfo.put("displayName", displayName);
                discData.put(discName, discInfo); // Enregistrer les nouvelles informations dans le fichier JSON
                Files.writeString(discUuidFile.toPath(), discData.toString(4));
            }

            // Créer le disque
            ItemStack disc = new ItemStack(Material.MUSIC_DISC_13);
            ItemMeta meta = disc.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + displayName); // Définir le nom du disque
                // Ajouter une description personnalisée
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Custom music disc: " + displayName);
                meta.setLore(lore);

                // Utiliser le customModelData calculé
                meta.setCustomModelData(discInfo.getInt("customModelData"));

                // Masquer "C418 - 13"
                // cant (or rather lazy to create a new json just for that)

                disc.setItemMeta(meta); // Appliquer les modifications à l'objet ItemStack

                updateSoundsJson(discName);
                updateDiscModelJson(discName, discInfo.getInt("customModelData"));
                createCustomMusicDiscJson(discName);
            }

            player.getInventory().addItem(disc); // Ajouter le disque à l'inventaire du joueur
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void updateSoundsJson(String discName) {
        String jsonPathInZip = "assets/minecraft/sounds.json";
        try {
            File tempJson = File.createTempFile("sounds", ".json");
            extractFileFromZip(resourcePackFile, jsonPathInZip, tempJson);

            JSONObject soundsJson = tempJson.exists()
                    ? new JSONObject(Files.readString(tempJson.toPath()))
                    : new JSONObject();

            String soundKey = "customdisc." + discName;
            if (!soundsJson.has(soundKey)) {
                JSONObject soundData = new JSONObject();
                soundData.put("sounds", new JSONArray().put(new JSONObject()
                        .put("name", "custom/" + discName)
                        .put("stream", true)));
                soundsJson.put(soundKey, soundData);
                Files.writeString(tempJson.toPath(), soundsJson.toString(4));
                addFileToResourcePack(tempJson, jsonPathInZip);
            }
            tempJson.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateDiscModelJson(String discName, int customModelData) {
        String modelPathInZip = "assets/minecraft/models/item/music_disc_13.json";
        try {
            // Fichier temporaire pour extraire et modifier le JSON
            File tempJson = File.createTempFile("music_disc_13", ".json");
            extractFileFromZip(resourcePackFile, modelPathInZip, tempJson); // Extraire music_disc_13.json du ZIP
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
                JSONObject obj = overrides.getJSONObject(i);
                if (obj.getJSONObject("predicate").getInt("custom_model_data") == customModelData) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                overrides.put(newOverride);
                Files.writeString(tempJson.toPath(), modelJson.toString(4));
                addFileToResourcePack(tempJson, modelPathInZip);
            }
            tempJson.delete();
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

            Files.writeString(tempJson.toPath(), discJson.toString(4)); // Écrire le fichier JSON temporaire
            addFileToResourcePack(tempJson, modelPathInZip); // Ajouter le JSON dans le ZIP
            tempJson.delete(); // Nettoyage
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void extractFileFromZip(File zipFile, String filePath, File outputFile) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            ZipEntry entry;
            byte[] buffer = new byte[1024];
            int length;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(filePath)) {
                    while ((length = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }
                    return;
                }
            }
        }
    }

    public boolean addFileToResourcePack(File fileToAdd, String entryPath) {
        File tempZipFile = new File(resourcePackFile.getAbsolutePath() + ".tmp");
        try (
                FileInputStream fis = new FileInputStream(fileToAdd);
                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempZipFile));
                ZipInputStream zis = new ZipInputStream(new FileInputStream(resourcePackFile))
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
            return false;
        }

        // Replace the old server resource pack by the new one
        if (resourcePackFile.delete() && tempZipFile.renameTo(resourcePackFile)) {
            getLogger().info("Server resource pack updated.");
        } else {
            getLogger().severe("Error replacing the server resource pack.");
            return false;
        }

        return true;
    }



    public boolean deleteCustomDiscFromResourcePack(Player player, String discName) {
        String soundKey = "customdisc." + discName;
        String customMusicDiscJsonPath = "assets/minecraft/models/item/custom_music_disc_" + discName + ".json";
        String oggFilePath = "assets/minecraft/sounds/custom/" + discName + ".ogg";
        String modelJsonPath = "assets/minecraft/models/item/music_disc_13.json";

        try {
            if (!discUuidFile.exists()) {
                player.sendMessage(ChatColor.RED + "No custom music disc found.");
            }

            JSONObject discData = new JSONObject(Files.readString(discUuidFile.toPath()));
            if (!discData.has(discName)) {
                player.sendMessage(ChatColor.RED + "The disc '" + discName + "' doesn't exist.");
            }

            // Récupération du nom du disque pour affichage
            JSONObject discInfo = discData.getJSONObject(discName);
            String displayName = discInfo.getString("displayName");

            // Suppression de l'entrée du disque
            discData.remove(discName);
            Files.writeString(discUuidFile.toPath(), discData.toString(4));

            // Suppression de l'entrée dans sounds.json
            File tempSoundsJson = File.createTempFile("sounds", ".json");
            extractFileFromZip(resourcePackFile, "assets/minecraft/sounds.json", tempSoundsJson);
            JSONObject soundsJson = new JSONObject(Files.readString(tempSoundsJson.toPath()));
            soundsJson.remove(soundKey);
            Files.writeString(tempSoundsJson.toPath(), soundsJson.toString(4));
            addFileToResourcePack(tempSoundsJson, "assets/minecraft/sounds.json");
            tempSoundsJson.delete();

            // Suppression du modèle dans music_disc_13.json
            File tempModelJson = File.createTempFile("music_disc_13", ".json");
            extractFileFromZip(resourcePackFile, modelJsonPath, tempModelJson);
            JSONObject modelJson = new JSONObject(Files.readString(tempModelJson.toPath()));
            JSONArray overrides = modelJson.getJSONArray("overrides");

            for (int i = 0; i < overrides.length(); i++) {
                if (overrides.getJSONObject(i).getString("model").equals("item/custom_music_disc_" + discName)) {
                    overrides.remove(i);
                    break;
                }
            }

            Files.writeString(tempModelJson.toPath(), modelJson.toString(4));
            addFileToResourcePack(tempModelJson, modelJsonPath);
            tempModelJson.delete();

            // Suppression des fichiers JSON et OGG
            if (deleteFileFromResourcePack(customMusicDiscJsonPath) && deleteFileFromResourcePack(oggFilePath)) {
                player.sendMessage(ChatColor.GRAY + "Custom disc " + ChatColor.GOLD + displayName + ChatColor.GRAY + " deleted.");
            } else {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error deleting the custom disc " + ChatColor.GOLD + displayName + ChatColor.GRAY + ".");
                return false;
            }

            // Upload the server resource pack
            if (uploadResourcePack()) {
                player.sendMessage(ChatColor.GRAY + "Server resource pack uploaded.");
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error updating the server resource pack after deletion.");
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean deleteFileFromResourcePack(String filePath) {
        File tempZipFile = new File(resourcePackFile.getAbsolutePath() + ".tmp");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempZipFile));
             ZipInputStream zis = new ZipInputStream(new FileInputStream(resourcePackFile))) {

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
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (resourcePackFile.delete() && tempZipFile.renameTo(resourcePackFile)) {
            getLogger().info("Deleted " + filePath + " from resource pack.");
            return true;
        } else {
            getLogger().severe("Error removing " + filePath + " from resource pack.");
            return false;
        }
    }
}
