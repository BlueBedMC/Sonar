package com.bluebed.sonar.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public enum BlockUtil {
    ;

    public static Block getClosestBlockOfType(Location center, Material type, int radius) {
        World world = center.getWorld();
        Block closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = world.getBlockAt(center.clone().add(x, y, z));

                    if (block.getType() != type) continue;

                    double distance = block.getLocation().distanceSquared(center);
                    if (distance < closestDistance) {
                        closest = block;
                        closestDistance = distance;
                    }
                }
            }
        }

        return closest;
    }
}
