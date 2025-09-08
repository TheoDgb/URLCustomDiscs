package com.urlcustomdiscs.utils;

public class MinecraftServerVersionUtils {
    private final int major;
    private final int minor;
    private final int patch;

    private MinecraftServerVersionUtils(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    // Parse "1.21", "1.21.4", 1.21.4-RO.1-SNAPSHOT", etc.
    public static MinecraftServerVersionUtils parse(String minecraftServerVersion) {
        int major = 0, minor = 0, patch = 0;
        if (minecraftServerVersion != null) {
            String core = minecraftServerVersion.split("[\\-+]")[0]; // remove suffixes like "-SNAPSHOT" or "+R0.1-SNAPSHOT"
            String[] parts = core.split("\\.");
            try { if (parts.length > 0) major = Integer.parseInt(parts[0]); } catch (NumberFormatException ignored) {}
            try { if (parts.length > 1) minor = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
            try { if (parts.length > 2) patch = Integer.parseInt(parts[2]); } catch (NumberFormatException ignored) {}
        }
        return new MinecraftServerVersionUtils(major, minor, patch);
    }

    // Check resource pack new format for version 1.21.4+
    public boolean isNewFormat() {
        if (major > 1) return true;     // 2.x or above
        if (major < 1) return false;    // 0.x
        if (minor > 21) return true;    // 1.22+
        if (minor < 21) return false;   // 1.20 or below
        return patch >= 4;              // 1.21.4 or above
    }

    // Check the new drop position for version 1.21.3+
    public boolean isNewDropPosition() {
        if (major > 1) return true;     // 2.x or above
        if (major < 1) return false;    // 0.x
        if (minor > 21) return true;    // 1.22+
        if (minor < 21) return false;   // 1.20 or below
        return patch >= 3;              // 1.21.3 or above
    }
}