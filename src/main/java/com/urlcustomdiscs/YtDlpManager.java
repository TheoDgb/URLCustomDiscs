package com.urlcustomdiscs;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class YtDlpManager {

    private final URLCustomDiscs plugin;
    private final File ytDlpFile;
    private final URLCustomDiscs.OS os;

    public YtDlpManager(URLCustomDiscs plugin, URLCustomDiscs.OS os) {
        this.plugin = plugin;
        this.os = os;
        File binDir = plugin.getBinFolder();

        String ytDlpName = switch (os) {
            case WINDOWS_X64 -> "yt-dlp.exe";
            case WINDOWS_ARM64 -> "yt-dlp_arm64.exe";
            case LINUX_X64 -> "yt-dlp_linux";
            case LINUX_ARM64 -> "yt-dlp_linux_aarch64";
            case LINUX_ARMV7 -> "yt-dlp_linux_armv7l";
            case LINUX_MUSL_X64 -> "yt-dlp_musllinux";
            case LINUX_MUSL_ARM64 -> "yt-dlp_musllinux_aarch64";
            default -> "yt-dlp"; // fallback
        };
        this.ytDlpFile = new File(binDir, ytDlpName);
    }

    public boolean downloadAudioWithYtDlp(String url, File outputFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ytDlpFile.getAbsolutePath(),
                    "--no-playlist",
                    "-f", "bestaudio[ext=m4a]/best",
                    "--audio-format", "mp3",
                    "-o", outputFile.getAbsolutePath(),
                    url
            );

            File denoFile = new File(plugin.getBinFolder(),
                    os == URLCustomDiscs.OS.WINDOWS_X64 || os == URLCustomDiscs.OS.WINDOWS_ARM64
                            ? "deno.exe"
                            : "deno"
            );
            pb.environment().put("DENO_PATH", denoFile.getAbsolutePath());

            pb.redirectErrorStream(true); // Merge stderr in stdout
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    plugin.getLogger().info("[yt-dlp] " + line);
                }
            }

            int exitCode = process.waitFor();
            return exitCode == 0 && outputFile.exists();

        } catch (Exception e) {
            plugin.getLogger().severe("[yt-dlp] Failed to run yt-dlp: " + e.getMessage());
            return false;
        }
    }
}