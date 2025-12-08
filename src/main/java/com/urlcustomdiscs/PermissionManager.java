package com.urlcustomdiscs;

import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class PermissionManager {
    private final URLCustomDiscs plugin;
    private final File controlFile;
    private JSONObject controlData;

    public PermissionManager(URLCustomDiscs plugin) {
        this.plugin = plugin;
        this.controlFile = new File(plugin.getDataFolder(), "control.json");
        loadControlData();
    }

    private void loadControlData() {
        if (controlFile.exists()) {
            try {
                String content = Files.readString(controlFile.toPath());
                controlData = new JSONObject(content);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to load control.json: " + e.getMessage());
                controlData = getDefaultControlData();
            }
        } else {
            controlData = getDefaultControlData();
            saveControlData();
        }
    }

    private JSONObject getDefaultControlData() {
        JSONObject data = new JSONObject();
        data.put("creationEnabled", true);
        data.put("maxDiscsPerUser", -1);
        return data;
    }

    private void saveControlData() {
        try {
            Files.writeString(controlFile.toPath(), controlData.toString(4));
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save control.json: " + e.getMessage());
        }
    }

    public boolean isOp(Player p) {
        return p.isOp();
    }

    public boolean canCreate(Player p) {
        if (isOp(p)) return true;
        
        if (!controlData.optBoolean("creationEnabled", true)) {
            return false;
        }

        int maxDiscs = controlData.optInt("maxDiscsPerUser", -1);
        if (maxDiscs == -1) return true;

        int userDiscCount = getUserDiscCount(p.getName());
        return userDiscCount < maxDiscs;
    }

    public boolean canUse(Player p, JSONObject disc) {
        if (isOp(p)) return true;
        if (isOwner(p, disc)) return true;
        return hasSharedPermission(p.getName(), disc, "use");
    }

    public boolean canGive(Player p, JSONObject disc) {
        if (isOp(p)) return true;
        if (isOwner(p, disc)) return true;
        return hasSharedPermission(p.getName(), disc, "give");
    }

    public boolean canDelete(Player p, JSONObject disc) {
        if (isOp(p)) return true;
        if (isOwner(p, disc)) return true;
        return hasSharedPermission(p.getName(), disc, "delete");
    }

    public boolean canManage(Player p, JSONObject disc) {
        return isOp(p) || isOwner(p, disc);
    }

    private boolean isOwner(Player p, JSONObject disc) {
        String owner = disc.optString("owner", "");
        return owner.equals(p.getName());
    }

    private boolean hasSharedPermission(String playerName, JSONObject disc, String permission) {
        JSONObject shared = disc.optJSONObject("shared");
        if (shared == null) return false;
        
        if (!shared.has(playerName)) return false;
        
        Object perms = shared.get(playerName);
        if (perms instanceof org.json.JSONArray) {
            org.json.JSONArray permArray = (org.json.JSONArray) perms;
            for (int i = 0; i < permArray.length(); i++) {
                String perm = permArray.getString(i);
                if (perm.equals(permission) || perm.equals("all")) {
                    return true;
                }
            }
        }
        return false;
    }

    private int getUserDiscCount(String playerName) {
        File discUuidFile = new File(plugin.getDataFolder(), "discs.json");
        if (!discUuidFile.exists()) return 0;

        try {
            String content = Files.readString(discUuidFile.toPath());
            JSONObject discData = new JSONObject(content);
            int count = 0;
            
            for (String key : discData.keySet()) {
                JSONObject disc = discData.getJSONObject(key);
                if (playerName.equals(disc.optString("owner", ""))) {
                    count++;
                }
            }
            return count;
        } catch (IOException e) {
            return 0;
        }
    }

    public void shareDisc(JSONObject disc, String targetPlayer, String[] permissions) {
        JSONObject shared = disc.optJSONObject("shared");
        if (shared == null) {
            shared = new JSONObject();
            disc.put("shared", shared);
        }

        org.json.JSONArray permArray = new org.json.JSONArray();
        for (String perm : permissions) {
            permArray.put(perm);
        }
        shared.put(targetPlayer, permArray);
    }

    public void unshareDisc(JSONObject disc, String targetPlayer) {
        JSONObject shared = disc.optJSONObject("shared");
        if (shared != null && shared.has(targetPlayer)) {
            shared.remove(targetPlayer);
        }
    }

    public void setCreationEnabled(boolean enabled) {
        controlData.put("creationEnabled", enabled);
        saveControlData();
    }

    public void setMaxDiscsPerUser(int max) {
        controlData.put("maxDiscsPerUser", max);
        saveControlData();
    }

    public boolean isCreationEnabled() {
        return controlData.optBoolean("creationEnabled", true);
    }

    public int getMaxDiscsPerUser() {
        return controlData.optInt("maxDiscsPerUser", -1);
    }

    public void resetDiscOwnership(JSONObject disc) {
        disc.remove("owner");
        disc.remove("shared");
    }

    public void transferOwnership(JSONObject disc, String newOwner) {
        disc.put("owner", newOwner);
        disc.put("shared", new JSONObject());
    }

    public String getCreationBlockedReason(Player p) {
        if (!controlData.optBoolean("creationEnabled", true)) {
            return "Disc creation disabled.";
        }

        int maxDiscs = controlData.optInt("maxDiscsPerUser", -1);
        if (maxDiscs != -1) {
            int userDiscCount = getUserDiscCount(p.getName());
            if (userDiscCount >= maxDiscs) {
                return "Max reached.";
            }
        }

        return null;
    }
}
