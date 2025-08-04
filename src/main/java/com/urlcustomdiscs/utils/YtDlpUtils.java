package com.urlcustomdiscs.utils;

import com.urlcustomdiscs.URLCustomDiscs;

import java.io.File;

public class YtDlpUtils {
    private final URLCustomDiscs plugin;

    public YtDlpUtils(URLCustomDiscs plugin) {
        this.plugin = plugin;
    }
    public boolean downloadAudioWithYtDlp(String url, File outputFile) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String ytDlpExecutable;

            if (os.contains("win")) {
                ytDlpExecutable = new File(plugin.getDataFolder(), "bin/yt-dlp.exe").getAbsolutePath();
            } else {
                ytDlpExecutable = new File(plugin.getDataFolder(), "bin/yt-dlp").getAbsolutePath();
            }

            ProcessBuilder pb = new ProcessBuilder(
                    ytDlpExecutable,
                    "-f", "bestaudio[ext=m4a]/best",
                    "--audio-format", "mp3",
                    "-o", outputFile.getAbsolutePath(),
                    url
            );
            Process process = pb.start();
            int exitCode = process.waitFor();

            return exitCode == 0 && outputFile.exists();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
