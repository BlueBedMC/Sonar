package com.bluebed.sonar.util;

import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * Represents an offset relative to a location's yaw (forward/right/up),
 * plus an optional extra yaw rotation applied to the spawned display itself.
 *
 * - "forward" is the direction the location is facing (negative Z in Minecraft terms)
 * - "right"   is 90° clockwise from forward
 * - "up"      is always world Y
 * - "yaw"     is an additional rotation (degrees) applied to the panel's facing direction,
 *             on top of the jukebox's own yaw. 0 = face same direction as jukebox.
 *
 * Use {@link #apply(Location)} to get the world-space Location after applying the offset.
 */
public record RelativeOffset(double forward, double right, double up, float yaw) {

    public static final RelativeOffset ZERO = new RelativeOffset(0, 0, 0, 0);

    /** Backwards-compatible constructor for code that doesn't care about yaw. */
    public RelativeOffset(double forward, double right, double up) {
        this(forward, right, up, 0f);
    }

    /**
     * Returns a new Location offset from the base by this RelativeOffset.
     * The result's yaw is the base yaw plus this offset's extra yaw, so
     * TextDisplays face the right direction automatically.
     */
    public Location apply(Location base) {
        double yawRad = Math.toRadians(base.getYaw());

        // Forward vector (the direction the location faces)
        Vector fwd   = new Vector(-Math.sin(yawRad), 0,  Math.cos(yawRad));
        // Right vector (90° clockwise from forward)
        Vector right = new Vector( Math.cos(yawRad), 0,  Math.sin(yawRad));
        // Up is always world Y
        Vector up    = new Vector(0, 1, 0);

        Location result = base.clone().add(
                fwd.clone().multiply(forward)
                        .add(right.clone().multiply(this.right))
                        .add(up.clone().multiply(this.up))
        );

        // Base yaw + panel-specific yaw offset
        result.setYaw(base.getYaw() + this.yaw);
        result.setPitch(0);

        return result;
    }

    /** Convenience — shift an existing offset by additional amounts, preserving yaw. */
    public RelativeOffset add(double forward, double right, double up) {
        return new RelativeOffset(this.forward + forward, this.right + right, this.up + up, this.yaw);
    }

    /** Returns a copy with a different yaw. */
    public RelativeOffset withYaw(float yaw) {
        return new RelativeOffset(this.forward, this.right, this.up, yaw);
    }

    public static Location withYaw(Location base, Location anchor) {
        base.setYaw(anchor.getYaw());
        return base;
    }
}