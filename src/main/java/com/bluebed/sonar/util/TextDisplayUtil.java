package com.bluebed.sonar.util;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

public enum TextDisplayUtil {
    ;

    public static void removeBackground(TextDisplay textDisplay) {
        textDisplay.setBackgroundColor(Color.fromARGB(0, 255, 255, 255));
    }

    public static void setFixed(TextDisplay textDisplay) {
        textDisplay.setBillboard(Display.Billboard.FIXED);
        textDisplay.getLocation().setPitch(0);
    }

    public static void setScale(TextDisplay textDisplay, float scale) {
        Transformation transformation = textDisplay.getTransformation();
        transformation.getScale().set(scale);
        textDisplay.setTransformation(transformation);
    }

    public static void setScale(TextDisplay textDisplay, float x, float y, float z) {
        Transformation transformation = textDisplay.getTransformation();
        transformation.getScale().set(x, y, z);
        textDisplay.setTransformation(transformation);
    }

    public static Vector3f getScale(TextDisplay textDisplay) {
        Transformation transformation = textDisplay.getTransformation();
        return transformation.getScale();
    }

    public static void setOpacity(TextDisplay textDisplay, byte v) {
        textDisplay.setTextOpacity(v);
    }

    public static void setRotation(TextDisplay display, Location anchor) {
        display.setRotation(anchor.getYaw(), 0);
    }
}
