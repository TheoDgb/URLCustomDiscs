package com.urlcustomdiscs;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class YtDlpManager {

    private final URLCustomDiscs plugin;
    private final File ytDlpFile;

    public YtDlpManager(URLCustomDiscs plugin, URLCustomDiscs.OS os) {
        this.plugin = plugin;

        File binDir = plugin.getBinFolder();
        String ytDlpName = (os == URLCustomDiscs.OS.WINDOWS) ? "yt-dlp.exe" : "yt-dlp";
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