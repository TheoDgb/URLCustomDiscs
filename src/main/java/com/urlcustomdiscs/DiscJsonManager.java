package com.urlcustomdiscs;

import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

public class DiscJsonManager {
    private final File discUuidFile;

    public DiscJsonManager(JavaPlugin plugin) {
        this.discUuidFile = new File(plugin.getDataFolder(), "discs.json");
    }

    public JSONObject getOrCreateDisc(String discName, String displayName) throws IOException {
        JSONObject discData = loadDiscData();

        if (discData.has(discName)) {
            return discData.getJSONObject(discName);
        }

        // Create new disc info
        String newUUID = UUID.randomUUID().toString();
        JSONObject discInfo = new JSONObject();
        discInfo.put("uuid", newUUID);

        int customModelData = (int) (Long.parseLong(newUUID.replace("-", "").substring(0, 8), 16) & 0x7FFFFFFF);
        discInfo.put("customModelData", customModelData);
        discInfo.put("displayName", displayName);

        discData.put(discName, discInfo);
        saveDiscData(discData);

        return discInfo;
    }

    public JSONObject getDisc(String discName) throws IOException {
        JSONObject discData = loadDiscData();
        return discData.optJSONObject(discName);
    }

    public void deleteDisc(String discName) throws IOException {
        JSONObject discData = loadDiscData();
        if (discData.has(discName)) {
            discData.remove(discName);
            saveDiscData(discData);
        }
    }

    private JSONObject loadDiscData() throws IOException {
        if (discUuidFile.exists()) {
            String content = Files.readString(discUuidFile.toPath());
            return new JSONObject(content);
        } else {
            return new JSONObject();
        }
    }

    private void saveDiscData(JSONObject discData) throws IOException {
        Files.writeString(discUuidFile.toPath(), discData.toString(4));
    }
}