package com.urlcustomdiscs;
import com.urlcustomdiscs.utils.DiscUtils;

import com.urlcustomdiscs.utils.MinecraftServerVersionUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.Block;
import org.json.JSONObject;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class JukeboxListener implements Listener {

    private final URLCustomDiscs plugin;
    private final Map<Location, Set<String>> activeJukeboxes = new HashMap<>();
    private final Map<Location, ItemStack> customDiscsInJukeboxes = new HashMap<>();
    private final JukeboxMessageInterceptor messageInterceptor;
    private final boolean protocolLibEnabled;

    public JukeboxListener(URLCustomDiscs plugin, boolean protocolLibEnabled) {
        this.plugin = plugin;
        this.protocolLibEnabled = protocolLibEnabled;

        // Only initialize the messageInterceptor if ProtocolLib is available
        if (protocolLibEnabled) {
            this.messageInterceptor = new JukeboxMessageInterceptor(plugin);
        } else {
            this.messageInterceptor = null;
        }
    }

    @EventHandler
    public void onJukeboxPlay(PlayerInteractEvent event) {

        // Verify that the event is a right-click on a block
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Verify that the clicked block is a jukebox
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.JUKEBOX) {
            return;
        }

        // Ignore offhand interactions
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        File discUuidFile = new File(plugin.getDataFolder(), "discs.json");
        JSONObject discData = DiscUtils.loadDiscData(discUuidFile);

        Block jukeboxBlock = event.getClickedBlock();
        Jukebox jukebox = (Jukebox) jukeboxBlock.getState();
        Location jukeboxLocation = jukebox.getLocation();

        ItemStack currentRecord = getCurrentRecordCompatibility(jukebox); // Get the current content of the jukebox (normally disc or air)

        Set<String> activeSounds = activeJukeboxes.get(jukeboxLocation); // Currently playing custom sounds
        ItemStack customDisc = customDiscsInJukeboxes.get(jukeboxLocation); // Currently playing custom disc

        // Get the item the player is holding
        ItemStack itemInHand = event.getItem();

        Player player = event.getPlayer();

        // Check if the jukebox contains a physical vanilla disc (do nothing - ejects the disc)
        if (currentRecord.getType() != Material.AIR) {
            // Remove the stored custom disc info
            customDiscsInJukeboxes.remove(jukeboxLocation);
            return;
        }

        // If the jukebox is physically empty:

        // If the jukebox is playing a custom sound, stop it and drop the custom disc
        // Check if the jukebox is playing a custom sound
        if (activeSounds != null && !activeSounds.isEmpty()) {

            // If the player is not sneaking and holding a placeable block, do not place it
            if (!player.isSneaking() && itemInHand != null && itemInHand.getType().isBlock()) {
                event.setCancelled(true);
            }

            // If the player is sneaking and holding an item, do not stop/remove the custom disc and allow block placement
            if (player.isSneaking() && itemInHand != null) {
                return;
            }

            // Force player hand animation
            player.swingMainHand();

            // Check if the player is holding a disc item
            if (itemInHand != null && itemInHand.getType().isRecord()) {
                // Cancel the event to prevent any disc from playing
                event.setCancelled(true);
            }

            // Stop all custom sounds associated with this jukebox
            for (String currentSoundKey : activeSounds) {
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                        "execute positioned " + jukeboxLocation.getBlockX() + " " + jukeboxLocation.getBlockY() + " " + jukeboxLocation.getBlockZ() +
                                " run stopsound @a[distance=..80] * minecraft:" + currentSoundKey);
            }

            // Clear the list of active sounds for this jukebox
            activeSounds.clear();

            // Drop the stored custom disc
            dropCustomDisc(jukeboxLocation, customDisc);

            // Remove the stored custom disc info
            customDiscsInJukeboxes.remove(jukeboxLocation);

            return;
        }

        // If the jukebox is empty, no custom sound is playing, the player is holding a custom disc item and not sneaking
        // => insert the custom disc into the jukebox, stop the vanilla disc 13 sound, then play the custom disc sound

        // Check if the player is holding a custom disc item (disc 13 model) and not sneaking
        if (itemInHand != null && itemInHand.getType() == Material.MUSIC_DISC_13 && itemInHand.hasItemMeta() && !player.isSneaking()) {
            ItemMeta meta = itemInHand.getItemMeta();

            // Check if this item has custom model data
            if (meta != null && meta.hasCustomModelData()) {
                int customModelData = meta.getCustomModelData();

                String discName = DiscUtils.getDiscNameFromCustomModelData(discData, customModelData);
                // Check if the disc name is valid
                if (discName != null) {

                    // Cancel the event to prevent disc_13 (based model for custom discs) from playing
                    event.setCancelled(true);

                    try {
                        String soundKey = "customdisc." + discName;

                        // Store the custom disc for later ejection
                        ItemStack discToStore = itemInHand.clone();
                        discToStore.setAmount(1);
                        customDiscsInJukeboxes.put(jukeboxLocation, discToStore);

                        // Remove the custom disc from the player's hand only if he is not in Creative mode
                        if (player.getGameMode() != GameMode.CREATIVE) {
                            if (itemInHand.getAmount() > 1) {
                                itemInHand.setAmount(itemInHand.getAmount() - 1);
                            } else {
                                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                            }
                        }

                        // Play the custom audio on the next tick to allow the disc to be inserted into the jukebox
                        plugin.getServer().getScheduler().runTask(plugin, () -> {

                            // Play the custom sound
                            jukebox.getWorld().playSound(jukeboxLocation, soundKey, org.bukkit.SoundCategory.RECORDS, 4.0f, 1.0f);

                            // Add the sound to the list of active sounds for this jukebox
                            activeJukeboxes
                                    .computeIfAbsent(jukeboxLocation, k -> new HashSet<>())
                                    .add(soundKey);
                        });

                        // Retrieve disc info for the Now Playing Toast
                        JSONObject discInfo = discData.getJSONObject(discName);
                        String displayName = discInfo.getString("displayName");

                        // Mark this jukebox as having a custom disc inserted to display a Now Playing Toast custom message
                        if (protocolLibEnabled && messageInterceptor != null) {
                            messageInterceptor.markCustomDiscInserted(jukeboxLocation, displayName);
                        }

                    } catch (Exception e) {
                        plugin.getLogger().warning("Error inserting the custom disc in the jukebox: " + e.getMessage());
                        plugin.getLogger().severe("Exception: " + e.getMessage());

                        // Remove the stored custom disc info
                        customDiscsInJukeboxes.remove(jukeboxLocation);

                        // Return the item to the player only if he is not in Creative mode
                        if (player.getGameMode() != GameMode.CREATIVE) {
                            if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                                player.getInventory().setItemInMainHand(itemInHand);
                            } else {
                                player.getInventory().addItem(itemInHand);
                            }
                        }
                    }
                }
            }
        }
    }

    private ItemStack getCurrentRecordCompatibility(Jukebox jukebox) {
        try {
            // Try the standard method (returns ItemStack on most builds)
            // For Bukkit, Spigot, Paper, and maybe others
            return jukebox.getRecord(); // Get the current content of the jukebox (normally disc or air)
        } catch (NoSuchMethodError e) {
            // If the CraftBukkit/CraftJukebox implementation is incompatible, fallback to getPlaying()
            // For Arclight server
            try {
                Material playing = jukebox.getPlaying();
                return playing != Material.AIR ? new ItemStack(playing) : new ItemStack(Material.AIR);
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "Fallback getPlaying() failed", t);
                return new ItemStack(Material.AIR);
            }
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Unexpected error getting jukebox record", t);
            return new ItemStack(Material.AIR);
        }
    }

    @EventHandler
    public void onJukeboxBreak(BlockBreakEvent event) {
        Block brokenBlock = event.getBlock();

        // Check if the broken block is a jukebox
        if (brokenBlock.getType() != Material.JUKEBOX) {
            return;
        }

        Location jukeboxLocation = brokenBlock.getLocation();
        Set<String> activeSounds = activeJukeboxes.get(jukeboxLocation);
        ItemStack customDisc = customDiscsInJukeboxes.get(jukeboxLocation);

        // If this jukebox was playing a custom sound, stop it and drop the custom disc
        if (activeSounds != null && !activeSounds.isEmpty() && customDisc != null) {

            // Stop all custom sounds associated with this jukebox
            for (String currentSoundKey : activeSounds) {
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                        "execute positioned " + jukeboxLocation.getBlockX() + " " + jukeboxLocation.getBlockY() + " " + jukeboxLocation.getBlockZ() +
                                " run stopsound @a[distance=..80] * minecraft:" + currentSoundKey);
            }

            // Drop the stored custom disc
            dropCustomDisc(jukeboxLocation, customDisc);
        }

        // Clean up the jukebox from the active jukeboxes map and the stored custom disc info
        cleanupJukebox(jukeboxLocation);
    }

    // Cleanup method for when jukeboxes are destroyed
    public void cleanupJukebox(Location location) {
        // Remove the jukebox from the active jukeboxes map
        activeJukeboxes.remove(location);
        // Remove the stored custom disc info
        customDiscsInJukeboxes.remove(location);
    }

    private void dropCustomDisc(Location jukeboxLocation, ItemStack customDisc) {
        String minecraftServerVersion = plugin.getMinecraftServerVersion();
        MinecraftServerVersionUtils version = MinecraftServerVersionUtils.parse(minecraftServerVersion);
        if (version.isNewDropPosition()) {
            // Drop the stored custom disc using the new drop position behavior (1.21.8+)
            Objects.requireNonNull(jukeboxLocation.getWorld()).dropItemNaturally(
                    jukeboxLocation.clone().add(0.5, 1.05, 0.5), customDisc.clone());
        } else {
            // Drop the stored custom disc using the old drop position behavior
            Objects.requireNonNull(jukeboxLocation.getWorld()).dropItemNaturally(
                    jukeboxLocation.clone().add(0, 0.55, 0), customDisc.clone());
        }
    }
}