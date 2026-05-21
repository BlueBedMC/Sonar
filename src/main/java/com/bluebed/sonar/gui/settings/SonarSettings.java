package com.bluebed.sonar.gui.settings;

import com.bluebed.sonar.constructor.SonarJukebox;
import com.bluebed.sonar.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SonarSettings {

    private static final Map<UUID, SonarJukebox> JUKEBOXES = new HashMap<>();

    private static final String TITLE = "Settings";

    public static void open(Player player, SonarJukebox jukebox) {
        JUKEBOXES.put(player.getUniqueId(), jukebox);

        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        fillBorders(inv);

        inv.setItem(10, createSetting(jukebox, "title", "Title",
                "The title and artist/s of the song", Material.NAME_TAG));

        inv.setItem(11, createSetting(jukebox, "queue", "Queue",
                "A list of songs in the queue", Material.BOOK));

        inv.setItem(12, createSetting(jukebox, "cover", "Cover",
                "The album/song cover picture", Material.MAP));

        inv.setItem(13, createSetting(jukebox, "lyrics", "Lyrics",
                "The lyrics of the song", Material.PAPER));

        inv.setItem(14, createSetting(jukebox, "duration", "Duration",
                "The duration/playhead of the song", Material.CLOCK));

        inv.setItem(15, createSetting(jukebox, "particle", "Particles",
                "The song particle/s", Material.GLOWSTONE));

        player.openInventory(inv);
    }

    private static void fillBorders(Inventory inv) {
        ItemStack pane = new ItemBuilder()
                .toBuilder()
                .material(Material.BLACK_STAINED_GLASS_PANE)
                .name("§7")
                .get();

        for (int i = 0; i < 27; i++) {
            if (i < 9 || i > 17 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, pane);
            }
        }
    }

    private static ItemStack createSetting(SonarJukebox jukebox, String key,
                                           String label, String desc, Material mat) {
        boolean enabled = jukebox.getSettings().get(key);

        return new ItemBuilder()
                .toBuilder()
                .material(mat)
                .name("§e" + label)
                .lore(
                        "§7",
                        "§7" + desc,
                        "§7Enabled: " + (enabled ? "§aYes" : "§cNo"),
                        "§7",
                        "§8Shift-click to reposition"
                )
                .get();
    }

    public static SonarJukebox getJukebox(Player player) {
        return JUKEBOXES.get(player.getUniqueId());
    }

    public static void removeJukebox(Player player) {
        JUKEBOXES.remove(player);
    }
}