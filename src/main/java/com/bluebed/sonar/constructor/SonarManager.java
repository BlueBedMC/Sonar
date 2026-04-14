package com.bluebed.sonar.constructor;

import com.bluebed.sonar.util.ConfigUtil;
import org.bukkit.Location;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SonarManager {
    private static final Map<String, SonarJukebox> JUKEBOXES = new HashMap<>();

    public static SonarCodes createJukebox(String id, Location location) {
        if (JUKEBOXES.get(id) != null) return SonarCodes.ALREADY_EXISTS;

        SonarJukebox jukebox = new SonarJukebox(id, location);
        JUKEBOXES.put(id, jukebox);

        return SonarCodes.SUCCESS;
    }

    public static SonarCodes createJukeboxFromConfigKey(String configKey) {
        if (JUKEBOXES.get(configKey) != null) return SonarCodes.ALREADY_EXISTS;

        SonarJukebox jukebox = new SonarJukebox(configKey, ConfigUtil.getLocation(configKey));
        JUKEBOXES.put(configKey, jukebox);

        return SonarCodes.SUCCESS;
    }

    public static SonarJukebox getJukebox(String id) {
        return JUKEBOXES.get(id);
    }

    public static SonarJukebox getClosestJukebox(Location location, int radius) {
        for (SonarJukebox jukebox : JUKEBOXES.values()) {
            if (location.distance(jukebox.getLocation()) < radius) return jukebox;
        }
        return null;
    }

    public static Collection<SonarJukebox> getJukeboxes() {
        return JUKEBOXES.values();
    }
}
