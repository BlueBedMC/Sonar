package com.bluebed.sonar.gui.settings;

import com.bluebed.sonar.constructor.SonarJukebox;
import com.bluebed.sonar.gui.positioning.PanelPositionListener;
import com.bluebed.sonar.gui.positioning.PanelPositionSession;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class SonarSettingsListener implements Listener {

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (!e.getView().getTitle().equals("Settings")) return;
        SonarSettings.removeJukebox(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!e.getView().getTitle().equals("Settings")) return;

        e.setCancelled(true);

        SonarJukebox jukebox = SonarSettings.getJukebox(player);

        if (jukebox == null) return;

        int slot = e.getRawSlot();

        switch (slot) {
            case 10 -> handle(player, jukebox, "title", "Title", e);
            case 11 -> handle(player, jukebox, "queue", "Queue", e);
            case 12 -> handle(player, jukebox, "cover", "Cover", e);
            case 13 -> handle(player, jukebox, "lyrics", "Lyrics", e);
            case 14 -> handle(player, jukebox, "duration", "Duration", e);
            case 15 -> handle(player, jukebox, "particle", "Particles", e);
        }
    }

    private void handle(Player player, SonarJukebox jukebox,
                        String key, String label, InventoryClickEvent e) {

        if (e.isShiftClick()) {
            player.closeInventory();
            PanelPositionListener.startSession(
                    player,
                    new PanelPositionSession(player, jukebox, key)
            );
            return;
        }

        if (jukebox == null) {
            player.sendMessage("§cThe jukebox provided was null. Try reopen the settings menu.");
            return;
        }

        boolean value = jukebox.getSettings().get(key);
        jukebox.setSetting(key, !value);

        player.sendMessage(!value ? "§aEnabled " + label : "§cDisabled " + label);
        player.playSound(player,
                !value ? Sound.ENTITY_ARROW_HIT_PLAYER : Sound.BLOCK_NOTE_BLOCK_BASS,
                1, 1);

        SonarSettings.open(player, jukebox);
    }
}