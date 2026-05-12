package com.bluebed.sonar.gui.selector;

import com.bluebed.sonar.Sonar;
import com.bluebed.sonar.SongInfo;
import com.bluebed.sonar.constructor.SonarJukebox;
import com.bluebed.sonar.gui.search.SonarSearch;
import com.bluebed.sonar.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

import static com.bluebed.sonar.constructor.SonarManager.getClosestJukebox;

public class SonarSongSelector implements Listener {
    private static final String TITLE = "Song Selector";
    private static final Map<UUID, Integer> pages = new HashMap<>();

    private static final List<Material> DISCS = List.of(
            Material.MUSIC_DISC_13, Material.MUSIC_DISC_CAT, Material.MUSIC_DISC_BLOCKS,
            Material.MUSIC_DISC_CHIRP, Material.MUSIC_DISC_FAR, Material.MUSIC_DISC_MALL,
            Material.MUSIC_DISC_MELLOHI, Material.MUSIC_DISC_STAL, Material.MUSIC_DISC_STRAD,
            Material.MUSIC_DISC_WARD, Material.MUSIC_DISC_11, Material.MUSIC_DISC_WAIT,
            Material.MUSIC_DISC_OTHERSIDE, Material.MUSIC_DISC_5, Material.MUSIC_DISC_PIGSTEP,
            Material.MUSIC_DISC_RELIC, Material.MUSIC_DISC_CREATOR, Material.MUSIC_DISC_PRECIPICE
    );

    public static void open(Player player, SonarJukebox jukebox, int page) {
        List<SongInfo> songs = new ArrayList<>(Sonar.getCachedSongInfo().values());

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        int start = page * 28;
        int end = Math.min(start + 28, songs.size());

        for (int i = start; i < end; i++) {
            inv.addItem(createSongItem(songs.get(i)));
        }

        inv.setItem(45, button(Material.RED_STAINED_GLASS_PANE, "§cBack"));
        inv.setItem(53, button(Material.LIME_STAINED_GLASS_PANE, "§aNext"));

        inv.setItem(49, button(Material.BARRIER, "§cStop Music"));
        inv.setItem(48, button(Material.SPYGLASS, "§eSearch"));
        inv.setItem(50, volumeItem(jukebox));
        inv.setItem(51, button(Material.ARROW, "§aNext Queue"));

        pages.put(player.getUniqueId(), page);
        player.openInventory(inv);
    }

    private static ItemStack createSongItem(SongInfo song) {
        Material mat = DISCS.get(Math.abs(song.album().hashCode()) % DISCS.size());

        return new ItemBuilder()
                .toBuilder()
                .material(mat)
                .name(song.title())
                .lore(
                        "§7",
                        "§7Album: §f" + song.album(),
                        "§7Artist: §f" + song.artist(),
                        "§7Duration: §f" + time(song.duration()),
                        "§7",
                        "§aLeft click to play",
                        "§cRight click to queue"
                )
                .get();
    }

    private static ItemStack button(Material mat, String name) {
        return new ItemBuilder().toBuilder().material(mat).name(name).get();
    }

    private static ItemStack volumeItem(SonarJukebox jukebox) {
        return new ItemBuilder()
                .toBuilder()
                .material(Material.NOTE_BLOCK)
                .name("§eVolume")
                .lore(
                        "§7Current: §e" + Math.round(jukebox.getVolume() * 100) + "%",
                        "§aLeft: +",
                        "§cRight: -",
                        "§7Shift = 10%"
                )
                .get();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!e.getView().getTitle().equals(TITLE)) return;

        e.setCancelled(true);

        SonarJukebox jukebox = getJukebox(player);
        int slot = e.getRawSlot();
        int page = pages.getOrDefault(player.getUniqueId(), 0);

        List<SongInfo> songs = new ArrayList<>(Sonar.getCachedSongInfo().values());

        // NAV
        if (slot == 45 && page > 0) {
            open(player, jukebox, page - 1);
            return;
        }

        if (slot == 53 && (page + 1) * 28 < songs.size()) {
            open(player, jukebox, page + 1);
            return;
        }

        // CONTROLS
        if (slot == 49) {
            if (!player.hasPermission("sonar.stop")) {
                player.sendMessage("§cNo permission.");
                return;
            }

            jukebox.stopAllAudio();
            player.sendMessage("§aStopped music.");
            return;
        }

        if (slot == 48) {
            if (!player.hasPermission("sonar.search")) {
                player.sendMessage("§cNo permission.");
                return;
            }

            SonarSearch.startSearch(player, jukebox);
            return;
        }

        if (slot == 50) {
            if (!player.hasPermission("sonar.volume")) {
                player.sendMessage("§cNo permission.");
                return;
            }

            if (e.isLeftClick()) jukebox.addVolume(e.isShiftClick() ? 0.1f : 0.01f);
            else jukebox.removeVolume(e.isShiftClick() ? 0.1f : 0.01f);

            open(player, jukebox, page);
            return;
        }

        if (slot == 51) {
            if (!player.hasPermission("sonar.queue.next")) {
                player.sendMessage("§cNo permission.");
                return;
            }

            SongInfo next = jukebox.getSonarSongQueue().getFirstQueue();
            if (next != null) {
                jukebox.stopAllAudio();
                Bukkit.getScheduler().runTask(Sonar.getPlugin(), () -> jukebox.playSong(next, true));
            }
            return;
        }

        // SONG CLICK
        ItemStack item = e.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        String title = item.getItemMeta().getDisplayName();

        SongInfo song = Sonar.getCachedSongInfo().values()
                .stream()
                .filter(s -> s.title().equals(title))
                .findFirst().orElse(null);

        if (song == null) return;

        if (e.isLeftClick()) {
            if (!player.hasPermission("sonar.play")) {
                player.sendMessage("§cNo permission.");
                return;
            }

            jukebox.stopAllAudio();
            jukebox.playSong(song, true);
            player.sendMessage("§aPlaying " + song.title());
        } else {
            if (!player.hasPermission("sonar.queue.add")) {
                player.sendMessage("§cNo permission.");
                return;
            }

            jukebox.addSong(song);
            player.sendMessage("§aAdded to queue.");
        }
    }

    private static String time(double d) {
        int t = (int) d;
        return (t / 60) + ":" + String.format("%02d", t % 60);
    }

    private SonarJukebox getJukebox(Player player) {
        return getClosestJukebox(player.getLocation(), 5);
    }

}
