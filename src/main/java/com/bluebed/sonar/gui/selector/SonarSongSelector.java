package com.bluebed.sonar.gui.selector;

import com.bluebed.sonar.Sonar;
import com.bluebed.sonar.SonarPlugin;
import com.bluebed.sonar.SongInfo;
import com.bluebed.sonar.constructor.SonarJukebox;
import com.bluebed.sonar.gui.queue.SonarSongQueue;
import com.bluebed.sonar.gui.search.SonarSearch;
import com.bluebed.sonar.util.ItemBuilder;
import com.mojang.brigadier.Command;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;
import xyz.xenondevs.invui.window.Window;

import java.io.File;
import java.util.*;

import static com.bluebed.sonar.command.SonarCommands.lastPlay;

public class SonarSongSelector {

    private static final List<Material> discs = List.of(
            Material.MUSIC_DISC_13, Material.MUSIC_DISC_CAT, Material.MUSIC_DISC_BLOCKS,
            Material.MUSIC_DISC_CHIRP, Material.MUSIC_DISC_FAR, Material.MUSIC_DISC_MALL,
            Material.MUSIC_DISC_MELLOHI, Material.MUSIC_DISC_STAL, Material.MUSIC_DISC_STRAD,
            Material.MUSIC_DISC_WARD, Material.MUSIC_DISC_11, Material.MUSIC_DISC_WAIT,
            Material.MUSIC_DISC_OTHERSIDE, Material.MUSIC_DISC_5, Material.MUSIC_DISC_PIGSTEP,
            Material.MUSIC_DISC_RELIC, Material.MUSIC_DISC_CREATOR, Material.MUSIC_DISC_PRECIPICE
    );

    public static void open(Player player, SonarJukebox jukebox) {
        List<Item> items = getItems(jukebox);

        final PagedGui<Item> menu = PagedGui.items()
                .setStructure(
                        "Q # # # # # # # V",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "< # # # S N # # >"
                )
                .addIngredient('#', new BorderItem())
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('<', new BackItem())
                .addIngredient('>', new ForwardItem())
                .addIngredient('S', new StopMusicItem(jukebox))
                .addIngredient('N', new NextMusicItem(jukebox))
                .addIngredient('Q', new SearchMusicItem(jukebox))
                .addIngredient('V', new MusicVolumeItem(jukebox))
                .setContent(items)
                .build();

        final Window window = Window.single()
                .setViewer(player)
                .setGui(menu)
                .setTitle("Song Selector")
                .build();

        window.open();
    }

    private static List<Item> getItems(SonarJukebox jukebox) {
        List<Item> items = new ArrayList<>();

        for (Map.Entry<String, SongInfo> entry : Sonar.getCachedSongInfo().entrySet()) {
            items.add(new SongItem(jukebox, entry.getValue()));
        }

        return items;
    }

