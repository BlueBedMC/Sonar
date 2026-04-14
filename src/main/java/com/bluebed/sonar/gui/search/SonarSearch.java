package com.bluebed.sonar.gui.search;

import com.bluebed.sonar.Sonar;
import com.bluebed.sonar.SongInfo;
import com.bluebed.sonar.constructor.SonarJukebox;
import com.bluebed.sonar.gui.selector.SonarSongSelector;
import com.bluebed.sonar.util.ItemBuilder;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.inventoryaccess.abstraction.inventory.AnvilInventory;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;
import xyz.xenondevs.invui.window.AnvilWindow;
import xyz.xenondevs.invui.window.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.bluebed.sonar.command.SonarCommands.lastPlay;

public class SonarSearch {

    private static final List<Material> discs = List.of(
            Material.MUSIC_DISC_13, Material.MUSIC_DISC_CAT, Material.MUSIC_DISC_BLOCKS,
            Material.MUSIC_DISC_CHIRP, Material.MUSIC_DISC_FAR, Material.MUSIC_DISC_MALL,
            Material.MUSIC_DISC_MELLOHI, Material.MUSIC_DISC_STAL, Material.MUSIC_DISC_STRAD,
            Material.MUSIC_DISC_WARD, Material.MUSIC_DISC_11, Material.MUSIC_DISC_WAIT,
            Material.MUSIC_DISC_OTHERSIDE, Material.MUSIC_DISC_5, Material.MUSIC_DISC_PIGSTEP,
            Material.MUSIC_DISC_RELIC, Material.MUSIC_DISC_CREATOR, Material.MUSIC_DISC_PRECIPICE
    );

    public static void open(Player player, SonarJukebox jukebox) {
        PagedGui<Item> gui = PagedGui.items()
                .setStructure(
                        "# # # # # # # # #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "B # # # # # # # F"
                )
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', new BorderItem())
                .addIngredient('F', new ForwardItem())
                .addIngredient('B', new BackItem())
                .setContent(getItems("", jukebox))
                .build();

        ItemStack renameItem = new ItemBuilder()
                .toBuilder()
                .material(Material.NAME_TAG)
                .name(" ")
                .get();

        Gui upperGui = Gui.empty(3, 1);
        upperGui.setItem(0, new SimpleItem(renameItem));

        AnvilWindow.split()
                .setViewer(player)
                .setTitle("Search")
                .setUpperGui(upperGui)
                .setLowerGui(gui)
                .addRenameHandler(search -> gui.setContent(getItems(search, jukebox)))
                .open(player);
    }

    private static List<Item> getItems(String search, SonarJukebox jukebox) {
        List<Item> items = new ArrayList<>();

        String q = search.toLowerCase().trim();
        for (SongInfo info : Sonar.getCachedSongInfo()
                .values()
                .stream()
                .filter(m -> m.title().toLowerCase().contains(q)
                        || m.album().toLowerCase().contains(q)
                        || m.artist().toLowerCase().contains(q))
                .toList()) {
            items.add(new SongItem(jukebox, info));
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

    private static String doubleToTime(double value) {
        int totalSeconds = (int) Math.round(value);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

}
