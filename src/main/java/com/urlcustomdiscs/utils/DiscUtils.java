package com.urlcustomdiscs.utils;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.bukkit.Bukkit.getLogger;

public class DiscUtils {
    // Load data from the discs.json file
    public static JSONObject loadDiscData(File discUuidFile) {
        try {
            if (discUuidFile.exists()) {
                String content = Files.readString(discUuidFile.toPath());
                return new JSONObject(content);
            }
        } catch (IOException e) {
            getLogger().severe("Exception: " + e.getMessage());
            getLogger().severe("Failed to load disc data: " + discUuidFile);
        }
        return new JSONObject(); // Returns an empty object on error
    }

    // Find the disc name from the CustomModelData
    public static String getDiscNameFromCustomModelData(JSONObject discData, int customModelData) {
        for (String key : discData.keySet()) {
            JSONObject disc = discData.getJSONObject(key);
            if (disc.has("customModelData") && disc.getInt("customModelData") == customModelData) {
                return key;
            }
        }
        return null; // If no disc matches, returns null
    }
}