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

        Location startLoc = getTargetLocation();
        preview = player.getWorld().spawn(startLoc, TextDisplay.class);
        preview.text(
                Component.text("[ " + panelKey.toUpperCase() + " ]")
                        .color(TextColor.fromHexString("#FFFF55"))
        );
        preview.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);

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

        Location loc = getTargetLocation();
        // Face towards the player — opposite of the player's yaw
        loc.setYaw(player.getLocation().getYaw() + 180f);
        loc.setPitch(0);
        preview.teleport(loc);
    }

    // -------------------------------------------------------------------------
    // Confirm / Cancel
    // -------------------------------------------------------------------------

    /** Called on right-click. Saves the offset and cleans up. */
    public void confirm() {
        followTask.cancel();

        Location previewLoc = preview.getLocation();
        Location jukeboxLoc = jukebox.getLocation();

        // The yaw stored is the facing direction (player yaw + 180),
        // expressed as an offset relative to the jukebox's own yaw.
        float absoluteYaw = previewLoc.getYaw();
        float relativeYaw = absoluteYaw - jukeboxLoc.getYaw();

        RelativeOffset offset = worldToRelative(jukeboxLoc, previewLoc).withYaw(relativeYaw);
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

    private static RelativeOffset worldToRelative(Location base, Location target) {
        double yawRad = Math.toRadians(base.getYaw());

        double dx = target.getX() - base.getX();
        double dy = target.getY() - base.getY();
        double dz = target.getZ() - base.getZ();

        double fwdX = -Math.sin(yawRad);
        double fwdZ =  Math.cos(yawRad);

        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);

        double forward = dx * fwdX + dz * fwdZ;
        double right   = dx * rightX + dz * rightZ;

        return new RelativeOffset(forward, right, dy);
    }
}