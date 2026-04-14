package com.bluebed.sonar.gui.settings;

import com.bluebed.sonar.constructor.SonarJukebox;
import com.bluebed.sonar.gui.positioning.PanelPositionListener;
import com.bluebed.sonar.gui.positioning.PanelPositionSession;
import com.bluebed.sonar.util.ItemBuilder;
import lombok.RequiredArgsConstructor;
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
import xyz.xenondevs.invui.window.Window;

public class SonarSettings {

    public static void open(Player player, SonarJukebox jukebox) {
        final PagedGui<Item> menu = PagedGui.items()
                .setStructure(
                        "# # # # # # # # #",
                        "# T Q C L D P x #",
                        "# # # # # # # # #"
                )
                .addIngredient('#', new BorderItem())
                .addIngredient('T', new TitleItem(jukebox))
                .addIngredient('Q', new QueueItem(jukebox))
                .addIngredient('C', new CoverItem(jukebox))
                .addIngredient('L', new LyricsItem(jukebox))
                .addIngredient('D', new DurationItem(jukebox))
                .addIngredient('P', new ParticlesItem(jukebox))
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .build();

        final Window window = Window.single()
                .setViewer(player)
                .setGui(menu)
                .setTitle("Settings")
                .build();

        window.open();
    }

    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

    /**
     * Toggles a boolean setting on left/right click and starts a position session
     * on shift-click. Subclasses just supply the key, material, description, and
     * permission node.
     */
    private abstract static class SettingItem extends AbstractItem {
        protected final SonarJukebox jukebox;
        protected final String key;
        protected final String permission;
        protected final String label;
        protected final String description;
        protected final Material material;

        protected SettingItem(SonarJukebox jukebox, String key, String permission,
                              String label, String description, Material material) {
            this.jukebox     = jukebox;
            this.key         = key;
            this.permission  = permission;
            this.label       = label;
            this.description = description;
            this.material    = material;
        }

        @Override
        public ItemProvider getItemProvider() {
            boolean enabled = jukebox.getSettings().get(key);
            return new ItemBuilder()
                    .toBuilder()
                    .material(material)
                    .lore(
                            "§7",
                            "§7" + description,
                            "§7Enabled: " + (enabled ? "§aYes" : "§cNo"),
                            "§7",
                            "§8Shift-click to reposition"
                    )
                    .name("§e" + label);
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
                                @NotNull InventoryClickEvent event) {
            if (!player.hasPermission(permission)) {
                player.sendMessage("§cYou do not have permission for this.");
                return;
            }

            if (clickType.isShiftClick()) {
                // Close inventory and start the live positioning session
                player.closeInventory();
                PanelPositionListener.startSession(
                        player,
                        new PanelPositionSession(player, jukebox, key)
                );
                return;
            }

            // Normal click — toggle
            boolean value = jukebox.getSettings().get(key);
            jukebox.setSetting(key, !value);

            player.sendMessage(!value ? "§aEnabled " + label : "§cDisabled " + label);
            player.playSound(player,
                    !value ? Sound.BLOCK_NOTE_BLOCK_BASS : Sound.ENTITY_ARROW_HIT_PLAYER,
                    1, 1);
            open(player, jukebox);
        }
    }

    // -------------------------------------------------------------------------
    // Border
    // -------------------------------------------------------------------------

    private static final class BorderItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder()
                    .toBuilder()
                    .material(Material.BLACK_STAINED_GLASS_PANE)
                    .name("§7");
        }

        @Override
        public void handleClick(@NotNull ClickType click, @NotNull Player player,
                                @NotNull InventoryClickEvent event) {
            event.setCancelled(true);
        }
    }

    // -------------------------------------------------------------------------
    // Panel items — one line each now that the logic lives in SettingItem
    // -------------------------------------------------------------------------

    public static final class TitleItem extends SettingItem {
        public TitleItem(SonarJukebox jukebox) {
            super(jukebox, "title", "sonar.settings.title",
                    "Title", "The title and artist/s of the song", Material.NAME_TAG);
        }
    }

    public static final class QueueItem extends SettingItem {
        public QueueItem(SonarJukebox jukebox) {
            super(jukebox, "queue", "sonar.settings.queue",
                    "Queue", "A list of songs in the queue", Material.BOOK);
        }
    }

    public static final class CoverItem extends SettingItem {
        public CoverItem(SonarJukebox jukebox) {
            super(jukebox, "cover", "sonar.settings.cover",
                    "Cover", "The album/song cover picture", Material.MAP);
        }
    }

    public static final class LyricsItem extends SettingItem {
        public LyricsItem(SonarJukebox jukebox) {
            super(jukebox, "lyrics", "sonar.settings.lyrics",
                    "Lyrics", "The lyrics of the song", Material.PAPER);
        }
    }

    public static final class DurationItem extends SettingItem {
        public DurationItem(SonarJukebox jukebox) {
            super(jukebox, "duration", "sonar.settings.duration",
                    "Duration", "The duration/playhead of the song", Material.CLOCK);
        }
    }

    public static final class ParticlesItem extends SettingItem {
        public ParticlesItem(SonarJukebox jukebox) {
            super(jukebox, "particle", "sonar.settings.particle",
                    "Particles", "The song particle/s", Material.GLOWSTONE);
        }
    }
}