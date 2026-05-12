package com.bluebed.sonar.gui.lyrics;

import com.bluebed.sonar.util.RelativeOffset;
import com.bluebed.sonar.util.TextDisplayUtil;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SonarLyrics {

    // -------------------------------------------------------------------------
    // Default offsets — forward/right/up relative to the anchor location's yaw.
    // Swap these out per-jukebox once you build the editor.
    // -------------------------------------------------------------------------
    public static final RelativeOffset OFFSET_PREVIOUS = new RelativeOffset(0, 0,  0.3);
    public static final RelativeOffset OFFSET_CURRENT  = new RelativeOffset(0, 0,  0.0);
    public static final RelativeOffset OFFSET_NEXT     = new RelativeOffset(0, 0, -0.3);

    private final TextDisplay previousLyrics;
    private final TextDisplay currentLyrics;
    private final TextDisplay nextLyrics;

    private final List<LyricLine> parsedLyrics = new ArrayList<>();
    private double current = 0;

    private static final Pattern LINE_PATTERN =
            Pattern.compile("^\\[(\\d{2}):(\\d{2}\\.\\d{2})\\](.*)$");

    public SonarLyrics(Location anchor, JsonObject lyricsObject) {
        previousLyrics = anchor.getWorld().spawn(OFFSET_PREVIOUS.apply(anchor), TextDisplay.class);
        TextDisplayUtil.removeBackground(previousLyrics);
        TextDisplayUtil.setScale(previousLyrics, 0.4F);
        TextDisplayUtil.setOpacity(previousLyrics, (byte) 64);
        TextDisplayUtil.setFixed(previousLyrics);
        previousLyrics.addScoreboardTag("sonar_jukebox");

        currentLyrics = anchor.getWorld().spawn(OFFSET_CURRENT.apply(anchor), TextDisplay.class);
        TextDisplayUtil.removeBackground(currentLyrics);
        TextDisplayUtil.setScale(currentLyrics, 0.6F);
        TextDisplayUtil.setFixed(currentLyrics);
        currentLyrics.addScoreboardTag("sonar_jukebox");

        nextLyrics = anchor.getWorld().spawn(OFFSET_NEXT.apply(anchor), TextDisplay.class);
        TextDisplayUtil.removeBackground(nextLyrics);
        TextDisplayUtil.setScale(nextLyrics, 0.4F);
        TextDisplayUtil.setOpacity(nextLyrics, (byte) 64);
        TextDisplayUtil.setFixed(nextLyrics);
        nextLyrics.addScoreboardTag("sonar_jukebox");

        if (lyricsObject.get("code") != null && lyricsObject.get("code").getAsInt() == 404) {
            currentLyrics.text(Component.text(lyricsObject.get("message").getAsString())
                    .color(TextColor.color(255, 0, 0)));
            return;
        }

        if (lyricsObject.get("syncedLyrics") == null
                || lyricsObject.get("syncedLyrics").isJsonNull()
                || lyricsObject.get("syncedLyrics").getAsString().isEmpty()) {
            currentLyrics.text(Component.text("§cSomething went wrong with the lyrics."));
            return;
        }

        parsedLyrics.addAll(parse(lyricsObject.get("syncedLyrics").getAsString()));
    }

    public void teleport(Location anchor) {
        previousLyrics.teleport(OFFSET_PREVIOUS.apply(anchor));
        currentLyrics.teleport(OFFSET_CURRENT.apply(anchor));
        nextLyrics.teleport(OFFSET_NEXT.apply(anchor));
    }

    // -------------------------------------------------------------------------
    // Playback sync
    // -------------------------------------------------------------------------

    public void setCurrentTime(double currentTime) {
        this.current = currentTime;

        LyricLine prev = getPreviousLine();
        LyricLine curr = getCurrentLine();
        LyricLine next = getNextLine();

        if (prev != null) previousLyrics.text(Component.text(prev.text()).color(TextColor.fromHexString("#AAAAAA")));
        if (curr != null) currentLyrics.text(Component.text(curr.text()));
        if (next != null) nextLyrics.text(Component.text(next.text()).color(TextColor.fromHexString("#AAAAAA")));
    }

    // -------------------------------------------------------------------------
    // Line lookup
    // -------------------------------------------------------------------------

    private LyricLine getCurrentLine() {
        int idx = findIndex(current);
        return idx >= 0 ? parsedLyrics.get(idx) : null;
    }

    private LyricLine getPreviousLine() {
        int idx = findIndex(current);
        return idx > 0 ? parsedLyrics.get(idx - 1) : null;
    }

    private LyricLine getNextLine() {
        int idx = findIndex(current);
        return (idx >= 0 && idx + 1 < parsedLyrics.size()) ? parsedLyrics.get(idx + 1) : null;
    }

    private int findIndex(double currentTime) {
        int idx = Collections.binarySearch(
                parsedLyrics,
                new LyricLine(currentTime, ""),
                Comparator.comparingDouble(LyricLine::time)
        );
        if (idx >= 0) return idx;
        idx = -idx - 2;
        return (idx >= 0 && idx < parsedLyrics.size()) ? idx : -1;
    }

    private List<LyricLine> parse(String rawLyrics) {
        List<LyricLine> lyrics = new ArrayList<>();
        for (String line : rawLyrics.split("\\r?\\n")) {
            Matcher m = LINE_PATTERN.matcher(line);
            if (!m.matches()) continue;
            int minutes = Integer.parseInt(m.group(1));
            double seconds = Double.parseDouble(m.group(2));
            String text = m.group(3).trim();
            lyrics.add(new LyricLine(minutes * 60 + seconds, text));
        }
        Collections.sort(lyrics);
        return lyrics;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void remove() {
        previousLyrics.remove();
        currentLyrics.remove();
        nextLyrics.remove();
    }

    // -------------------------------------------------------------------------
    // Data
    // -------------------------------------------------------------------------

    public record LyricLine(double time, String text) implements Comparable<LyricLine> {
        @Override
        public int compareTo(LyricLine other) {
            return Double.compare(this.time, other.time);
        }
    }
}