package com.bluebed.sonar;

import com.bluebed.sonar.command.SonarCommands;
import com.bluebed.sonar.constructor.SonarJukebox;
import com.bluebed.sonar.constructor.SonarManager;
import com.bluebed.sonar.gui.positioning.PanelPositionListener;
import com.bluebed.sonar.listener.InteractListener;
import com.bluebed.sonar.util.ConfigUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.xenondevs.invui.InvUI;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public final class Sonar extends JavaPlugin {

    @Getter
    private static Sonar plugin;

    @Getter
    private static final Map<String, SongInfo> cachedSongInfo = new HashMap<>();

    @Getter
    private static String ffmpeg;
    @Getter
    private static String ffprobe;

    @Override
    public void onEnable() {
        plugin = this;

        InvUI.getInstance().setPlugin(this);

        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");

        getLogger().log(Level.INFO, "Machine OS: {0}, Version: {1}, Arch: {2}",
                new Object[]{osName, osVersion, osArch});

        BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service != null) {
            service.registerPlugin(new SonarPlugin());
            getLogger().info("Sonar enabled");
        } else {
            getLogger().severe("SimpleVoiceChat not found");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        ffmpeg = getConfig().getString("processes.ffmpeg");
        ffprobe = getConfig().getString("processes.ffprobe");

        ffmpeg  = makeExecutable(ffmpeg);
        ffprobe = makeExecutable(ffprobe);

        File songsDirectory = new File(getDataFolder(), "songs");
        if (!songsDirectory.exists()) songsDirectory.mkdirs();

        updateSongsIndex();

        Bukkit.getPluginManager().registerEvents(new InteractListener(), this);
        Bukkit.getPluginManager().registerEvents(new PanelPositionListener(), this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            LiteralArgumentBuilder<CommandSourceStack> command = SonarCommands.build();

            commands.registrar().register(command.build());
        });

        ConfigurationSection jukeboxes = getConfig().getConfigurationSection("jukeboxes");
        if (jukeboxes != null) {
            for (String key : jukeboxes.getKeys(false)) {
                SonarManager.createJukeboxFromConfigKey(key);
            }
        }
    }

    @Override
    public void onDisable() {
        for (SonarJukebox jukebox : SonarManager.getJukeboxes()) {
            jukebox.shutdown();
        }
    }

    public void reload() {
        reloadConfig();

        ffmpeg = getConfig().getString("processes.ffmpeg");
        ffprobe = getConfig().getString("processes.ffprobe");

        ffmpeg = makeExecutable(ffmpeg);
        ffprobe = makeExecutable(ffprobe);

        updateSongsIndex();
    }

    public void updateSongsIndex() {
        getPlugin().getLogger().info(new File(Sonar.getFfprobe()).exists() ? "FFProbe path exists!" : "FFProbe path is missing! This will lead to no song information or lyrics.");

        File songsDirectory = new File(getDataFolder(), "songs");
        if (!songsDirectory.exists()) return;
        File[] songs = songsDirectory.listFiles((dir, name) -> name.endsWith(".mp3"));
        if (songs == null) return;
        for (File song : songs) {
            if (cachedSongInfo.containsKey(song.getName())) continue;

            try {
                JsonObject songJson = getCachedSongInfo(song.getPath());
                if (songJson == null) continue;
                JsonObject format = songJson.getAsJsonObject("format");
                if (format == null) continue;
                JsonObject tags = format.getAsJsonObject("tags");
                if (tags == null) continue;
                String artist = tags.has("artist") ? tags.get("artist").getAsString() : "Unknown Artist";
                String album = tags.has("album") ? tags.get("album").getAsString() : "Unknown Album";
                String title = tags.has("title") ? tags.get("title").getAsString() : "Unknown Title";
                double length = format.has("duration") ? format.get("duration").getAsDouble() : 0;

                SongInfo songRecord = new SongInfo(song.getName(), title, album, artist, length, song.getPath());
                cachedSongInfo.put(song.getName(), songRecord);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static JsonObject getCachedSongInfo(String filePath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                Sonar.getFfprobe(),
                "-v", "quiet",
                "-print_format", "json",
                "-show_format",
                "-show_streams",
                filePath
        );

        Process process = pb.start();
        InputStream in = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line);
        }

        process.waitFor();

        JsonElement jsonElement = JsonParser.parseString(output.toString());

        if (!jsonElement.isJsonObject()) return null;

        return jsonElement.getAsJsonObject();
    }

    private String makeExecutable(String path) {
        try {
            File original = new File(path);
            File tmp = new File(System.getProperty("java.io.tmpdir"), original.getName());

            // Copy to /tmp if not already there or outdated
            if (!tmp.exists() || tmp.lastModified() < original.lastModified()) {
                Files.copy(original.toPath(), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            tmp.setExecutable(true);
            getLogger().info("Using " + original.getName() + " from temp: " + tmp.getAbsolutePath());
            return tmp.getAbsolutePath();
        } catch (IOException e) {
            getLogger().warning("Could not copy " + path + " to temp: " + e.getMessage());
            return path; // fall back to original, will likely still fail
        }
    }
}
