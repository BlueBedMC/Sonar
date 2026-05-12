package com.bluebed.sonar.constructor;

import com.bluebed.sonar.Sonar;
import com.bluebed.sonar.SonarPlugin;
import com.bluebed.sonar.SongInfo;
import com.bluebed.sonar.gui.lyrics.SonarLyrics;
import com.bluebed.sonar.gui.queue.SonarSongQueue;
import com.bluebed.sonar.gui.song.SonarSongDisplay;
import com.bluebed.sonar.util.ConfigUtil;
import com.bluebed.sonar.util.RelativeOffset;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static com.bluebed.sonar.util.BlockUtil.getClosestBlockOfType;

@Getter
public class SonarJukebox {

    private final String id;
    private Location location;

    private final Map<String, Boolean> settings = new HashMap<>();

    private final Map<String, RelativeOffset> panelOffsets = new HashMap<>();

    private SonarSongDisplay sonarSong;
    private SonarLyrics sonarLyrics;
    private SonarSongQueue sonarSongQueue;

    private volatile float volume = 0.5f;

    private final List<SongInfo> queue = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public SonarJukebox(String id, Location location) {
        this.id = id;
        this.location = location;

        settings.put("title",    false);
        settings.put("queue",    false);
        settings.put("cover",    false);
        settings.put("lyrics",   false);
        settings.put("duration", false);
        settings.put("particle", false);

        load();
    }

    // -------------------------------------------------------------------------
    // Settings helpers
    // -------------------------------------------------------------------------

    private boolean setting(String key) {
        return settings.getOrDefault(key, false);
    }

    // -------------------------------------------------------------------------
    // Config load / save
    // -------------------------------------------------------------------------

    public void load() {
        FileConfiguration config = Sonar.getPlugin().getConfig();
        ConfigurationSection section = config.getConfigurationSection("jukeboxes." + id);

        if (section == null) {
            save();
            return;
        }

        location = ConfigUtil.getLocation(id);
        settings.replaceAll((key, ignored) -> section.getBoolean(key + ".enabled", false));

        // Load per-panel offsets stored as forward/right/up doubles under each key
        for (String key : settings.keySet()) {
            ConfigurationSection off = section.getConfigurationSection(key + ".offset");
            if (off != null) {
                panelOffsets.put(key, new RelativeOffset(
                        off.getDouble("forward", 0),
                        off.getDouble("right",   0),
                        off.getDouble("up",      0),
                        (float) off.getDouble("yaw", 0)
                ));
            }
        }
    }

