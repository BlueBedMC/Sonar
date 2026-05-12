package com.bluebed.sonar.gui.song;

import com.bluebed.sonar.util.RelativeOffset;
import lombok.Getter;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SonarSongCover {

    // -------------------------------------------------------------------------
    // Default offset for the top-left corner of the cover art.
    // Lines stack downward (negative up) from this point.
    // -------------------------------------------------------------------------
    public static final RelativeOffset OFFSET_ORIGIN = new RelativeOffset(0, 0, 0);

    private static final double SCALE      = 0.1;
    private static final double LINE_STEP  = 0.015 / 2.0; // vertical gap per line

    private final List<TextDisplay> cover = new ArrayList<>();

    public void setCover(Location anchor, TextComponent[] image) {
        World world = anchor.getWorld();

        for (int i = 0; i < image.length; i++) {
            TextComponent line = image[i];
            if (line == null) continue;

            // Each line drops by LINE_STEP in world-up from the origin offset
            RelativeOffset lineOffset = OFFSET_ORIGIN.add(0, 0, -(i * LINE_STEP));
            Location spawnLoc = lineOffset.apply(anchor);

            TextDisplay display = world.spawn(spawnLoc, TextDisplay.class);
            display.text(line);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setBackgroundColor(Color.BLACK);
            display.setLineWidth(1000);

            Transformation transformation = display.getTransformation();
            transformation.getScale().set(SCALE / 2);
            display.setTransformation(transformation);
            display.addScoreboardTag("sonar_jukebox");

            cover.add(display);
        }
    }

    public void teleport(Location anchor) {
        for (int i = 0; i < cover.size(); i++) {
            RelativeOffset lineOffset = OFFSET_ORIGIN.add(0, 0, -(i * LINE_STEP));
            cover.get(i).teleport(lineOffset.apply(anchor));
        }
    }

    public void remove() {
        for (TextDisplay display : cover) display.remove();
        cover.clear();
    }
}