package com.bluebed.sonar;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.*;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.*;

public class SonarPlugin implements VoicechatPlugin {
    @Getter @Setter
    private static VoicechatServerApi api;

    @Getter
    private static final String VOLUME_CATEGORY_ID = "sonar";

    @Override
    public String getPluginId() { return "sonar"; }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
    }

    public void onServerStarted(VoicechatServerStartedEvent event) {
        setApi(event.getVoicechat());

        VolumeCategory sonar = api.volumeCategoryBuilder()
                .setId(VOLUME_CATEGORY_ID)
                .setName("Sonar Jukeboxes")
                .setDescription("The jukeboxes made by the Sonar plugin")
                .setIcon(getIcon("Untitled.png"))
                .build();

        api.registerVolumeCategory(sonar);
    }

    @Nullable
    private int[][] getIcon(String path) {
        try {
            Enumeration<URL> resources = Plugin.class.getClassLoader().getResources(path);
            while (resources.hasMoreElements()) {
                BufferedImage bufferedImage = ImageIO.read(resources.nextElement().openStream());
                if (bufferedImage.getWidth() != 16) {
                    continue;
                }
                if (bufferedImage.getHeight() != 16) {
                    continue;
                }
                int[][] image = new int[16][16];
                for (int x = 0; x < bufferedImage.getWidth(); x++) {
                    for (int y = 0; y < bufferedImage.getHeight(); y++) {
                        image[x][y] = bufferedImage.getRGB(x, y);
                    }
                }
                return image;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}