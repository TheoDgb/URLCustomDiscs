package com.urlcustomdiscs.utils;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class DiscUtils {
    // Charger les données du fichier discs.json
    public static JSONObject loadDiscData(File discUuidFile) {
        try {
            if (discUuidFile.exists()) {
                String content = Files.readString(discUuidFile.toPath());
                return new JSONObject(content);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load disc data: " + discUuidFile);
        }
        return new JSONObject(); // Retourne un objet vide en cas d'erreur
    }

    // Trouver le nom du disque à partir du CustomModelData
    public static String getDiscNameFromCustomModelData(JSONObject discData, int customModelData) {
        for (String key : discData.keySet()) {
            JSONObject disc = discData.getJSONObject(key);
            if (disc.has("customModelData") && disc.getInt("customModelData") == customModelData) {
                return key;
            }
        }
        return null; // Si aucun disque ne correspond, retourne null
    }
}