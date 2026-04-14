package com.bluebed.sonar.gui.queue;

import com.bluebed.sonar.Sonar;
import com.bluebed.sonar.SongInfo;
import com.bluebed.sonar.constructor.SonarJukebox;
import com.bluebed.sonar.util.RelativeOffset;
import com.bluebed.sonar.util.TextDisplayUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;

import java.util.ArrayList;
import java.util.List;

public class SonarSongQueue {

    public static final RelativeOffset OFFSET_TITLE      = new RelativeOffset(0, 0,  0.0);
    public static final RelativeOffset OFFSET_BACKGROUND = new RelativeOffset(0, 0.25,  0.2);
    public static final double         QUEUE_ROW_STEP    = -0.2;

    private final SonarJukebox jukebox;
    private Location anchor;

    private final TextDisplay queueBackground;
    private final TextDisplay queueTitle;

    // Instance list — was static before, which broke multi-jukebox setups
    private final List<TextDisplay> queueDisplays = new ArrayList<>();

    public SonarSongQueue(SonarJukebox jukebox, Location anchor) {
        this.jukebox = jukebox;
        this.anchor  = anchor.clone();

        queueBackground = anchor.getWorld().spawn(OFFSET_BACKGROUND.apply(anchor), TextDisplay.class);
        queueBackground.text(Component.text(" "));
        queueBackground.setAlignment(TextDisplay.TextAlignment.LEFT);
        Transformation bgTransform = queueBackground.getTransformation();
        bgTransform.getScale().set(-15, -10, 0);
        queueBackground.setTransformation(bgTransform);
        TextDisplayUtil.setFixed(queueBackground);
        queueBackground.setBackgroundColor(Color.fromARGB(50, 0, 0, 0));

        queueTitle = anchor.getWorld().spawn(OFFSET_TITLE.apply(anchor), TextDisplay.class);
        Transformation titleTransform = queueTitle.getTransformation();
        titleTransform.getScale().set(0.4F);
        queueTitle.setTransformation(titleTransform);
        TextDisplayUtil.setFixed(queueTitle);
        TextDisplayUtil.removeBackground(queueTitle);
        queueTitle.text(Component.text("Current Queue"));

        updateQueue();
    }

    public void teleport(Location anchor) {
        this.anchor = anchor.clone();
        queueBackground.teleport(OFFSET_BACKGROUND.apply(anchor));
        queueTitle.teleport(OFFSET_TITLE.apply(anchor));
        updateQueue(); // respawns the row displays at the new anchor
    }

    public SongInfo getFirstQueue() {
        if (jukebox.getQueue().isEmpty()) return null;
        SongInfo song = jukebox.getQueue().removeFirst();
        updateQueue();
        return song;
    }

    public void updateQueue() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (TextDisplay display : queueDisplays) display.remove();
                queueDisplays.clear();

                List<SongInfo> q = jukebox.getQueue();
                int limit = Math.min(8, q.size());

                for (int i = 0; i < limit; i++) {
                    SongInfo song = q.get(i);

                    // Each row drops by QUEUE_ROW_STEP in the up axis
                    RelativeOffset rowOffset = OFFSET_TITLE.add(0, 0, QUEUE_ROW_STEP * (i + 1));
                    Location rowLoc = rowOffset.apply(anchor);

                    String text = "§7" + (i + 1) + ". §f" + song.title();
                    TextDisplay display = anchor.getWorld().spawn(rowLoc, TextDisplay.class);
                    display.text(Component.text(text));
                    TextDisplayUtil.setScale(display, text.length() > 24 ? 0.3F : 0.5F);
                    display.setAlignment(TextDisplay.TextAlignment.LEFT);
                    TextDisplayUtil.removeBackground(display);
                    TextDisplayUtil.setFixed(display);
                    queueDisplays.add(display);
                }
            }
        }.runTask(Sonar.getPlugin());
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void remove() {
        for (TextDisplay display : queueDisplays) display.remove();
        queueDisplays.clear();
        queueBackground.remove();
        queueTitle.remove();
    }
}