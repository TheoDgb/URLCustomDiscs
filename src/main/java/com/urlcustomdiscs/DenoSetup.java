package com.urlcustomdiscs;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class DenoSetup {

    private final JavaPlugin plugin;
    private final URLCustomDiscs.OS os;
    private final File denoFile;

    public DenoSetup(JavaPlugin plugin, URLCustomDiscs.OS os) {
        this.plugin = plugin;
        this.os = os;
        File binDir = new File(plugin.getDataFolder(), "bin");

        String denoName = switch (os) {
            case WINDOWS_X64, WINDOWS_ARM64 -> "deno.exe";
            case LINUX_X64, LINUX_ARM64, LINUX_ARMV7, LINUX_MUSL_X64, LINUX_MUSL_ARM64 -> "deno";
            default -> throw new IllegalStateException("Unsupported OS: " + os);
        };
        this.denoFile = new File(binDir, denoName);
    }

    public void setup() {
        try {
            String localVersion = getLocalVersion();
            String latestVersion = fetchLatestVersion();

            plugin.getLogger().info("[SETUP] Local Deno version: " + (localVersion == null ? "none" : localVersion));
            plugin.getLogger().info("[SETUP] Latest Deno version: " + latestVersion);

            if (localVersion == null || !localVersion.equals(latestVersion)) {
                plugin.getLogger().info("[SETUP] Updating Deno...");
                if (denoFile.exists()) denoFile.delete();

                String downloadUrl = switch (os) {
                    case WINDOWS_X64, WINDOWS_ARM64 -> "https://github.com/denoland/deno/releases/latest/download/deno-x86_64-pc-windows-msvc.zip";
                    case LINUX_X64 -> "https://github.com/denoland/deno/releases/latest/download/deno-x86_64-unknown-linux-gnu.zip";
                    case LINUX_ARM64 -> "https://github.com/denoland/deno/releases/latest/download/deno-aarch64-unknown-linux-gnu.zip";
                    case LINUX_ARMV7 -> "https://github.com/denoland/deno/releases/latest/download/deno-armv7-unknown-linux-gnueabihf.zip";
                    case LINUX_MUSL_X64 -> "https://github.com/denoland/deno/releases/latest/download/deno-x86_64-unknown-linux-musl.zip";
                    case LINUX_MUSL_ARM64 -> "https://github.com/denoland/deno/releases/latest/download/deno-aarch64-unknown-linux-musl.zip";
                    default -> throw new IllegalStateException("Unsupported OS/architecture combination: " + os);
                };

                File zipFile = new File(denoFile.getParentFile(), denoFile.getName() + ".zip");
                downloadFile(downloadUrl, zipFile);
                unzipSingleFile(zipFile, denoFile.getName(), denoFile);

                if (!zipFile.delete()) {
                    plugin.getLogger().warning("[SETUP] Could not delete " + zipFile.getName());
                }

                if (!denoFile.setExecutable(true)) {
                    plugin.getLogger().warning("[SETUP] Could not set Deno as executable.");
                }

                plugin.getLogger().info("[SETUP] Deno updated.");
            } else {
                plugin.getLogger().info("[SETUP] Deno is up-to-date.");
            }

        } catch (Exception e) {
            plugin.getLogger().warning("[SETUP] Failed to setup Deno: " + e.getMessage());
        }
    }

    private String getLocalVersion() {
        if (!denoFile.exists()) return null;
        try {
            ProcessBuilder pb = new ProcessBuilder(denoFile.getAbsolutePath(), "--version");
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
        // Fetch latest version from GitHub API
        HttpURLConnection connection = openHttpConnection(
                "https://api.github.com/repos/denoland/deno/releases/latest",
                5000, 5000
        );

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) json.append(line);

            int idx = json.indexOf("\"tag_name\"");
            if (idx == -1) throw new IOException("Invalid GitHub API response: tag_name missing");
            int start = json.indexOf("\"", idx + 10);
            int end = json.indexOf("\"", start + 1);
            String tagName = json.substring(start + 1, end);
            return tagName.startsWith("v") ? tagName.substring(1) : tagName;
        }
    }

    private void downloadFile(String urlString, File destination) throws IOException {
        plugin.getLogger().info("[SETUP] Downloading " + urlString);

        HttpURLConnection connection = openHttpConnection(urlString, 15000, 15000);

        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    private void unzipSingleFile(File zipFile, String fileNameInZip, File targetFile) throws IOException {
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new FileInputStream(zipFile))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(fileNameInZip)) {
                    targetFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                        zis.transferTo(fos);
                    }
                    break;
                }
            }
        }
    }

    private static @NotNull HttpURLConnection openHttpConnection(String urlString, int connectTimeout, int readTimeout) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", "JavaPlugin");
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);

        if (connection.getResponseCode() != 200) {
            throw new IOException("Failed request to " + urlString + ", HTTP " + connection.getResponseCode());
        }

        return connection;
    }
}