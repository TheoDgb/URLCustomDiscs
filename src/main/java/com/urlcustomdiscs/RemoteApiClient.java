package com.urlcustomdiscs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class RemoteApiClient {

    private final URLCustomDiscs plugin;
    private final String apiBaseURL;

    public RemoteApiClient(URLCustomDiscs plugin, String apiBaseURL) {
        this.plugin = plugin;
        this.apiBaseURL = apiBaseURL;
    }

    public void requestTokenFromRemoteServer(Player player, Runnable onSuccess) {
        player.sendMessage(ChatColor.GRAY + "Registering the server with the remote API...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection connection = createPostConnection("/register-mc-server");

                String jsonInputString = "{}";
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (InputStream responseStream = connection.getInputStream()) {
                        String response = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
                        String receivedToken = parseValueFromJson(response, "token");
                        String receivedDownloadPackURL = parseValueFromJson(response, "downloadPackUrl");

                        if (receivedToken != null && !receivedToken.isEmpty() &&
                                receivedDownloadPackURL != null && !receivedDownloadPackURL.isEmpty()) {
                            File configFile = new File(plugin.getDataFolder(), "config.yml");
                            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                            config.set("token", receivedToken);
                            config.set("downloadPackURL", receivedDownloadPackURL);
                            config.save(configFile);

                            plugin.setToken(receivedToken);
                            plugin.setDownloadPackURL(receivedDownloadPackURL);

                            player.sendMessage(ChatColor.GREEN + "Your token has been generated and downloadPackURL is available in the config.yml file of the URLCustomDiscs plugin.");
                            player.sendMessage(ChatColor.YELLOW + "Don't forget to set the 'resource-pack=' field in your Minecraft server's 'server.properties' file using the downloadPackURL, as explained in the 'config.yml' file of the URLCustomDiscs plugin.");
                            Bukkit.getScheduler().runTask(plugin, onSuccess);
                        } else {
                            plugin.getLogger().warning("No token received from remote API.");
                        }
                    }
                } else {
                    plugin.getLogger().warning("Remote API returned status code: " + responseCode);
                }

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to register with remote API:");
                e.printStackTrace();
            }
        });
    }

    private String parseValueFromJson(String json, String key) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            return jsonObject.getString(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void createCustomDiscRemotely(Player player, String finalAudioIdentifier, String discName, String audioType, JSONObject discInfo, String token) {
        player.sendMessage(ChatColor.GRAY + "Sending information to the remote API...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean isLocalFile = !finalAudioIdentifier.toLowerCase().startsWith("http");
            HttpURLConnection connection;

            try {
                if (isLocalFile) {
                    File mp3File = new File(plugin.getDataFolder(), "audio_to_send/" + finalAudioIdentifier);
                    if (!mp3File.exists()) {
                        Bukkit.getScheduler().runTask(plugin, () ->
                                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "File not found: " + finalAudioIdentifier));
                        return;
                    }

                    String boundary = Long.toHexString(System.currentTimeMillis());
                    String CRLF = "\r\n";

                    connection = createPostConnection("/create-custom-disc-from-mp3");
                    connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                    connection.setDoOutput(true);

                    try (
                            OutputStream output = connection.getOutputStream();
                            PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), true)
                    ) {
                        // Text fields
                        writer.append("--").append(boundary).append(CRLF);
                        writer.append("Content-Disposition: form-data; name=\"discName\"").append(CRLF).append(CRLF).append(discName).append(CRLF);

                        writer.append("--").append(boundary).append(CRLF);
                        writer.append("Content-Disposition: form-data; name=\"audioType\"").append(CRLF).append(CRLF).append(audioType).append(CRLF);

                        writer.append("--").append(boundary).append(CRLF);
                        writer.append("Content-Disposition: form-data; name=\"customModelData\"").append(CRLF).append(CRLF)
                                .append(String.valueOf(discInfo.getInt("customModelData"))).append(CRLF);

                        writer.append("--").append(boundary).append(CRLF);
                        writer.append("Content-Disposition: form-data; name=\"token\"").append(CRLF).append(CRLF).append(token).append(CRLF);

                        // File field
                        writer.append("--").append(boundary).append(CRLF);
                        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(mp3File.getName()).append("\"").append(CRLF);
                        writer.append("Content-Type: audio/mpeg").append(CRLF).append(CRLF);
                        writer.flush();

                        Files.copy(mp3File.toPath(), output);
                        output.flush();
                        writer.append(CRLF).flush();

                        writer.append("--").append(boundary).append("--").append(CRLF).flush();
                    }
                } else {
                    // Send JSON with URL
                    connection = createPostConnection("/create-custom-disc");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);

                    JSONObject payload = new JSONObject();
                    payload.put("url", finalAudioIdentifier);
                    payload.put("discName", discName);
                    payload.put("audioType", audioType);
                    payload.put("customModelData", discInfo.getInt("customModelData"));
                    payload.put("token", token);

                    try (OutputStream os = connection.getOutputStream()) {
                        byte[] inputBytes = payload.toString().getBytes(StandardCharsets.UTF_8);
                        os.write(inputBytes, 0, inputBytes.length);
                    }
                }

                // Handle response
                int responseCode = connection.getResponseCode();
                InputStream responseStream = (responseCode >= 200 && responseCode < 300)
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                String response = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);

                Bukkit.getScheduler().runTask(plugin, () ->
                        handleApiResponse(player, responseCode, response, discInfo, discName, "create")
                );

            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "An error occurred while sending the request.")
                );
            }
        });
    }

    public void deleteCustomDiscRemotely(Player player, String discName, JSONObject discInfo, String token) {
        player.sendMessage(ChatColor.GRAY + "Sending information to the remote API...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection connection = createPostConnection("/delete-custom-disc");

                JSONObject payload = new JSONObject();
                payload.put("discName", discName);
                payload.put("token", token);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                String responseBody;

                try (InputStream responseStream = (responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream())) {
                    responseBody = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
                }

                final String finalResponseBody = responseBody;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    handleApiResponse(player, responseCode, finalResponseBody, discInfo, discName, "delete");
                });

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.RED + "An error occurred while contacting the remote API.");
                    plugin.getLogger().severe("Exception during remote disc deletion:");
                });
                e.printStackTrace();
            }
        });
    }

    private HttpURLConnection createPostConnection(String endpoint) throws Exception {
        URL url = new URL(apiBaseURL + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        return connection;
    }

    private void handleApiResponse(Player player, int responseCode, String responseBody, JSONObject discInfo, String discName, String mode) {
        if (responseCode >= 200 && responseCode < 300) {  // HTTP success range
            try {
                JSONObject json = new JSONObject(responseBody);
                if (json.has("success") && json.getBoolean("success")) {
                    // Functional success (the API operation completed successfully)
                    String message = json.optString("message", "Operation completed successfully.");

                    String displayName = discInfo.optString("displayName", "unknown");

                    if (mode.equals("create")) {
                        player.sendMessage(ChatColor.GREEN + "Custom disc " + ChatColor.GOLD + displayName + ChatColor.GREEN + " created.");
                        plugin.getLogger().info("[API SUCCESS] " + message);

                        // Generate the custom disc in memory and add it to the player's inventory
                        DiscFactory.giveCustomDiscToPlayer(player, discInfo);

                        // Browse all online players and send them the resource pack
                        String downloadPackURL = plugin.getDownloadPackURL();
                        if (!downloadPackURL.isEmpty()) {
                            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                                onlinePlayer.sendMessage(ChatColor.GRAY + "A new resource pack will be loaded...");
                                onlinePlayer.setResourcePack(downloadPackURL);
                            }
                        }
                    } else if (mode.equals("delete")) {
                        cleanupDiscEntry(discName);

                        player.sendMessage(ChatColor.GREEN + "Custom disc " + ChatColor.GOLD + displayName + ChatColor.GREEN + " deleted.");
                        plugin.getLogger().info("[API SUCCESS] " + message);
                    }
                } else {
                    // Functional error (e.g. operation failed even if HTTP status is 200)
                    String error = json.optString("error", "An unknown error occurred.");
                    player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "API error: " + error);
                    plugin.getLogger().warning("[API ERROR] " + error);

                    if (mode.equals("create")) {
                        cleanupDiscEntry(discName);
                    }
                }
            } catch (Exception e) {
                // Malformed JSON or other exception during parsing
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Failed to parse API response.");
                plugin.getLogger().severe("Failed to parse API response: " + responseBody);

                if (mode.equals("create")) {
                    cleanupDiscEntry(discName);
                }
            }
        } else if (responseCode == 419) { // communicate the error 'Authentication Timeout (YouTube cookie expired)' in the player chat
            JSONObject json = new JSONObject(responseBody);
            String error = json.optString("error", "Authentication Timeout (YouTube cookie expired) error.");
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + error);
            plugin.getLogger().warning("[API ERROR] " + error);

            if (mode.equals("create")) {
                cleanupDiscEntry(discName);
            }
        } else if (responseCode == 401) { // communicate the error 'Unauthorized' in the player chat
            JSONObject json = new JSONObject(responseBody);
            String error = json.optString("error", "Unauthorized error.");
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + error);
            plugin.getLogger().warning("[API ERROR] " + error);

            // Reset config
            try {
                File configFile = new File(plugin.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("token", "");
                config.set("downloadPackURL", "");
                config.save(configFile);

                plugin.setToken("");
                plugin.setDownloadPackURL("");
            } catch (IOException ex) {
                plugin.getLogger().warning("Failed to reset token and/or downloadPackURL in config.yml: " + ex.getMessage() + " You must clear these values manually in the config.yml file, then restart your Minecraft server.");
            }

            // Delete discs.json
            try {
                DiscJsonManager discManager = new DiscJsonManager(plugin);
                discManager.deleteDiscFile();
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to delete discs.json: " + ex.getMessage());
            }

            if (mode.equals("create")) {
                cleanupDiscEntry(discName);
            }
        } else if (responseCode == 409) { // communicate the error 'Conflict' in the player chat
            JSONObject json = new JSONObject(responseBody);
            String error = json.optString("error", "Conflict error.");
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + error);
            plugin.getLogger().warning("[API ERROR] " + error);

            if (mode.equals("create")) {
                cleanupDiscEntry(discName);
            }
        } else if (responseCode == 429) { // communicate the error 'Too Many Requests' in the player chat
            JSONObject json = new JSONObject(responseBody);
            String error = json.optString("error", "Too many requests error.");
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + error);
            plugin.getLogger().warning("[API ERROR] " + error);

            if (mode.equals("create")) {
                cleanupDiscEntry(discName);
            }
        } else if (responseCode == 503) { // communicate the error 'Service Unavailable' in the player chat
            JSONObject json = new JSONObject(responseBody);
            String error = json.optString("error", "Service Unavailable error.");
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + error);
            plugin.getLogger().warning("[API ERROR] " + error);

            if (mode.equals("create")) {
                cleanupDiscEntry(discName);
            }
        } else {
            // HTTP error code (4xx, 5xx, etc.)
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "HTTP Error " + responseCode + ": Check server logs for details.");
            plugin.getLogger().warning("API responded with HTTP error code: " + responseCode + ", response body: " + responseBody);

            if (mode.equals("create")) {
                cleanupDiscEntry(discName);
            }
        }
    }

    private void cleanupDiscEntry(String discName) {
        try {
            DiscJsonManager discManager = new DiscJsonManager(plugin);
            discManager.deleteDisc(discName);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to clean up local disc entry: " + ex.getMessage());
        }
    }
}
