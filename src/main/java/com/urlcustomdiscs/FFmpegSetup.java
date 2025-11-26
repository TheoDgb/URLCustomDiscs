package com.urlcustomdiscs;

import org.tukaani.xz.XZInputStream;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FFmpegSetup {

    private final URLCustomDiscs plugin;
    private final URLCustomDiscs.OS os;
    private final File binDir;
    private final File ffmpegDir;

    FFmpegSetup(URLCustomDiscs plugin, URLCustomDiscs.OS os) {
        this.plugin = plugin;
        this.os = os;
        this.binDir = new File(plugin.getDataFolder(), "bin");
        this.ffmpegDir = new File(binDir, "FFmpeg");
    }

    public void setup() {
        try {
            File ffmpegFile;
            if (ffmpegDir.exists()) {
                ffmpegFile = detectExecutable(ffmpegDir);
                if (ffmpegFile != null && ffmpegFile.exists()) {
                    plugin.getLogger().info("[SETUP] FFmpeg is already installed.");
                    return;
                } else {
                    plugin.getLogger().warning("[SETUP] FFmpeg folder exists but executable is missing, re-downloading...");
                }
            }

            plugin.getLogger().info("[SETUP] Downloading FFmpeg...");
            String downloadUrl = switch (os) {
                case WINDOWS_X64, WINDOWS_ARM64 ->
                        "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl-shared.zip";
                case LINUX_X64, LINUX_MUSL_X64 ->
                        "https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-amd64-static.tar.xz";
                case LINUX_ARM64, LINUX_MUSL_ARM64 ->
                        "https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-arm64-static.tar.xz";
                case LINUX_ARMV7 ->
                        "https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-armhf-static.tar.xz";
                default ->
                        throw new IllegalStateException("Unsupported OS: " + os);
            };

            File archive;
            if (os == URLCustomDiscs.OS.WINDOWS_X64 || os == URLCustomDiscs.OS.WINDOWS_ARM64) {
                archive = new File(binDir, "ffmpeg.zip");
            } else {
                archive = new File(binDir, "ffmpeg.tar.xz");
            }
            downloadFile(downloadUrl, archive);


            // Extract archive
            switch (os) {
                case WINDOWS_X64, WINDOWS_ARM64 -> {
                    unzip(archive, ffmpegDir);
                    ffmpegFile = findFile(ffmpegDir, "ffmpeg.exe");
                }
                case LINUX_X64, LINUX_ARM64, LINUX_ARMV7, LINUX_MUSL_X64, LINUX_MUSL_ARM64 -> {
                    untarXz(archive, ffmpegDir);
                    ffmpegFile = findFile(ffmpegDir, "ffmpeg");
                }
                default -> throw new IllegalStateException("Unsupported OS for extraction: " + os);
            }

            // Delete archive
            if (archive.exists() && archive.delete()) {
                plugin.getLogger().info("[SETUP] Deleted archive: " + archive.getName());
            } else {
                plugin.getLogger().warning("[SETUP] Could not delete archive: " + archive.getName());
            }

            if (ffmpegFile == null || !ffmpegFile.exists()) {
                throw new IOException("FFmpeg executable not found after extraction!");
            }

            if (!ffmpegFile.setExecutable(true)) {
                plugin.getLogger().warning("[SETUP] Could not set FFmpeg as executable (usually fine on Windows).");
            }

            plugin.getLogger().info("[SETUP] FFmpeg is installed");

        } catch (Exception e) {
            plugin.getLogger().severe("Exception: " + e.getMessage());
            plugin.getLogger().warning("[SETUP] Failed to setup FFmpeg: " + e.getMessage());
        }
    }

    private void downloadFile(String urlString, File destination) throws IOException {
        plugin.getLogger().info("[SETUP] Downloading " + urlString);
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);

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

    private void unzip(File zipFile, File targetDir) throws IOException {
        targetDir.mkdirs();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // Delete the first folder level
                String entryName = entry.getName();
                String[] pathParts = entryName.split("/", 2);
                if (pathParts.length < 2) continue; // Skip if no subfolder
                String relativePath = pathParts[1]; // Take everything after the first "/"
                File newFile = new File(targetDir, relativePath);
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        zis.transferTo(fos);
                    }
                }
            }
        }
    }

    private void untarXz(File tarXzFile, File targetDir) throws IOException {
        targetDir.mkdirs();

        try (FileInputStream fis = new FileInputStream(tarXzFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             XZInputStream xzis = new XZInputStream(bis)) {

            extractTar(xzis, targetDir);
        }
    }

    private void extractTar(InputStream tarStream, File targetDir) throws IOException {
        byte[] buffer = new byte[1024];

        while (true) {
            // Read tar header (512 bytes)
            byte[] header = new byte[512];
            int bytesRead = readFully(tarStream, header);
            if (bytesRead < 512) break;

            // Check if we've reached the end (two consecutive zero blocks)
            if (isZeroBlock(header)) break;

            // Parse header
            String fileName = parseString(header, 0, 100).trim();
            if (fileName.isEmpty()) break;

            String fileMode = parseString(header, 100, 8).trim();
            String fileSize = parseString(header, 124, 12).trim();
            String fileType = parseString(header, 156, 1);

            if (fileName.equals("././@PaxHeader") || fileName.startsWith("PaxHeader/")) {
                // Skip PAX headers - just read and discard the content
                long size = parseOctal(fileSize);
                if (size > 0) {
                    skipBytes(tarStream, size);
                    // Skip padding to 512-byte boundary
                    long padding = (512 - (size % 512)) % 512;
                    skipBytes(tarStream, padding);
                }
                continue;
            }

            long size = parseOctal(fileSize);

            // Delete the first folder level
            String[] pathParts = fileName.split("/", 2);
            if (pathParts.length < 2) continue; // Skip if no subfolder
            String relativePath = pathParts[1]; // Take everything after the first "/"
            File newFile = new File(targetDir, relativePath);

            // Handle directories
            if (fileType.equals("5") || fileName.endsWith("/")) {
                newFile.mkdirs();
                continue;
            }

            // Handle regular files
            if (fileType.equals("0") || fileType.isEmpty()) {
                // Create parent directories
                File parentDir = newFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                // Extract file content
                try (FileOutputStream fos = new FileOutputStream(newFile);
                     BufferedOutputStream bos = new BufferedOutputStream(fos)) {

                    long remaining = size;
                    while (remaining > 0) {
                        int toRead = (int) Math.min(buffer.length, remaining);
                        int read = tarStream.read(buffer, 0, toRead);
                        if (read <= 0) break;

                        bos.write(buffer, 0, read);
                        remaining -= read;
                    }
                }

                // Set file permissions if possible
                if (!fileMode.isEmpty()) {
                    try {
                        int mode = Integer.parseInt(fileMode, 8);
                        newFile.setExecutable((mode & 0111) != 0);
                        newFile.setReadable((mode & 0444) != 0);
                        newFile.setWritable((mode & 0222) != 0);
                    } catch (NumberFormatException ignored) {}
                }
            }

            // Skip padding to 512-byte boundary
            long padding = (512 - (size % 512)) % 512;
            skipBytes(tarStream, padding);
        }
    }

    private int readFully(InputStream is, byte[] buffer) throws IOException {
        int totalRead = 0;
        int remaining = buffer.length;

        while (remaining > 0) {
            int read = is.read(buffer, totalRead, remaining);
            if (read <= 0) break;
            totalRead += read;
            remaining -= read;
        }

        return totalRead;
    }

    private boolean isZeroBlock(byte[] block) {
        for (byte b : block) {
            if (b != 0) return false;
        }
        return true;
    }

    private String parseString(byte[] buffer, int offset, int length) {
        int end = offset;
        for (int i = offset; i < offset + length && i < buffer.length; i++) {
            if (buffer[i] == 0) break;
            end = i + 1;
        }
        return new String(buffer, offset, end - offset);
    }

    private long parseOctal(String octalString) {
        if (octalString == null || octalString.trim().isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(octalString.trim(), 8);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void skipBytes(InputStream is, long bytesToSkip) throws IOException {
        long remaining = bytesToSkip;
        byte[] skipBuffer = new byte[8192];

        while (remaining > 0) {
            int toRead = (int) Math.min(skipBuffer.length, remaining);
            int read = is.read(skipBuffer, 0, toRead);
            if (read <= 0) break;
            remaining -= read;
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

    private File detectExecutable(File ffmpegDir) throws IOException {
        return switch (os) {
            case WINDOWS_X64, WINDOWS_ARM64 -> findFile(ffmpegDir, "ffmpeg.exe");
            case LINUX_X64, LINUX_ARM64, LINUX_ARMV7, LINUX_MUSL_X64, LINUX_MUSL_ARM64 -> findFile(ffmpegDir, "ffmpeg");
            default -> null;
        };
    }
}