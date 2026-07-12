package com.cookiecraftmods.timeadjustmentmod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

public final class DaylightDynamicsGameRules {
    public static final GameRules.Key<GameRules.BooleanValue> RUNNING = GameRules.register(
            "daylightDynamicsRunning", GameRules.Category.UPDATES, GameRules.BooleanValue.create(false));
    public static final GameRules.Key<GameRules.BooleanValue> REALTIME = GameRules.register(
            "daylightDynamicsRealtime", GameRules.Category.UPDATES, GameRules.BooleanValue.create(false));
    public static final GameRules.Key<GameRules.IntegerValue> DAY_LENGTH_MINUTES = GameRules.register(
            "daylightDynamicsDayLengthMinutes", GameRules.Category.UPDATES, GameRules.IntegerValue.create(24 * 60));

    private DaylightDynamicsGameRules() {
    }

    public static void initialize() {
        // Forces class initialization early enough for world-creation game rules.
    }

    public static DaylightDynamicsConfig read(GameRules gameRules) {
        DaylightDynamicsConfig defaults = DaylightDynamicsConfig.defaults();
        DaylightDynamicsConfig.Mode mode = gameRules.getBoolean(REALTIME)
                ? DaylightDynamicsConfig.Mode.TIMEZONE
                : DaylightDynamicsConfig.Mode.CUSTOM;
        return new DaylightDynamicsConfig(
                gameRules.getBoolean(RUNNING),
                mode,
                gameRules.getInt(DAY_LENGTH_MINUTES),
                defaults.timezoneId()
        ).sanitize();
    }

    public static void write(GameRules gameRules, DaylightDynamicsConfig config, MinecraftServer server) {
        DaylightDynamicsConfig sanitized = config.sanitize();
        gameRules.getRule(RUNNING).set(sanitized.running(), server);
        gameRules.getRule(REALTIME).set(sanitized.mode() == DaylightDynamicsConfig.Mode.TIMEZONE, server);
        gameRules.getRule(DAY_LENGTH_MINUTES).set(sanitized.customDayLengthMinutes(), server);
    }
}
