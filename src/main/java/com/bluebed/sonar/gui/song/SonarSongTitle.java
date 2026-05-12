package com.bluebed.sonar.gui.song;

import com.bluebed.sonar.util.RelativeOffset;
import com.bluebed.sonar.util.TextDisplayUtil;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;

import static com.bluebed.sonar.util.RelativeOffset.withYaw;

public class SonarSongTitle {

    // -------------------------------------------------------------------------
    // Default offsets — forward/right/up relative to the anchor's yaw.
    // -------------------------------------------------------------------------
    public static final RelativeOffset OFFSET_TITLE  = new RelativeOffset(0, 0,  0.0);
    public static final RelativeOffset OFFSET_ARTIST = new RelativeOffset(0, 0, -0.2);

    private final TextDisplay title;
    private final TextDisplay artist;

    public SonarSongTitle(Location anchor, JsonObject info) {
        JsonObject format = info.getAsJsonObject("format");
        JsonObject tags   = format.getAsJsonObject("tags");
        String artistStr = get(tags, "artist", "Unknown Artist");
        String albumStr  = get(tags, "album",  "Unknown Album");
        String titleStr  = get(tags, "title",  "Unknown Title");

        title = anchor.getWorld().spawn(OFFSET_TITLE.apply(anchor), TextDisplay.class);
        title.setAlignment(TextDisplay.TextAlignment.LEFT);
        title.setBackgroundColor(Color.fromRGB(0));
        title.text(Component.text(titleStr));
        TextDisplayUtil.setScale(title, titleStr.length() > 24 ? 0.6F : 1F);
        TextDisplayUtil.removeBackground(title);
        title.addScoreboardTag("sonar_jukebox");

        artist = anchor.getWorld().spawn(OFFSET_ARTIST.apply(anchor), TextDisplay.class);
        artist.setAlignment(TextDisplay.TextAlignment.CENTER);
        artist.setBackgroundColor(Color.fromRGB(0));
        Transformation transformation = artist.getTransformation();
        transformation.getScale().set(0.4);
        artist.setTransformation(transformation);
        artist.text(
                Component.text(albumStr)
                        .append(Component.text(" • ").color(TextColor.fromHexString("#AAAAAA")))
                        .append(Component.text(artistStr.replace("/", ", ")).color(TextColor.color(255, 255, 255)))
        );
        artist.setLineWidth(1000);
        TextDisplayUtil.removeBackground(artist);
        artist.addScoreboardTag("sonar_jukebox");
    }

    public void teleport(Location anchor) {
        title.teleport(withYaw(OFFSET_TITLE.apply(anchor), anchor));
        artist.teleport(withYaw(OFFSET_ARTIST.apply(anchor), anchor));
    }

    public void remove() {
        title.remove();
        artist.remove();
    }

    private String get(JsonObject info, String key, String def) {
        return info != null && info.has(key) ? info.get(key).getAsString() : def;
    }
}