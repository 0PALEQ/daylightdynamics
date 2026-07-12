package com.cookiecraftmods.timeadjustmentmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.GameRules;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;

public record DaylightDynamicsConfig(boolean running, Mode mode, int customDayLengthMinutes, String timezoneId) {
    public enum Mode {
        TIMEZONE,
        CUSTOM
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "daylightdynamics.json";

    public static DaylightDynamicsConfig defaults() {
        return new DaylightDynamicsConfig(false, Mode.CUSTOM, 24 * 60, ZoneId.systemDefault().getId());
    }

    public static DaylightDynamicsConfig load(MinecraftServer server) {
        Path path = configPath(server);
        DaylightDynamicsConfig fallback = DaylightDynamicsGameRules.read(server.getGameRules());

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                DaylightDynamicsConfig parsed = GSON.fromJson(reader, DaylightDynamicsConfig.class);
                DaylightDynamicsConfig config = parsed == null ? fallback : parsed.sanitize();
                DaylightDynamicsGameRules.write(server.getGameRules(), config, server);
                return config;
            } catch (IOException | JsonSyntaxException ex) {
                DaylightDynamicsGameRules.write(server.getGameRules(), fallback, server);
                return fallback;
            }
        }

        fallback.save(server);
        DaylightDynamicsGameRules.write(server.getGameRules(), fallback, server);
        return fallback;
    }

    public void save(MinecraftServer server) {
        Path path = configPath(server);
        try {
            Files.createDirectories(path.getParent());
            DaylightDynamicsGameRules.write(server.getGameRules(), this, server);
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(sanitize(), writer);
            }
        } catch (IOException ignored) {
        }
    }

    public DaylightDynamicsConfig sanitize() {
        Mode safeMode = mode == null ? Mode.CUSTOM : mode;
        int safeMinutes = Math.max(1, Math.min(24 * 60, customDayLengthMinutes));
        String safeTimezone = sanitizeTimezone(timezoneId);
        return new DaylightDynamicsConfig(running, safeMode, safeMinutes, safeTimezone);
    }

    public DaylightDynamicsConfig copy() {
        return new DaylightDynamicsConfig(running, mode, customDayLengthMinutes, timezoneId);
    }

    public DaylightDynamicsConfig withRunning(boolean running) {
        return new DaylightDynamicsConfig(running, mode, customDayLengthMinutes, timezoneId);
    }

    public static DaylightDynamicsConfig fromGameRules(GameRules gameRules) {
        return DaylightDynamicsGameRules.read(gameRules);
    }

    private static String sanitizeTimezone(String value) {
        if (value == null || value.isBlank()) {
            return ZoneId.systemDefault().getId();
        }

        try {
            return ZoneId.of(value).getId();
        } catch (Exception ignored) {
            return ZoneId.systemDefault().getId();
        }
    }

    private static Path configPath(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT)
                .resolve("serverconfig")
                .resolve(FILE_NAME);
    }
}
