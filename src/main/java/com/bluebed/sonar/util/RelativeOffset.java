package com.bluebed.sonar.util;

import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * Represents an offset relative to a location's yaw (forward/right/up).
 *
 * - "forward" is the direction the location is facing (negative Z in Minecraft terms)
 * - "right"   is 90° clockwise from forward
 * - "up"      is always world Y
 *
 * Use {@link #apply(Location)} to get the world-space Location after applying the offset.
 * This means all GUI panels look correct regardless of which direction the jukebox faces.
 */
public record RelativeOffset(double forward, double right, double up) {

    public static final RelativeOffset ZERO = new RelativeOffset(0, 0, 0);

    /**
     * Returns a new Location offset from the base by this RelativeOffset,
     * preserving the base yaw/pitch so TextDisplays face the same way.
     */
    public Location apply(Location base) {
        double yawRad = Math.toRadians(base.getYaw());

        // Forward vector (the direction the location faces)
        Vector fwd = new Vector(-Math.sin(yawRad), 0, Math.cos(yawRad));
        // Right vector (90° clockwise from forward)
        Vector right = new Vector(Math.cos(yawRad), 0, Math.sin(yawRad));
        // Up is always world Y
        Vector up = new Vector(0, 1, 0);

        Location result = base.clone().add(
                fwd.clone().multiply(forward)
                        .add(right.clone().multiply(this.right))
                        .add(up.clone().multiply(this.up))
        );

        result.setYaw(base.getYaw());
        result.setPitch(0);

        return result;
    }

    /** Convenience — shift an existing offset by additional amounts. */
    public RelativeOffset add(double forward, double right, double up) {
        return new RelativeOffset(this.forward + forward, this.right + right, this.up + up);
    }

    public static Location withYaw(Location base, Location anchor) {
        base.setYaw(anchor.getYaw());
        return base;
    }
}