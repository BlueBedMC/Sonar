package com.bluebed.sonar.gui.search;

import com.bluebed.sonar.Sonar;
import com.bluebed.sonar.SongInfo;
import com.bluebed.sonar.constructor.SonarJukebox;
import com.bluebed.sonar.gui.selector.SonarSongSelector;
import com.bluebed.sonar.util.ItemBuilder;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

import static com.bluebed.sonar.command.SonarCommands.lastPlay;
import static com.bluebed.sonar.constructor.SonarManager.getClosestJukebox;

public class SonarSearch implements Listener {

    private static final Map<UUID, SonarJukebox> searching = new HashMap<>();

    public static void startSearch(Player player, SonarJukebox jukebox) {
        searching.put(player.getUniqueId(), jukebox);

        player.closeInventory();
        player.sendMessage("§eType your search in chat. Type 'cancel' to exit.");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();

        if (!searching.containsKey(player.getUniqueId())) return;

        e.setCancelled(true);

        String msg = e.getMessage();

        if (msg.equalsIgnoreCase("cancel")) {
            searching.remove(player.getUniqueId());
            player.sendMessage("§cSearch cancelled.");
            return;
        }

        SonarJukebox jukebox = searching.remove(player.getUniqueId());

        List<SongInfo> results = Sonar.getCachedSongInfo().values()
                .stream()
                .filter(s ->
                        s.title().toLowerCase().contains(msg.toLowerCase()) ||
                                s.artist().toLowerCase().contains(msg.toLowerCase()) ||
                                s.album().toLowerCase().contains(msg.toLowerCase())
                )
                .limit(28)
                .toList();

        Bukkit.getScheduler().runTask(Sonar.getPlugin(), () -> {
            Inventory inv = Bukkit.createInventory(null, 54, "Search Results");

            for (SongInfo song : results) {
                inv.addItem(new ItemBuilder()
                        .toBuilder()
                        .material(Material.MUSIC_DISC_CAT)
                        .name(song.title())
                        .lore(
                                "§7" + song.artist(),
                                "§aClick to play")
                        .build());
            }

            player.openInventory(inv);
        });
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!e.getView().getTitle().equals("Search Results")) return;

        e.setCancelled(true);

        ItemStack item = e.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        String title = item.getItemMeta().getDisplayName();

        SongInfo song = Sonar.getCachedSongInfo().values()
                .stream()
                .filter(s -> s.title().equals(title))
                .findFirst().orElse(null);

        if (song == null) return;

        SonarJukebox jukebox = getClosestJukebox(player.getLocation(), 5);

        jukebox.stopAllAudio();
        jukebox.playSong(song, true);

        player.sendMessage("§aPlaying " + song.title());
    }
}