    public void save() {
        FileConfiguration config = Sonar.getPlugin().getConfig();

        ConfigUtil.saveLocation(id, location);

        for (Map.Entry<String, Boolean> entry : settings.entrySet()) {
            config.set("jukeboxes." + id + "." + entry.getKey() + ".enabled", entry.getValue());
        }

        // Persist per-panel offsets
        for (Map.Entry<String, RelativeOffset> entry : panelOffsets.entrySet()) {
            String base = "jukeboxes." + id + "." + entry.getKey() + ".offset.";
            config.set(base + "forward", entry.getValue().forward());
            config.set(base + "right",   entry.getValue().right());
            config.set(base + "up",      entry.getValue().up());
            config.set(base + "yaw", (double) entry.getValue().yaw());
        }

        Sonar.getPlugin().saveConfig();
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void addSong(SongInfo song) {
        getQueue().add(song);
        if (sonarSongQueue != null) sonarSongQueue.updateQueue();
    }

    // -------------------------------------------------------------------------
    // Panel offset API
    // -------------------------------------------------------------------------

    /**
     * Returns the saved offset for a panel, or {@link RelativeOffset#ZERO} if none
     * has been set yet, so panels always have a sane default position.
     */
    public RelativeOffset getPanelOffset(String key) {
        return panelOffsets.getOrDefault(key, RelativeOffset.ZERO);
    }

    /**
     * Saves a new offset for the given panel, persists it to config, and immediately
     * repositions any live display for that panel.
     * Called by {@code PanelPositionSession} when the player confirms placement.
     */
    public void setPanelOffset(String key, RelativeOffset offset) {
        panelOffsets.put(key, offset);
        save();
        applyPanelOffsetLive(key, offset);
    }

    /**
     * Resolves a panel's world-space anchor by applying its saved offset to the
     * jukebox location. Falls back to the jukebox location itself when no offset
     * has been set yet (since ZERO.apply() == the base location).
     */
    public Location getPanelLocation(String key) {
        return getPanelOffset(key).apply(location);
    }

    /**
     * Moves a live display to its new anchor after the player confirms placement.
     * Only panels that are currently active need to be moved.
     */
    private void applyPanelOffsetLive(String key, RelativeOffset offset) {
        Location anchor = offset.apply(location);
        Bukkit.getScheduler().runTask(Sonar.getPlugin(), () -> {
            switch (key) {
                case "lyrics" -> {
                    if (sonarLyrics != null) sonarLyrics.teleport(anchor);
                }
                case "queue" -> {
                    if (sonarSongQueue != null) sonarSongQueue.teleport(anchor);
                }
                case "title", "cover", "duration" -> {
                    if (sonarSong != null) sonarSong.teleport(key, anchor);
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Settings API
    // -------------------------------------------------------------------------

    /**
     * Updates a setting, persists it to config, and immediately applies the change
     * so it takes effect mid-song without needing a restart.
     *
     * @param key   one of: "title", "queue", "cover", "lyrics", "duration", "particle"
     * @param value true to enable, false to disable
     */
    public void setSetting(String key, boolean value) {
        if (!settings.containsKey(key)) {
            Sonar.getPlugin().getLogger().warning("Unknown setting: " + key);
            return;
        }

        settings.put(key, value);
        save();
        applySettingLive(key, value);
    }

    /**
     * Applies a single setting change immediately while a song may be playing.
     */
    private void applySettingLive(String key, boolean enabled) {
        Sonar plugin = Sonar.getPlugin();
        boolean isPlaying = !activePlayers.isEmpty();

        switch (key) {
            case "particle" -> {
                if (enabled && isPlaying) {
                    if (particleTask == null) {
                        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                            Block block = getClosestBlockOfType(location, Material.JUKEBOX, 5);
                            if (block != null && block.getState() instanceof Jukebox) {
                                Location loc = block.getLocation();
                                loc.getWorld().spawnParticle(
                                        Particle.NOTE,
                                        loc.clone().add(0.5, 1.2, 0.5),
                                        1, 0, 0, 0, 1
                                );
                            }
                        }, 0L, 20L);
                    }
                } else {
                    if (particleTask != null) {
                        particleTask.cancel();
                        particleTask = null;
                    }
                }
            }

            case "lyrics" -> {
                if (enabled && isPlaying && lastSongJson != null) {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> fetchLyrics(lastSongJson));
                } else {
                    removeLyrics();
                }
            }

            case "queue" -> {
                if (enabled) {
                    if (sonarSongQueue == null) {
                        Bukkit.getScheduler().runTask(plugin, () ->
                                sonarSongQueue = new SonarSongQueue(this, getPanelLocation("queue"))
                        );
                    }
                } else {
                    if (sonarSongQueue != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            sonarSongQueue.remove();
                            sonarSongQueue = null;
                        });
                    }
                }
            }

            // title / cover / duration all drive SonarSongDisplay — recreate or remove it
            case "title", "cover", "duration" -> {
                boolean anyDisplayEnabled = setting("title") || setting("cover") || setting("duration");
                if (anyDisplayEnabled && isPlaying && lastSongPath != null && lastSongJson != null) {
                    removeSongDisplay();
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sonarSong = new SonarSongDisplay(
                                    getPanelLocation("title"),
                                    lastSongPath, lastSongJson,
                                    setting("cover"), setting("title"), setting("duration")
                            )
                    );
                } else if (!anyDisplayEnabled) {
                    removeSongDisplay();
                }
            }
        }
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(2.0f, volume));
    }

    public void addVolume(float v) {
        setVolume(getVolume() + v);
    }

    public void removeVolume(float v) {
        setVolume(getVolume() - v);
    }

    public float getGain() {
        if (volume == 0) return 0;
        return (float) Math.pow(volume, 3); // cubic curve, close to Spotify's
    }

    // -------------------------------------------------------------------------
    // Audio internals
    // -------------------------------------------------------------------------

    private record AudioPlayerInfo(
            de.maxhenkel.voicechat.api.audiochannel.AudioPlayer player,
            de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel channel,
            de.maxhenkel.voicechat.api.opus.OpusEncoder encoder,
            Process process
    ) {}

    private final Map<UUID, AudioPlayerInfo> activePlayers = new HashMap<>();

    private volatile boolean songStoppedManually = false;
    private volatile boolean manualPlay = false;

    // Held so setSetting() can cancel/restart the particle task live
    private BukkitTask particleTask = null;

    // Cached so setSetting() can reconstruct displays mid-song without re-decoding audio
    private volatile JsonObject lastSongJson = null;
    private volatile String lastSongPath = null;

    // -------------------------------------------------------------------------
    // Play
    // -------------------------------------------------------------------------

    public void playSong(SongInfo info, boolean manual) {
        manualPlay = manual;
        stopAllAudioOnly();
        songStoppedManually = false;

        Sonar plugin = Sonar.getPlugin();
        Logger log = plugin.getLogger();

        VoicechatServerApi api = SonarPlugin.getApi();
        if (api == null) {
            log.severe("Voice chat API not found.");
            return;
        }

        // Queue display
        if (setting("queue")) {
            if (sonarSongQueue == null) {
                sonarSongQueue = new SonarSongQueue(this, getPanelLocation("queue"));
            }
        } else {
            if (sonarSongQueue != null) {
                sonarSongQueue.remove();
                sonarSongQueue = null;
            }
        }

        String path = info.path();

        // Particle task — only started when the setting is enabled
        if (setting("particle")) {
            particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                Block block = getClosestBlockOfType(location, Material.JUKEBOX, 5);
                if (block != null && block.getState() instanceof Jukebox) {
                    Location loc = block.getLocation();
                    loc.getWorld().spawnParticle(
                            Particle.NOTE,
                            loc.clone().add(0.5, 1.2, 0.5),
                            1, 0, 0, 0, 1
                    );
                }
            }, 0L, 20L);
        } else {
            particleTask = null;
        }

        Thread audioThread = new Thread(() -> {
            Process process = null;
            de.maxhenkel.voicechat.api.opus.OpusEncoder encoder = null;
            de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel channel = null;
            BukkitTask progressTask = null;
            UUID channelId = UUID.randomUUID();

            try {
                JsonObject songJson = getSongInfo(path);
                lastSongJson = songJson;
                lastSongPath = path;

                // Lyrics — fetch and display only when enabled
                if (setting("lyrics")) {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> fetchLyrics(songJson));
                } else {
                    removeLyrics();
                }

                // Song display (title / cover / duration) — create when any of those settings
                // are enabled; the display class itself will check which sub-features to show
                if (setting("title") || setting("cover") || setting("duration")) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sonarSong = new SonarSongDisplay(
                                    getPanelLocation("title"),
                                    path, songJson,
                                    setting("cover"), setting("title"), setting("duration")
                            )
                    );
                } else {
                    removeSongDisplay();
                }

                // Create audio channel
                ServerLevel serverLevel = api.fromServerLevel(location.getWorld());
                Position position = api.createPosition(location.getX(), location.getY(), location.getZ());
                channel = api.createLocationalAudioChannel(channelId, serverLevel, position);

                if (channel == null) {
                    log.severe("Failed to create locational audio channel.");
                    return;
                }

                channel.setCategory(SonarPlugin.getVOLUME_CATEGORY_ID());
                channel.setDistance(48);

                // Decode audio via ffmpeg
                ProcessBuilder pb = new ProcessBuilder(
                        Sonar.getFfmpeg(), "-i", path,
                        "-f", "s16le", "-ar", "48000", "-ac", "1",
                        "pipe:1"
                );
                pb.redirectErrorStream(true);
                process = pb.start();

                short[] pcmData = readPcmData(process.getInputStream());

                encoder = api.createEncoder();
                AtomicInteger frameIndex = new AtomicInteger(0);

                de.maxhenkel.voicechat.api.audiochannel.AudioPlayer player =
                        api.createAudioPlayer(channel, encoder, () -> {
                            int idx = frameIndex.get();
                            if (idx + 960 > pcmData.length) return null;

                            // volume stuff
                            short[] frame = Arrays.copyOfRange(pcmData, idx, idx + 960);
                            for (int i = 0; i < frame.length; i++) {
                                frame[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, (int)(frame[i] * getGain())));
                            }

                            frameIndex.addAndGet(960);
                            return frame;
                        });

                activePlayers.put(channelId, new AudioPlayerInfo(player, channel, encoder, process));
                player.startPlaying();

                // Progress ticker — drives duration display and lyrics sync
                progressTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    double currentSeconds = frameIndex.get() / 48000.0;
                    if (setting("duration") && sonarSong != null) {
                        sonarSong.getDuration().setCurrentTime(currentSeconds);
                    }
                    if (setting("lyrics") && sonarLyrics != null) {
                        sonarLyrics.setCurrentTime(currentSeconds);
                    }
                }, 0L, 2L); // ~100 ms at 20 TPS

                // Wait for playback to finish
                while (player.isPlaying()) {
                    Thread.sleep(200);
                }

            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
                plugin.getLogger().warning("Audio playback interrupted: " + e.getMessage());
            } finally {
                if (progressTask != null) progressTask.cancel();
                if (particleTask != null) {
                    particleTask.cancel();
                    particleTask = null;
                }

                destroyResources(channel, encoder, process);
                activePlayers.remove(channelId);

                boolean wasManual = manualPlay;
                manualPlay = false;

                // Auto-advance queue if playback ended naturally.
                // FIX: Determine the next song BEFORE dispatching to the main thread,
                // then do the cleanup AND the new playSong() in the same task so the
                // destroyResources() GUI teardown can never run after the new displays
                // have already been created.
                if (!songStoppedManually && !wasManual) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (!getQueue().isEmpty()) {
                            SongInfo next = getQueue().removeFirst();
                            if (sonarSongQueue != null) sonarSongQueue.updateQueue();
                            removeSongDisplayNoTask();
                            removeLyricsNoTask();
                            playSong(next, false);
                        } else {
                            removeSongDisplayNoTask();
                            removeLyricsNoTask();
                        }
                    });
                }
            }
        }, "Sonar-Audio-Player-" + id);

        audioThread.setDaemon(true);
        audioThread.start();
    }

    // -------------------------------------------------------------------------
    // Stop
    // -------------------------------------------------------------------------

    public void stopAllAudio() {
        songStoppedManually = true;

        for (AudioPlayerInfo info : activePlayers.values()) {
            if (info.player() != null && info.player().isPlaying()) {
                info.player().stopPlaying();
            }
            destroyResources(info.channel(), info.encoder(), info.process());
        }
        activePlayers.clear();

        // GUI cleanup now that destroyResources() no longer does it
        Bukkit.getScheduler().runTask(Sonar.getPlugin(), () -> {
            removeSongDisplayNoTask();
            removeLyricsNoTask();
        });
    }

    private void stopAllAudioOnly() {
        songStoppedManually = true;
        for (AudioPlayerInfo info : activePlayers.values()) {
            if (info.player() != null && info.player().isPlaying()) {
                info.player().stopPlaying();
            }
            destroyResources(info.channel(), info.encoder(), info.process());
        }
        activePlayers.clear();
    }

    // -------------------------------------------------------------------------
    // GUI helpers
    // -------------------------------------------------------------------------

    private void removeSongDisplay() {
        if (sonarSong != null) {
            Bukkit.getScheduler().runTask(Sonar.getPlugin(), () -> {
                sonarSong.remove();
                sonarSong = null;
            });
        }
    }

    private void removeSongDisplayNoTask() {
        if (sonarSong != null) {
            sonarSong.remove();
            sonarSong = null;
        }
    }

    private void removeLyrics() {
        if (sonarLyrics != null) {
            Bukkit.getScheduler().runTask(Sonar.getPlugin(), () -> {
                sonarLyrics.remove();
                sonarLyrics = null;
            });
        }
    }

    private void removeLyricsNoTask() {
        if (sonarLyrics != null) {
            sonarLyrics.remove();
            sonarLyrics = null;
        }
    }

    // -------------------------------------------------------------------------
    // Resource teardown
    // -------------------------------------------------------------------------

    /**
     * Tears down encoder and ffmpeg process only. GUI removal is intentionally
     * NOT done here — callers in the audio thread's finally block handle GUI
     * cleanup themselves on the main thread to avoid the race condition where
     * this task runs after a new song has already created fresh displays.
     */
    private void destroyResources(
            de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel channel,
            de.maxhenkel.voicechat.api.opus.OpusEncoder encoder,
            Process process) {

        if (encoder != null) encoder.close();

        if (process != null) {
            process.destroy();
            try {
                if (!process.waitFor(1, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Shutdown
    // -------------------------------------------------------------------------

    public void shutdown() {
        removeSongDisplayNoTask();
        removeLyricsNoTask();
        if (sonarSongQueue != null) {
            sonarSongQueue.remove();
            sonarSongQueue = null;
        }

        save();
    }

    // -------------------------------------------------------------------------
    // PCM reading
    // -------------------------------------------------------------------------

    private short[] readPcmData(InputStream stream) throws IOException {
        short[] buf = new short[480_000]; // ~10 s at 48 kHz; grows as needed
        int count = 0;
        byte[] raw = new byte[8192];
        int read;

        try (stream) {
            while ((read = stream.read(raw)) != -1) {
                int samples = read / 2;
                if (count + samples > buf.length) {
                    buf = Arrays.copyOf(buf, Math.max(buf.length * 2, count + samples));
                }
                for (int i = 0; i < read; i += 2) {
                    buf[count++] = (short) ((raw[i] & 0xFF) | (raw[i + 1] << 8));
                }
            }
        }

        return count == buf.length ? buf : Arrays.copyOf(buf, count);
    }

    // -------------------------------------------------------------------------
    // ffprobe / lrclib helpers
    // -------------------------------------------------------------------------

    private JsonObject getSongInfo(String filePath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                Sonar.getFfprobe(),
                "-v", "quiet",
                "-print_format", "json",
                "-show_format",
                "-show_streams",
                filePath
        );

        Process process = pb.start();
        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            output = sb.toString();
        }
        process.waitFor();
        return JsonParser.parseString(output).getAsJsonObject();
    }

    private void fetchLyrics(JsonObject songJson) {
        JsonObject format = songJson.getAsJsonObject("format");
        JsonObject tags   = format.getAsJsonObject("tags");

        double length    = format.has("duration") ? format.get("duration").getAsDouble() : 0;
        String artistStr = tags != null && tags.has("artist") ? tags.get("artist").getAsString() : "Unknown Artist";
        String albumStr  = tags != null && tags.has("album")  ? tags.get("album").getAsString()  : "Unknown Album";
        String titleStr  = tags != null && tags.has("title")  ? tags.get("title").getAsString()  : "Unknown Title";

        try {
            String apiUrl = "https://lrclib.net/api/get?"
                    + "artist_name=" + URLEncoder.encode(artistStr, StandardCharsets.UTF_8)
                    + "&track_name=" + URLEncoder.encode(titleStr,  StandardCharsets.UTF_8)
                    + "&duration="   + (int) Math.round(length);

            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "SonarPlugin/1.0");
            connection.setConnectTimeout(5_000);
            connection.setReadTimeout(10_000);

            try {
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String body;
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        body = sb.toString();
                    }

                    JsonObject lyricsJson = JsonParser.parseString(body).getAsJsonObject();
                    Bukkit.getScheduler().runTask(Sonar.getPlugin(), () ->
                            sonarLyrics = new SonarLyrics(
                                    getPanelLocation("lyrics"),
                                    lyricsJson
                            )
                    );
                } else {
                    Sonar.getPlugin().getLogger().warning("Lyrics fetch failed — HTTP " + responseCode);
                }
            } finally {
                connection.disconnect();
            }
        } catch (Exception e) {
            Sonar.getPlugin().getLogger().warning("Lyrics fetch error: " + e.getMessage());
        }
    }
}