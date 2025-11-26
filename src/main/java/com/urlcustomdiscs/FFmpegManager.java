package com.urlcustomdiscs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FFmpegManager {

    private final URLCustomDiscs plugin;
    private final File ffmpegFile;

    public FFmpegManager(URLCustomDiscs plugin, URLCustomDiscs.OS os) {
        this.plugin = plugin;

        File binDir = plugin.getBinFolder();
        File ffmpegDir = new File(binDir, "FFmpeg");
        String ffmpegName = switch (os) {
            case WINDOWS_X64, WINDOWS_ARM64 -> "ffmpeg.exe";
            case LINUX_X64, LINUX_ARM64, LINUX_ARMV7, LINUX_MUSL_X64, LINUX_MUSL_ARM64 -> "ffmpeg";
            default -> null;
        };

        try {
            this.ffmpegFile = findFile(ffmpegDir, ffmpegName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean convertAudioWithFFmpeg(String inputFileMP3, File outputFileOggVorbis, String audioType) {
        File mp3File = new File(plugin.getTempAudioFolder(), inputFileMP3);
        try {
            List<String> command = new ArrayList<>();
            command.add(ffmpegFile.getAbsolutePath());
            command.add("-y"); // Overwrite existing files
            command.add("-i"); // Input file
            command.add(String.valueOf(mp3File)); // Input file
            command.add("-vn"); // No video
            if ("mono".equalsIgnoreCase(audioType)) {
                command.add("-ac"); // Channels
                command.add("1"); // Mono
            }
            command.add("-c:a"); // Audio codec
            command.add("libvorbis"); // Ogg Vorbis codec for Minecraft compatibility
            command.add(outputFileOggVorbis.getAbsolutePath()); // Output file

            ProcessBuilder pb = new ProcessBuilder(command);

            pb.redirectErrorStream(true); // Merge stderr in stdout
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    plugin.getLogger().info("[FFmpeg] " + line);
                }
            }

            int exitCode = process.waitFor();
            return exitCode == 0 && outputFileOggVorbis.exists();

        } catch (Exception e) {
            plugin.getLogger().severe("Exception: " + e.getMessage());
            plugin.getLogger().severe("[FFmpeg] Failed to run FFmpeg: " + e.getMessage());
            return false;
        }
    }

    private File findFile(File dir, String name) throws IOException {
        try (Stream<Path> files = Files.walk(dir.toPath())) {
            return files
                    .filter(p -> p.toFile().isFile()) // Only look at files
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase(name))
                    .map(Path::toFile)
                    .findFirst()
                    .orElse(null);
        }
    }
}