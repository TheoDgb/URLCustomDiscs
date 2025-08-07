package com.urlcustomdiscs;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.json.JSONObject;

import java.util.List;

public class DiscFactory {

    public static void giveCustomDiscToPlayer(Player player, JSONObject discInfo) {
        if (discInfo == null) return;

        String displayName = discInfo.optString("displayName", "Unknown Disc");
        int customModelData = discInfo.optInt("customModelData", 0);

        ItemStack disc = new ItemStack(Material.MUSIC_DISC_13);
        ItemMeta meta = disc.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + displayName);
            meta.setLore(List.of(ChatColor.GRAY + "Custom disc: " + displayName));
            meta.setCustomModelData(customModelData);
            disc.setItemMeta(meta);
        }

        player.getInventory().addItem(disc);
        player.sendMessage(ChatColor.GREEN + "You received your custom disc: " + ChatColor.GOLD + displayName);
    }
}
