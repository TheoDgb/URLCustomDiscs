package com.urlcustomdiscs;

import com.urlcustomdiscs.utils.MinecraftServerVersionUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ResourcePackManager {
    private final URLCustomDiscs plugin;

    public ResourcePackManager(URLCustomDiscs plugin) {
        this.plugin = plugin;
    }

    // Download the resource pack from the URL
    public File downloadResourcePack(File outputDir, String downloadResourcePackUrl) throws IOException {
        plugin.getLogger().info("Downloading the resource pack...");

        File destinationFile = new File(outputDir, "URLCustomDiscsPack.zip");

        HttpURLConnection connection = (HttpURLConnection) new URL(downloadResourcePackUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to download the resource pack: " + connection.getResponseMessage());
        }

        try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
             FileOutputStream fileOutputStream = new FileOutputStream(destinationFile)) {

            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        }

        plugin.getLogger().info("Resource pack downloaded: " + destinationFile.getAbsolutePath());
        return destinationFile;
    }

    // Upload the resource pack to the HTTP server
    public boolean uploadResourcePack(File resourcePackFile, String uploadResourcePackURL) {
        plugin.getLogger().info("Uploading the resource pack...");

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(uploadResourcePackURL).openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");

            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream outputStream = connection.getOutputStream();
                 FileInputStream fileInputStream = new FileInputStream(resourcePackFile);
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true)) {

                // write multipart form data headers
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                        .append(resourcePackFile.getName()).append("\"\r\n");
                writer.append("Content-Type: application/zip\r\n\r\n");
                writer.flush();

                // Write file content
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();

                // Close boundary
                writer.append("\r\n--").append(boundary).append("--\r\n");
                writer.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                plugin.getLogger().info("Resource pack uploaded successfully.");
                return true;
            } else {
                plugin.getLogger().severe("Error uploading the resource pack: " + connection.getResponseMessage());
                return false;
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Exception while uploading resource pack: " + e.getMessage());
            return false;
        }
    }

    // Unzip the resource pack in the temp_unpacked folder
    public void unzipResourcePack(File zipFile, File outputDir) {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(outputDir, entry.getName());
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    newFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        zis.transferTo(fos);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Archive the contents of the unpacked folder into a temporary ZIP file, then replace the existing resource pack ZIP with the temporary ZIP file.
    public void rezipResourcePack(File sourceDir, File targetZip) throws IOException {
        Path sourcePath = sourceDir.toPath();

        // Create a temporary file in the same directory as the target file
        File tempZip = new File(targetZip.getParent(), targetZip.getName() + ".tmp");

        // Write to the temporary file
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempZip))) {
            Files.walk(sourcePath).forEach(path -> {
                File file = path.toFile();
                String entryName = sourcePath.relativize(path).toString().replace("\\", "/");
                if (file.isDirectory()) { // Add directories recursively
                    if (!entryName.isEmpty()) {
                        try {
                            zos.putNextEntry(new ZipEntry(entryName + "/"));
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                } else { // Add files
                    // Copy the file content into the zip file
                    try (FileInputStream fis = new FileInputStream(file)) {
                        zos.putNextEntry(new ZipEntry(entryName));
                        fis.transferTo(zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });
        }

        // Replace the old file with the new one
        try {
            // Delete the old file
            if (targetZip.exists() && !targetZip.delete()) {
                throw new IOException("Unable to delete the old ZIP file: " + targetZip.getAbsolutePath());
            }

            // Rename the temporary file
            if (!tempZip.renameTo(targetZip)) {
                throw new IOException("Unable to rename the temporary file: " + tempZip.getAbsolutePath() + " to " + targetZip.getAbsolutePath());
            }

        } catch (IOException e) {
            // Clean up the temporary file if an error occurs
            if (tempZip.exists()) {
                tempZip.delete();
            }
            throw e;
        }
    }

    // Add the Ogg file in the resource pack
    public void addOggFileToResourcePack(File oggFile, File unpackedDir, String discName) throws IOException {
        File target = new File(unpackedDir, "assets/minecraft/sounds/custom/" + discName + ".ogg");
        Files.copy(oggFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    // Add a custom disc entry to sounds.json
    public void addDiscEntryToSoundsJson(File unpackedDir, String discName) throws IOException {
        File soundsJson = new File(unpackedDir, "assets/minecraft/sounds.json");
        JSONObject sounds = new JSONObject();

        if (soundsJson.exists()) {
            String content = Files.readString(soundsJson.toPath());
            sounds = new JSONObject(content);
        }

        String key = "customdisc." + discName;

        if(!sounds.has(key)) {
            JSONObject soundEntry = new JSONObject();
            soundEntry.put("category", "record");

            JSONArray soundsArray = new JSONArray();
            JSONObject soundObj = new JSONObject();
            soundObj.put("name", "custom/" + discName);
            soundObj.put("stream", true);
            soundsArray.put(soundObj);

            soundEntry.put("sounds", soundsArray);

            sounds.put(key, soundEntry);
            Files.writeString(soundsJson.toPath(), sounds.toString(2));
        }
    }

    // Add a custom disc entry to music_disc_13.json
    public void addDiscEntryToMusicDisc13ModelJson(File unpackedDir, String discName, int customModelData, String minecraftServerVersion) throws IOException {

        MinecraftServerVersionUtils version = MinecraftServerVersionUtils.parse(minecraftServerVersion);
        File modelPath = version.isNewFormat()
                ? new File(unpackedDir, "assets/minecraft/items/music_disc_13.json")
                : new File(unpackedDir, "assets/minecraft/models/item/music_disc_13.json");

        JSONObject root;
        if (modelPath.exists()) {
            String content = Files.readString(modelPath.toPath());
            root = new JSONObject(content);
        } else {
            root = new JSONObject();
        }

        if (version.isNewFormat()) { // New format with "entries" (1.21.4+)
            // Retrieve or create the "model" object
            JSONObject modelObject = root.optJSONObject("model");
            if (modelObject == null) {
                modelObject = new JSONObject();
                modelObject.put("type", "range_dispatch");
                modelObject.put("property", "custom_model_data");

                JSONObject fallback = new JSONObject();
                fallback.put("type", "model");
                fallback.put("model", "minecraft:item/music_disc_13");
                modelObject.put("fallback", fallback);

                modelObject.put("entries", new JSONArray());
                root.put("model", modelObject);
            }

            JSONArray entries = modelObject.getJSONArray("entries");

            // Checks if the customModelData already exists
            boolean alreadyExists = false;
            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = entries.getJSONObject(i);
                if (entry.optInt("threshold") == customModelData) {
                    alreadyExists = true;
                    break;
                }
            }

            // Add a new entry if necessary
            if (!alreadyExists) {
                JSONObject newEntry = new JSONObject();
                newEntry.put("threshold", customModelData);

                JSONObject entryModel = new JSONObject();
                entryModel.put("type", "model");
                entryModel.put("model", "item/custom_music_disc_" + discName);

                newEntry.put("model", entryModel);
                entries.put(newEntry);
            }

        } else { // Old format with "overrides" (1.21)
            if (!modelPath.exists() || !root.has("parent")) {
                root.put("parent", "minecraft:item/generated");

                JSONObject textures = new JSONObject();
                textures.put("layer0", "minecraft:item/music_disc_13");
                root.put("textures", textures);
            }
            JSONArray overrides = root.optJSONArray("overrides");
            if (overrides == null) {
                overrides = new JSONArray();
                root.put("overrides", overrides);
            }

            boolean alreadyExists = false;
            for (int i = 0; i < overrides.length(); i++) {
                JSONObject override = overrides.getJSONObject(i);
                JSONObject predicate = override.optJSONObject("predicate");
                if (predicate != null && predicate.optInt("custom_model_data") == customModelData) {
                    alreadyExists = true;
                    break;
                }
            }

            if (!alreadyExists) {
                JSONObject newOverride = new JSONObject();
                JSONObject predicate = new JSONObject();
                predicate.put("custom_model_data", customModelData);

                newOverride.put("predicate", predicate);
                newOverride.put("model", "item/custom_music_disc_" + discName);

                overrides.put(newOverride);
            }
        }

        Files.createDirectories(modelPath.toPath().getParent());
        Files.writeString(modelPath.toPath(), root.toString(2));
    }

    // Create the JSON model for the custom disc
    public void createCustomDiscModelJson(File unpackedDir, String discName, String minecraftServerVersion) throws IOException {

        MinecraftServerVersionUtils version = MinecraftServerVersionUtils.parse(minecraftServerVersion);

        File modelPath = new File(unpackedDir, "assets/minecraft/models/item/custom_music_disc_" + discName + ".json");

        JSONObject model = new JSONObject();
        model.put("parent", "minecraft:item/generated");

        JSONObject textures = new JSONObject();
        textures.put("layer0", version.isNewFormat()
                ? "item/record_custom"
                : "minecraft:item/record_custom");

        model.put("textures", textures);

        modelPath.getParentFile().mkdirs();
        Files.writeString(modelPath.toPath(), model.toString(2));
    }

    // Remove Ogg file from the resource pack
    public void removeOggFileFromResourcePack(File unpackedDir, String discName) throws IOException {
        File oggFilePath = new File(unpackedDir, "assets/minecraft/sounds/custom/" + discName + ".ogg");
        if (oggFilePath.exists()) oggFilePath.delete();
    }

    // Remove a custom disc entry from sounds.json
    public void removeDiscEntryFromSoundsJson(File unpackedDir, String discName) throws IOException {
        File soundsJson = new File(unpackedDir, "assets/minecraft/sounds.json");
        String key = "customdisc." + discName;

        if (!soundsJson.exists()) return;

        // Read the sounds.json file
        String content = Files.readString(soundsJson.toPath());
        JSONObject sounds = new JSONObject(content);

        // Remove the entry if it exists
        if (sounds.has(key)) {
            sounds.remove(key);

            Files.writeString(soundsJson.toPath(), sounds.toString(2));
        }
    }

    // Remove a custom disc entry to music_disc_13.json
    public void removeDiscEntryToMusicDisc13ModelJson(File unpackedDir, int customModelData, String minecraftServerVersion) throws IOException {

        MinecraftServerVersionUtils version = MinecraftServerVersionUtils.parse(minecraftServerVersion);
        File modelPath = version.isNewFormat()
                ? new File(unpackedDir, "assets/minecraft/items/music_disc_13.json")
                : new File(unpackedDir, "assets/minecraft/models/item/music_disc_13.json");

        if (!modelPath.exists()) return;

        String content = Files.readString(modelPath.toPath());
        JSONObject root = new JSONObject(content);

        // Remove the entry if it exists
        if (version.isNewFormat()) {
            // The entries are in "model.entries"
            JSONObject modelObject = root.optJSONObject("model");
            if (modelObject != null) {
                JSONArray entries = modelObject.optJSONArray("entries");
                if (entries != null) {
                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject entry = entries.getJSONObject(i);
                        if (entry.optInt("threshold") == customModelData) {
                            entries.remove(i); // Remove the matching entry
                            break;
                        }
                    }
                }
            }
        } else {
            JSONArray overrides = root.optJSONArray("overrides");
            if (overrides != null) {
                for (int i = 0; i < overrides.length(); i++) {
                    JSONObject override = overrides.getJSONObject(i);
                    JSONObject predicate = override.optJSONObject("predicate");
                    if (predicate != null && predicate.optInt("custom_model_data") == customModelData) {
                        overrides.remove(i); // Remove the matching override
                        break;
                    }
                }
            }
        }

        Files.writeString(modelPath.toPath(), root.toString(2));
    }

    // Delete the JSON model from the custom disc
    public void deleteCustomDiscModelJson(File unpackedDir, String discName) throws IOException {
        File modelPath = new File(unpackedDir, "assets/minecraft/models/item/custom_music_disc_" + discName + ".json");

        if (modelPath.exists()) {
            if (!modelPath.delete()) {
                throw new IOException("Failed to delete the custom disc model JSON: " + modelPath.getAbsolutePath());
            }
        }
    }
}