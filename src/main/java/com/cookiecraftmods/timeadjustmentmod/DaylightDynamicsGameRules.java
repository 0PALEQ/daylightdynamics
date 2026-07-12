package com.cookiecraftmods.timeadjustmentmod;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;

public final class DaylightDynamicsGameRules {
    public static final GameRules.Key<GameRules.BooleanRule> RUNNING = GameRuleRegistry.register(
            "daylightDynamicsRunning",
            GameRules.Category.UPDATES,
            GameRuleFactory.createBooleanRule(false)
    );
    public static final GameRules.Key<GameRules.BooleanRule> REALTIME = GameRuleRegistry.register(
            "daylightDynamicsRealtime",
            GameRules.Category.UPDATES,
            GameRuleFactory.createBooleanRule(false)
    );
    public static final GameRules.Key<GameRules.IntRule> DAY_LENGTH_MINUTES = GameRuleRegistry.register(
            "daylightDynamicsDayLengthMinutes",
            GameRules.Category.UPDATES,
            GameRuleFactory.createIntRule(24 * 60)
    );

    private DaylightDynamicsGameRules() {
    }

    public static void initialize() {
    }

    public static DaylightDynamicsConfig read(GameRules gameRules) {
        DaylightDynamicsConfig defaults = DaylightDynamicsConfig.defaults();
        boolean running = gameRules.getBoolean(RUNNING);
        boolean realtime = gameRules.getBoolean(REALTIME);
        int dayLengthMinutes = gameRules.getInt(DAY_LENGTH_MINUTES);
        DaylightDynamicsConfig.Mode mode = realtime
                ? DaylightDynamicsConfig.Mode.TIMEZONE
                : DaylightDynamicsConfig.Mode.CUSTOM;
        return new DaylightDynamicsConfig(
                running,
                mode,
                dayLengthMinutes,
                defaults.timezoneId()
        ).sanitize();
    }

    public static void write(GameRules gameRules, DaylightDynamicsConfig config, MinecraftServer server) {
        DaylightDynamicsConfig sanitized = config.sanitize();
        gameRules.get(RUNNING).set(sanitized.running(), server);
        gameRules.get(REALTIME).set(sanitized.mode() == DaylightDynamicsConfig.Mode.TIMEZONE, server);
        gameRules.get(DAY_LENGTH_MINUTES).set(sanitized.customDayLengthMinutes(), server);
    }
}
