package com.bluebed.sonar.gui.positioning;

import com.bluebed.sonar.Sonar;
import com.bluebed.sonar.constructor.SonarJukebox;
import com.bluebed.sonar.util.RelativeOffset;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;

/**
 * Spawns a preview TextDisplay that follows 3 blocks in front of the player's eyes.
 * Right-click confirms and saves the offset; left-click cancels.
 *
 * The saved value is a {@link RelativeOffset} computed from the jukebox location to
 * wherever the player confirmed, expressed in the jukebox's yaw-relative axes.
 * This means the panel will always appear in the same relative position regardless
 * of which direction the jukebox faces.
 */
public class PanelPositionSession {

    private static final double PREVIEW_DISTANCE = 3.0;

    private final Player player;
    private final SonarJukebox jukebox;
    private final String panelKey;

    private final TextDisplay preview;
    private final BukkitTask followTask;

    public PanelPositionSession(Player player, SonarJukebox jukebox, String panelKey) {
        this.player   = player;
        this.jukebox  = jukebox;
        this.panelKey = panelKey;

        // Spawn the initial preview 3 blocks ahead
        Location startLoc = getTargetLocation();
        preview = player.getWorld().spawn(startLoc, TextDisplay.class);
        preview.text(
                Component.text("[ " + panelKey.toUpperCase() + " ]")
                        .color(TextColor.fromHexString("#FFFF55"))
        );
        preview.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);

        // Tick every 1 tick — keeps the preview snappy
        followTask = Sonar.getPlugin().getServer().getScheduler()
                .runTaskTimer(Sonar.getPlugin(), this::tick, 0L, 1L);

        player.sendMessage("§ePositioning §6" + panelKey + "§e — §aRight-click §eto confirm, §cLeft-click §eto cancel.");
    }

    // -------------------------------------------------------------------------
    // Tick
    // -------------------------------------------------------------------------

    private void tick() {
        if (!player.isOnline()) {
            cancel();
            return;
        }
        preview.teleport(getTargetLocation());
    }

    // -------------------------------------------------------------------------
    // Confirm / Cancel
    // -------------------------------------------------------------------------

    /** Called on right-click. Saves the offset and cleans up. */
    public void confirm() {
        followTask.cancel();

        Location previewLoc  = preview.getLocation();
        Location jukeboxLoc  = jukebox.getLocation();

        // Convert the world-space difference into jukebox-yaw-relative axes
        RelativeOffset offset = worldToRelative(jukeboxLoc, previewLoc);
        jukebox.setPanelOffset(panelKey, offset);

        preview.remove();

        player.sendMessage("§aPanel §6" + panelKey + "§a position saved!");
    }

    /** Called on left-click. Discards the preview with no changes. */
    public void cancel() {
        followTask.cancel();
        preview.remove();
        player.sendMessage("§cPositioning cancelled.");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** 3 blocks straight ahead of the player's eye position, at eye height. */
    private Location getTargetLocation() {
        Location eye = player.getEyeLocation();
        return eye.clone().add(eye.getDirection().multiply(PREVIEW_DISTANCE));
    }

    /**
     * Converts a world-space target location into a {@link RelativeOffset} expressed
     * in the forward/right/up axes of the base location's yaw.
     *
     *   forward = dot(delta, fwd)
     *   right   = dot(delta, right)
     *   up      = delta.y
     */
    private static RelativeOffset worldToRelative(Location base, Location target) {
        double yawRad = Math.toRadians(base.getYaw());

        double dx = target.getX() - base.getX();
        double dy = target.getY() - base.getY();
        double dz = target.getZ() - base.getZ();

        // Forward unit vector (same as in RelativeOffset.apply)
        double fwdX = -Math.sin(yawRad);
        double fwdZ =  Math.cos(yawRad);

        // Right unit vector
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);

        double forward = dx * fwdX   + dz * fwdZ;
        double right   = dx * rightX + dz * rightZ;

        return new RelativeOffset(forward, right, dy);
    }
}