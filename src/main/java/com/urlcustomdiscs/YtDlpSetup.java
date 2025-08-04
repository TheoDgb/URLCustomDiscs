package com.urlcustomdiscs;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class YtDlpSetup {

    public enum OS {
        WINDOWS, LINUX, OTHER
    }

    private final JavaPlugin plugin;
    private final OS os;
    private final File binDir;
    private final File ytDlpFile;

    public YtDlpSetup(JavaPlugin plugin) {
        this.plugin = plugin;
        this.os = detectOperatingSystem();
        this.binDir = new File(plugin.getDataFolder(), "bin");

        String ytDlpName = (os == OS.WINDOWS) ? "yt-dlp.exe" : "yt-dlp";
        this.ytDlpFile = new File(binDir, ytDlpName);
    }

    private OS detectOperatingSystem() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) return OS.WINDOWS;
        if (osName.contains("nux") || osName.contains("nix")) return OS.LINUX;
        return OS.OTHER;
    }

    public void setup() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (!binDir.exists()) binDir.mkdirs();

                String localVersion = getLocalVersion();
                String latestVersion = fetchLatestVersion();

                plugin.getLogger().info("[SETUP] Local yt-dlp version: " + (localVersion == null ? "none" : localVersion));
                plugin.getLogger().info("[SETUP] Latest yt-dlp version: " + latestVersion);

                if (localVersion == null || !localVersion.equals(latestVersion)) {
                    plugin.getLogger().info("[SETUP] Updating yt-dlp...");
                    if (ytDlpFile.exists()) ytDlpFile.delete();

                    String downloadUrl = switch (os) {
                        case WINDOWS -> "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe";
                        case LINUX -> "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux";
                        default -> throw new IllegalStateException("Unsupported OS: " + os);
                    };

                    downloadFile(downloadUrl, ytDlpFile);

                    if (!ytDlpFile.setExecutable(true)) {
                        plugin.getLogger().warning("[SETUP] Could not set yt-dlp as executable (usually fine on Windows).");
                    }

                    plugin.getLogger().info("[SETUP] yt-dlp updated.");
                } else {
                    plugin.getLogger().info("[SETUP] yt-dlp is up-to-date.");
                }

            } catch (Exception e) {
                plugin.getLogger().warning("[SETUP] Failed to setup yt-dlp: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private String getLocalVersion() {
        if (!ytDlpFile.exists()) return null;
        try {
            ProcessBuilder pb = new ProcessBuilder(ytDlpFile.getAbsolutePath(), "--version");
            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String version = reader.readLine();
            p.waitFor();

            return version != null ? version.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String fetchLatestVersion() throws IOException {
        URL url = new URL("https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", "JavaPlugin");
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        if (connection.getResponseCode() != 200) {
            throw new IOException("Failed to fetch latest version, HTTP " + connection.getResponseCode());
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) json.append(line);

            String tagName = parseTagNameFromJson(json.toString());
            if (tagName == null) {
                throw new IOException("Invalid GitHub API response: tag_name missing");
            }
            return tagName.startsWith("v") ? tagName.substring(1) : tagName;
        }
    }

    private String parseTagNameFromJson(String json) {
        int idx = json.indexOf("\"tag_name\"");
        if (idx == -1) return null;
        int start = json.indexOf("\"", idx + 10);
        if (start == -1) return null;
        int end = json.indexOf("\"", start + 1);
        if (end == -1) return null;
        return json.substring(start + 1, end);
    }

    private void downloadFile(String urlString, File destination) throws IOException {
        plugin.getLogger().info("[SETUP] Downloading " + urlString);
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        if (connection.getResponseCode() != 200) {
            throw new IOException("Failed to download file: HTTP " + connection.getResponseCode());
        }

        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }
}
