package com.bluebed.sonar.gui.song;

import com.bluebed.sonar.Sonar;
import com.bluebed.sonar.util.RelativeOffset;
import com.bluebed.sonar.util.TextDisplayUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;

public class SonarSongDuration {

    // -------------------------------------------------------------------------
    // Default offsets — forward/right/up relative to the anchor's yaw.
    // "right" of -0.9 / +0.9 puts the timestamps either side of centre.
    // The slider starts at right = -1 and moves to right = +1 as the song plays.
    // -------------------------------------------------------------------------
    public static final RelativeOffset OFFSET_CURRENT_TIME = new RelativeOffset(0, -0.9,  0.0);
    public static final RelativeOffset OFFSET_DURATION     = new RelativeOffset(0,  0.9,  0.0);
    public static final RelativeOffset OFFSET_BACKGROUND   = new RelativeOffset(0,  0.0,  0.0);
    public static final RelativeOffset OFFSET_SLIDER_START = new RelativeOffset(0, -1.0,  0.06);

    private static final double SLIDER_RANGE = 2.0 - 0.01; // matches left/right extents

    private final TextDisplay currentTimeDisplay;
    private final TextDisplay durationDisplay;
    private final TextDisplay background;
    private final TextDisplay slider;

    private double current = 0;
    private final double length;

    private Location anchor;

    public SonarSongDuration(Location anchor, double length) {
        this.length = length;
        this.anchor = anchor.clone();

        currentTimeDisplay = anchor.getWorld().spawn(OFFSET_CURRENT_TIME.apply(anchor), TextDisplay.class);
        currentTimeDisplay.setAlignment(TextDisplay.TextAlignment.LEFT);
        currentTimeDisplay.setBackgroundColor(Color.fromRGB(0));
        currentTimeDisplay.text(Component.text(doubleToTime(current)));
        TextDisplayUtil.removeBackground(currentTimeDisplay);
        TextDisplayUtil.setScale(currentTimeDisplay, 0.2F);
        currentTimeDisplay.addScoreboardTag("sonar_jukebox");

        durationDisplay = anchor.getWorld().spawn(OFFSET_DURATION.apply(anchor), TextDisplay.class);
        durationDisplay.setAlignment(TextDisplay.TextAlignment.RIGHT);
        durationDisplay.setBackgroundColor(Color.fromRGB(0));
        durationDisplay.text(Component.text(doubleToTime(length)));
        TextDisplayUtil.removeBackground(durationDisplay);
        TextDisplayUtil.setScale(durationDisplay, 0.2F);
        durationDisplay.addScoreboardTag("sonar_jukebox");

        background = anchor.getWorld().spawn(OFFSET_BACKGROUND.apply(anchor), TextDisplay.class);
        background.text(Component.text("                ")
                .style(Style.style()
                        .decoration(TextDecoration.BOLD, true)
                        .decoration(TextDecoration.STRIKETHROUGH, true)
                        .color(TextColor.fromHexString("#AAAAAA"))
                        .build()
                )
        );
        background.setAlignment(TextDisplay.TextAlignment.CENTER);
        TextDisplayUtil.removeBackground(background);
        background.addScoreboardTag("sonar_jukebox");

        slider = anchor.getWorld().spawn(OFFSET_SLIDER_START.apply(anchor), TextDisplay.class);
        slider.text(Component.text("■").color(TextColor.fromHexString("#FFFF55")));
        slider.setAlignment(TextDisplay.TextAlignment.LEFT);
        TextDisplayUtil.removeBackground(slider);
        TextDisplayUtil.setScale(slider, 0.5F);
        slider.addScoreboardTag("sonar_jukebox");
    }

    public void teleport(Location anchor) {
        this.anchor = anchor.clone();
        currentTimeDisplay.teleport(OFFSET_CURRENT_TIME.apply(anchor));
        durationDisplay.teleport(OFFSET_DURATION.apply(anchor));
        background.teleport(OFFSET_BACKGROUND.apply(anchor));
        slider.teleport(OFFSET_SLIDER_START.apply(anchor));
    }

    // -------------------------------------------------------------------------
    // Playback sync
    // -------------------------------------------------------------------------

    public void setCurrentTime(double currentTime) {
        this.current = currentTime;
        currentTimeDisplay.text(Component.text(doubleToTime(currentTime)));

        // Recompute slider position along the "right" axis relative to the anchor's yaw.
        // progress goes 0 → 1, mapped to right = -1 → +1 (SLIDER_RANGE wide).
        double progress = length > 0 ? Math.min(currentTime / length, 1.0) : 0;
        double rightOffset = -1.0 + progress * SLIDER_RANGE;

        Location sliderLoc = OFFSET_SLIDER_START
                .add(0, rightOffset - (-1.0), 0) // shift right by how far along we are
                .apply(anchor);

        Bukkit.getScheduler().runTask(Sonar.getPlugin(), () -> slider.teleport(sliderLoc));
    }

    public void remove() {
        currentTimeDisplay.remove();
        durationDisplay.remove();
        background.remove();
        slider.remove();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String doubleToTime(double value) {
        int totalSeconds = (int) Math.round(value);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}