package com.bluebed.sonar.gui.song;

import com.bluebed.sonar.Sonar;
import com.google.gson.JsonObject;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Getter
public class SonarSongDisplay {
    private final SonarSongCover cover;
    private final SonarSongTitle title;
    private final SonarSongDuration duration;

    public SonarSongDisplay(Location location, String path, JsonObject info, boolean showCover, boolean showTitle, boolean showDuration) {
        this.cover = (showCover ? new SonarSongCover() : null);
        this.title = (showTitle ? new SonarSongTitle(location.clone(), info) : null);

        JsonObject format = info.getAsJsonObject("format");
        double length = format.has("duration") ? format.get("duration").getAsDouble() : 0;
        this.duration = (showDuration ? new SonarSongDuration(location.clone().subtract(0, 0.5, 0), length) : null);

        Bukkit.getScheduler().runTaskAsynchronously(Sonar.getPlugin(),() -> {
            TextComponent[] components = getImage(path);
            Bukkit.getScheduler().runTask(Sonar.getPlugin(), () -> setCoverImage(location.clone().add(0, 1, 0), components));
        });
    }

    public void setCoverImage(Location location, TextComponent[] coverImage) {
        if (cover == null) return;

        this.cover.setCover(location.clone().add(0, 0.35, 0), coverImage);
    }

    public void remove() {
        if (cover != null) cover.remove();
        if (title != null) title.remove();
        if (duration != null) duration.remove();
    }

    private TextComponent[] getImage(String path) {
        try {
            File file = new File("cover.jpg");
            if (file.exists()) file.delete();

            ProcessBuilder pb = new ProcessBuilder(
                    Sonar.getFfmpeg(), "-i", path,
                    "-an", "-vcodec", "copy", "-map", "0:v:0", "cover.jpg"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exit = process.waitFor();

            if (exit != 0) {
                System.err.println("FFmpeg failed to extract cover art for " + path);
                return new TextComponent[]{ Component.text("No cover art found") };
            }

            BufferedImage img = ImageIO.read(new File("cover.jpg"));
            if (img == null) {
                System.err.println("ImageIO failed to read cover.jpg");
                return new TextComponent[]{ Component.text("No image data") };
            }

            int width = 128;
            int height = 128;
            BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.drawImage(img, 0, 0, width, height, null);
            g.dispose();

            TextComponent[] lines = new TextComponent[height];
            for (int y = 0; y < height; y++) {
                lines[y] = Component.text("");
                for (int x = 0; x < width; x++) {
                    int rgb = resized.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int gCol = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    lines[y] = lines[y].append(Component.text("■").color(TextColor.color(r, gCol, b)));
                }
            }

            return lines;
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            return new TextComponent[]{ Component.text("Error loading image") };
        }
    }

    public void teleport(String key, Location anchor) {
        switch (key) {
            case "title" -> { if (title != null) title.teleport(anchor); }
            case "duration" -> { if (duration != null) duration.teleport(anchor); }
            case "cover" -> { if (cover != null) cover.teleport(anchor); }
        }
    }
}