    private static final class BorderItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder()
                    .toBuilder()
                    .material(Material.BLACK_STAINED_GLASS_PANE)
                    .name("§7");
        }

        @Override
        public void handleClick(
                final @NotNull ClickType click,
                final @NotNull Player player,
                final @NotNull InventoryClickEvent event
        ) {
            event.setCancelled(true);
        }
    }

    @RequiredArgsConstructor
    private static final class SongItem extends AbstractItem {
        private final SonarJukebox jukebox;
        private final SongInfo songInfo;

        @Override
        public ItemProvider getItemProvider() {
            Material randomMaterial = discs.get(Math.abs(songInfo.album().hashCode()) % discs.size());
            return new ItemBuilder()
                    .toBuilder()
                    .material(randomMaterial)
                    .lore(
                            "§7",
                            "§7Album: §f" + songInfo.album(),
                            "§7Artist: §f" + songInfo.artist(),
                            "§7Duration: §f" + doubleToTime(songInfo.duration()),
                            "§7",
                            "§aʟᴇꜰᴛ-ᴄʟɪᴄᴋ §7ᴛᴏ §aᴘʟᴀʏ",
                            "§cʀɪɢʜᴛ-ᴄʟɪᴄᴋ §7ᴛᴏ §cᴀᴅᴅ ᴛᴏ ǫᴜᴇᴜᴇ" )
                    .name(songInfo.title());
        }

        @Override
        public void handleClick(@NotNull ClickType click, @NotNull Player player, @NotNull InventoryClickEvent event) {
            event.setCancelled(true);

            Sonar plugin = Sonar.getPlugin();
            String songId = songInfo.id();
            String path = plugin.getDataFolder() + "/songs/" + songId;
            if (!path.endsWith(".mp3")) path += ".mp3";

            File file = new File(path);
            if (!file.exists()) {
                player.sendMessage("§cThat file doesn't exist!");
                return;
            }

            SongInfo info = Sonar.getCachedSongInfo().get(songId);
            if (info == null) {
                player.sendMessage("§cThat song isn't cached. Get an administrator to reload.");
                return;
            }

            if (click.isLeftClick()) {
                if ((System.currentTimeMillis() - lastPlay) < 1000) {
                    player.sendMessage("§cPlease wait a second!");
                    return;
                }

                lastPlay = System.currentTimeMillis();
                jukebox.stopAllAudio();
                jukebox.playSong(info, true);
                player.sendMessage("§aStarted playing " + songInfo.title() + ".");
                return;
            }

            // add to queue
            if (jukebox.getQueue().contains(info) && !player.hasPermission("sonar.override.queue")) {
                player.sendMessage("§c" + info.title() + " is already in the queue!");
                return;
            }

            jukebox.addSong(info);
            player.sendMessage("§aAdded " + songInfo.title() + " to queue.");
        }
    }

    public static final class BackItem extends PageItem {
        public BackItem() {
            super(false);
        }

        @Override
        public ItemProvider getItemProvider(@NotNull final PagedGui<?> gui) {
            return new ItemBuilder()
                    .toBuilder()
                    .material(Material.RED_STAINED_GLASS_PANE)
                    .name("§cBack")
                    .lore(
                            List.of(gui.hasPreviousPage() ? "§f" + gui.getCurrentPage() + "§7/§f" + gui.getPageAmount() : "§7Can't go back any further")
                    );
        }
    }

    public static final class ForwardItem extends PageItem {
        public ForwardItem() {
            super(true);
        }

        @Override
        public ItemProvider getItemProvider(@NotNull final PagedGui<?> gui) {
            return new ItemBuilder()
                    .toBuilder()
                    .material(Material.LIME_STAINED_GLASS_PANE)
                    .name("§aForward")
                    .lore(
                            List.of(gui.hasNextPage() ? "§f" + (gui.getCurrentPage() + 2) + "§7/§f" + gui.getPageAmount() : "§7There are no more pages")
                    );
        }
    }

    @RequiredArgsConstructor
    public static final class StopMusicItem extends AbstractItem {
        private final SonarJukebox jukebox;

        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder()
                    .toBuilder()
                    .material(Material.BARRIER)
                    .lore(
                            "§7",
                            "§7Stop all music playing",
                            "§7"
                    )
                    .name("§cStop Music");
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
            if (!player.hasPermission("sonar.stopmusic")) {
                player.sendMessage("§cYou do not have permission for this.");
                return;
            }

            jukebox.stopAllAudio();
            player.sendMessage("§aStopped all playing music.");
        }
    }

    @RequiredArgsConstructor
    public static final class SearchMusicItem extends AbstractItem {
        private final SonarJukebox jukebox;

        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder()
                    .toBuilder()
                    .material(Material.SPYGLASS)
                    .lore(
                            "§7",
                            "§7Search for a music",
                            "§7"
                    )
                    .name("§eSearch");
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
            if (!player.hasPermission("sonar.search")) {
                player.sendMessage("§cYou do not have permission for this.");
                return;
            }

            SonarSearch.open(player, jukebox);
        }
    }

    @RequiredArgsConstructor
    public static final class MusicVolumeItem extends AbstractItem {
        private final SonarJukebox jukebox;

        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder()
                    .toBuilder()
                    .material(Material.NOTE_BLOCK)
                    .lore(
                            "§7",
                            "§7Change volume level.",
                            "§7Current: §e" + Math.round(jukebox.getVolume() * 100) + "%",
                            "§7",
                            "§fʟᴇꜰᴛ ᴄʟɪᴄᴋ §7to §aincrease volume",
                            "§fʀɪɢʜᴛ ᴄʟɪᴄᴋ §7to §cdecrease volume",
                            "§fꜱʜɪꜰᴛ ᴄʟɪᴄᴋ §7to step by §e10%"
                    )
                    .name("§eVolume");
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
            if (!player.hasPermission("sonar.volume")) {
                player.sendMessage("§cYou do not have permission for this.");
                return;
            }

            if (clickType.isShiftClick()) {
                if (clickType.isLeftClick()) {
                    jukebox.addVolume(0.1F);
                    player.playSound(player, Sound.ENTITY_ARROW_HIT_PLAYER, jukebox.getVolume(), 1);
                } else {
                    jukebox.removeVolume(0.1F);
                    player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, jukebox.getVolume(), 1);
                }
            } else {
                if (clickType.isLeftClick()) {
                    jukebox.addVolume(0.01F);
                    player.playSound(player, Sound.ENTITY_ARROW_HIT_PLAYER, jukebox.getVolume(), 1);
                } else {
                    jukebox.removeVolume(0.01F);
                    player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, jukebox.getVolume(), 1);
                }
            }


        }
    }

    @RequiredArgsConstructor
    public static final class NextMusicItem extends AbstractItem {
        private final SonarJukebox jukebox;

        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder()
                    .toBuilder()
                    .material(Material.ARROW)
                    .lore(
                            "§7",
                            "§7Play next music in queue",
                            "§7"
                    )
                    .name("§aPlay Next");
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
            if (!player.hasPermission("sonar.nextqueue")) {
                player.sendMessage("§cYou do not have permission for this.");
                return;
            }

            SonarSongQueue sonarSongQueue = jukebox.getSonarSongQueue();
            if (sonarSongQueue == null) {
                player.sendMessage("§cThere are currently no songs in the queue.");
                return;
            }

            SongInfo nextInQueue = sonarSongQueue.getFirstQueue();
            if (nextInQueue == null) {
                player.sendMessage("§cThere are currently no songs in the queue.");
                return;
            }

            jukebox.stopAllAudio();
            Bukkit.getScheduler().runTask(Sonar.getPlugin(), () -> jukebox.playSong(nextInQueue, true));
            player.sendMessage("§aPlayed the next in queue");
        }
    }

    private static String doubleToTime(double value) {
        int totalSeconds = (int) Math.round(value);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
