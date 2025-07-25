package com.urlcustomdiscs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class RemoteApiClient {

    private final URLCustomDiscs plugin;
    private final String apiBaseURL;

    public RemoteApiClient(URLCustomDiscs plugin, String apiBaseURL) {
        this.plugin = plugin;
        this.apiBaseURL = apiBaseURL;
    }

    public void requestTokenFromRemoteServer(Player player, Runnable onSuccess) {
        player.sendMessage(ChatColor.GRAY + "Registering server with remote backend...");
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

                            player.sendMessage(ChatColor.GREEN + "Your token is generated and downloadPackURL is available in the config.yml file of the URLCustomDiscs plugin.");
                            player.sendMessage(ChatColor.YELLOW + "Do not forget to set resource-pack= with the downloadPackURL in your Minecraft server's server.properties file, following the instructions in the config.yml file of the URLCustomDiscs plugin.");
                            Bukkit.getScheduler().runTask(plugin, onSuccess);
                        } else {
                            plugin.getLogger().warning("No token received from remote server.");
                        }
                    }
                } else {
                    plugin.getLogger().warning("Remote server returned status code: " + responseCode);
                }

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to register with remote server:");
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

    public void createCustomDiscRemotely(Player player, String url, String discName, String audioType, JSONObject discInfo, String token) {
        player.sendMessage(ChatColor.GRAY + "Sending information to the remote server...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection connection = createPostConnection("/create-custom-disc");

                JSONObject payload = new JSONObject();
                payload.put("url", url);
                payload.put("discName", discName);
                payload.put("audioType", audioType);
                payload.put("customModelData", discInfo.getInt("customModelData"));
                payload.put("token", token);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (InputStream responseStream = connection.getInputStream()) {
                        String response = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
                        Bukkit.getScheduler().runTask(plugin, () -> handleApiResponse(player, responseCode, response, discInfo));
                    }
                } else {
                    InputStream errorStream = connection.getErrorStream();
                    if (errorStream != null) {
                        String errorResponse = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                        Bukkit.getScheduler().runTask(plugin, () -> handleApiResponse(player, responseCode, errorResponse, discInfo));
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Failed to create disc. HTTP status: " + responseCode);
                            plugin.getLogger().warning("API returned status: " + responseCode);
                        });
                    }
                }

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "An error occurred while sending the request.");
                    plugin.getLogger().severe("Failed to send disc creation request:");
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

    private void handleApiResponse(Player player, int responseCode, String responseBody, JSONObject discInfo) {
        if (responseCode >= 200 && responseCode < 300) {  // HTTP success range
            try {
                JSONObject json = new JSONObject(responseBody);
                if (json.has("success") && json.getBoolean("success")) {
                    // Functional success (the API operation completed successfully)
                    String message = json.optString("message", "Operation completed successfully.");
                    player.sendMessage(ChatColor.GREEN + message);
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
                } else {
                    // Functional error (e.g. operation failed even if HTTP status is 200)
                    String error = json.optString("error", "An unknown error occurred.");
                    player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "API error: " + error);
                    plugin.getLogger().warning("[API ERROR] " + error);
                }
            } catch (Exception e) {
                // Malformed JSON or other exception during parsing
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Failed to parse API response.");
                plugin.getLogger().severe("Failed to parse API response: " + responseBody);
            }
        } else {
            // HTTP error code (4xx, 5xx, etc.)
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "HTTP Error " + responseCode + ": Check server logs for details.");
            plugin.getLogger().warning("API responded with HTTP error code: " + responseCode + ", response body: " + responseBody);
        }
    }
}
