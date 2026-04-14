package com.bluebed.sonar.util;

import com.bluebed.sonar.Sonar;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

public enum ConfigUtil {
    ;

    public static void saveLocation(String id, Location location) {
        Sonar sonar = Sonar.getPlugin();
        FileConfiguration config = sonar.getConfig();
        config.set("jukeboxes." + id + ".location", location);
        sonar.saveConfig();
    }

    public static Location getLocation(String configKey) {
        Sonar sonar = Sonar.getPlugin();
        FileConfiguration config = sonar.getConfig();
        return config.getLocation("jukeboxes." + configKey + ".location");
    }

}
