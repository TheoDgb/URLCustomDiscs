package com.urlcustomdiscs;
import com.urlcustomdiscs.utils.DiscUtils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Jukebox;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.Block;
import org.json.JSONObject;

import java.io.File;
import java.util.*;

public class JukeboxListener implements Listener {

    private final URLCustomDiscs plugin;
    private final Map<Location, Set<String>> activeJukeboxes = new HashMap<>();  // Use a Set to enable multiple sounds

    public JukeboxListener(URLCustomDiscs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJukeboxPlay(PlayerInteractEvent event) {
        File discUuidFile = new File(plugin.getDataFolder(), "discs.json");
        JSONObject discData = DiscUtils.loadDiscData(discUuidFile);

        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.JUKEBOX) {
            Block jukeboxBlock = event.getClickedBlock();
            Jukebox jukebox = (Jukebox) jukeboxBlock.getState();
            Location jukeboxLocation = jukebox.getLocation();

            // Check if a vanilla disc is playing
            ItemStack currentRecord = jukebox.getInventory().getItem(0);  // Index 0 is usually for the jukebox disc
            if (currentRecord != null && currentRecord.getType() != Material.AIR) {
                jukebox.getInventory().clear();
                event.setCancelled(true); // Cancel event to prevent playback by default
                // Schedule a task to empty the jukebox and stop the music
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                }, 1L);
            }

            // If the jukebox is already active, stop the music and remove the disc
            Set<String> activeSounds = activeJukeboxes.get(jukeboxLocation);
            if (activeSounds != null && !activeSounds.isEmpty()) {
                // Arrêter tous les sons associés à ce jukebox
                for (String currentSoundKey : activeSounds) {

                    // Mute sound for all players
                    // stopsound at coordinates... beautiful dream
                    // for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                    //     onlinePlayer.stopSound(currentSoundKey, org.bukkit.SoundCategory.RECORDS);
                    // }

                    // Stop sound (vanilla command) for all players within 50 blocks of the jukebox
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                            "execute positioned " + jukeboxLocation.getBlockX() + " " + jukeboxLocation.getBlockY() + " " + jukeboxLocation.getBlockZ() +
                                    " run stopsound @a[distance=..80] * minecraft:" + currentSoundKey);
                }
                activeSounds.clear();  // Clears the list of active sounds
                jukebox.getInventory().clear(); // Eject the disc
                event.setCancelled(true);
                // Force delete disc from jukebox
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> jukebox.setRecord(null), 1L);
                return;
            }

            ItemStack item = event.getItem();
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasCustomModelData()) {
                    int customModelData = meta.getCustomModelData();

                    String discName = DiscUtils.getDiscNameFromCustomModelData(discData, customModelData);
                    if (discName != null) {
                        String soundKey = "customdisc." + discName;

                        // Play custom music
                        jukebox.getWorld().playSound(jukeboxLocation, soundKey, org.bukkit.SoundCategory.RECORDS, 5.0f, 1.0f);

                        // Add the sound to the list of active sounds of this jukebox
                        activeJukeboxes
                                .computeIfAbsent(jukeboxLocation, k -> new HashSet<>())
                                .add(soundKey);

                        event.setCancelled(true); // Prevent the jukebox from playing disc 13 by default
                    }
                }
            }
        }
    }
}
