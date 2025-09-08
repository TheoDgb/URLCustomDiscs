package com.urlcustomdiscs;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

// SimpleToastHandler
public class JukeboxMessageInterceptor {
    private final URLCustomDiscs plugin;
    private final Map<Location, String> recentCustomDiscs = new ConcurrentHashMap<>();
    private final Map<Player, Long> lastToastTime = new ConcurrentHashMap<>();

    public JukeboxMessageInterceptor(URLCustomDiscs plugin) {
        this.plugin = plugin;
        registerPacketListener();
    }

    public void markCustomDiscInserted(Location jukeboxLocation, String displayName) {
        recentCustomDiscs.put(jukeboxLocation, displayName);

        // Send the toast to all nearby players
        sendToastToNearbyPlayers(jukeboxLocation, displayName);

        // Delete after 10 secondes
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                recentCustomDiscs.remove(jukeboxLocation), 200L);
    }

    private void sendToastToNearbyPlayers(Location jukeboxLocation, String displayName) {
        // Execute on the next tick to allow the disc to be inserted into the jukebox
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getWorld().equals(jukeboxLocation.getWorld()) &&
                        player.getLocation().distance(jukeboxLocation) <= 80) {
                    sendCustomMusicToast(player, displayName);
                }
            }
        });
    }

    private void registerPacketListener() {
        // Intercept all potential toast packets to cancel them
        PacketType[] toastPackets = {
                PacketType.Play.Server.ADVANCEMENTS,
                PacketType.Play.Server.SELECT_ADVANCEMENT_TAB,
                PacketType.Play.Server.RECIPE_UPDATE
        };

        // Intercept system messages to remove duplicates
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
                ListenerPriority.HIGHEST, PacketType.Play.Server.SYSTEM_CHAT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                handleSystemChatPacket(event);
            }
        });

        for (PacketType packetType : toastPackets) {
            try {
                ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
                        ListenerPriority.HIGHEST, packetType) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        handlePotentialToastPacket(event);
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Unable to intercept " + packetType + ": " + e.getMessage());
            }
        }

        plugin.getLogger().info("Registered Toast Interceptors");
    }

    private void handleSystemChatPacket(PacketEvent event) {
        try {
            WrappedChatComponent component = event.getPacket().getChatComponents().read(0);
            if (component != null) {
                String message = component.getJson();

                // Suppress server-generated "Now Playing" messages
                if (isNowPlayingMessage(message)) {
                    String customName = findNearbyCustomDisc(event.getPlayer());
                    if (customName != null) {
                        event.setCancelled(true);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error intercepting system chat: " + e.getMessage());
        }
    }

    private void handlePotentialToastPacket(PacketEvent event) {
        try {
            Player player = event.getPlayer();
            String customName = findNearbyCustomDisc(player);

            if (customName != null) {
                // Cancel the original package to avoid double posting
                long currentTime = System.currentTimeMillis();
                Long lastTime = lastToastTime.get(player);

                if (lastTime == null || (currentTime - lastTime) > 1000) { // Avoid spam
                    event.setCancelled(true);
                    lastToastTime.put(player, currentTime);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error intercepting toast packet: " + e.getMessage());
        }
    }

    private void sendCustomMusicToast(Player player, String customName) {
        try {
            String messageJson = "[{\"text\":\"Now Playing: \",\"color\":\"aqua\"}," +
                    "{\"text\":\"" + customName.replace("\"", "\\\"") + "\",\"color\":\"gold\"}]";

            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                    "title " + player.getName() +
                            " actionbar " + messageJson);
        } catch (Exception e) {
            plugin.getLogger().warning("Error sending the custom toast: " + e.getMessage());
        }
    }

    private boolean isNowPlayingMessage(String message) {
        return message != null && (
                message.contains("record.nowPlaying") ||
                        message.contains("Now Playing") ||
                        message.toLowerCase().contains("playing") ||
                        message.contains("C418") ||
                        message.contains("13")
        );
    }

    private String findNearbyCustomDisc(Player player) {
        Location playerLoc = player.getLocation();
        for (Map.Entry<Location, String> entry : recentCustomDiscs.entrySet()) {
            Location jukeboxLoc = entry.getKey();

            // Check proximity
            if (!Objects.equals(playerLoc.getWorld(), jukeboxLoc.getWorld()) ||
                    playerLoc.distance(jukeboxLoc) > 80) continue;

            Jukebox jukebox = (Jukebox) jukeboxLoc.getBlock().getState();
            ItemStack record = jukebox.getRecord();

            if (record.getType() != Material.MUSIC_DISC_13) continue;
            if (!record.hasItemMeta() || !Objects.requireNonNull(record.getItemMeta()).hasCustomModelData()) continue;

            return entry.getValue();
        }
        return null;
    }
}