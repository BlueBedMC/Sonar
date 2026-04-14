package com.bluebed.sonar.gui.positioning;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks active {@link PanelPositionSession}s and routes right/left clicks to them.
 * Register this listener once on plugin enable.
 */
public class PanelPositionListener implements Listener {

    // One session per player at a time
    private static final Map<UUID, PanelPositionSession> SESSIONS = new HashMap<>();

    // -------------------------------------------------------------------------
    // Session management (called from SonarSettings)
    // -------------------------------------------------------------------------

    public static void startSession(Player player, PanelPositionSession session) {
        // If a session is already running for this player, cancel it first
        PanelPositionSession existing = SESSIONS.get(player.getUniqueId());
        if (existing != null) existing.cancel();
        SESSIONS.put(player.getUniqueId(), session);
    }

    public static boolean hasSession(Player player) {
        return SESSIONS.containsKey(player.getUniqueId());
    }

    // -------------------------------------------------------------------------
    // Events
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        PanelPositionSession session = SESSIONS.get(player.getUniqueId());
        if (session == null) return;

        Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            SESSIONS.remove(player.getUniqueId());
            session.confirm();
        } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            SESSIONS.remove(player.getUniqueId());
            session.cancel();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PanelPositionSession session = SESSIONS.remove(event.getPlayer().getUniqueId());
        if (session != null) session.cancel();
    }
}