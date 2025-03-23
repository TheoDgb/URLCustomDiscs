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
    private final Map<Location, Set<String>> activeJukeboxes = new HashMap<>();  // Utiliser un Set pour permettre plusieurs sons

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

            // Vérifier si un disque vanilla est joué
            ItemStack currentRecord = jukebox.getInventory().getItem(0);  // L'index 0 est généralement pour le disque du jukebox
            if (currentRecord != null && currentRecord.getType() != Material.AIR) {
                jukebox.getInventory().clear();
                event.setCancelled(true); // Annule l'événement pour empêcher la lecture par défaut
                // Planifie une tâche pour vider le jukebox et arrêter la musique
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                }, 1L);
            }

            // Si le jukebox est déjà actif, on arrête la musique et on retire le disque
            Set<String> activeSounds = activeJukeboxes.get(jukeboxLocation);
            if (activeSounds != null && !activeSounds.isEmpty()) {
                // Arrêter tous les sons associés à ce jukebox
                for (String currentSoundKey : activeSounds) {

                    // Arrêter le son pour tous les joueurs
                    // stopsound à des coordonnées... beautiful dream
                    // for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                    //     onlinePlayer.stopSound(currentSoundKey, org.bukkit.SoundCategory.RECORDS);
                    // }

                    // Arrêter le son (commande vanilla) pour tous les joueurs dans une portée de 50 blocks autour du jukebox
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                            "execute positioned " + jukeboxLocation.getBlockX() + " " + jukeboxLocation.getBlockY() + " " + jukeboxLocation.getBlockZ() +
                                    " run stopsound @a[distance=..80] * minecraft:" + currentSoundKey);
                }
                activeSounds.clear();  // Vide la liste des sons actifs
                jukebox.getInventory().clear(); // Éjecte le disque
                event.setCancelled(true);
                // Forcer la suppression du disque dans le jukebox
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

                        // Jouer la musique personnalisée
                        jukebox.getWorld().playSound(jukeboxLocation, soundKey, org.bukkit.SoundCategory.RECORDS, 5.0f, 1.0f);

                        // Ajouter le son à la liste des sons actifs de ce jukebox
                        activeJukeboxes
                                .computeIfAbsent(jukeboxLocation, k -> new HashSet<>())
                                .add(soundKey);

                        event.setCancelled(true); // Empêcher le jukebox de jouer le disque 13 par défaut
                    }
                }
            }
        }
    }
}