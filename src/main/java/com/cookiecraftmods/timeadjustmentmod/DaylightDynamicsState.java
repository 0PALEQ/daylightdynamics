package com.cookiecraftmods.timeadjustmentmod;

import com.cookiecraftmods.timeadjustmentmod.network.DaylightDynamicsNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DaylightDynamicsState {
    public static final long DAY_TICKS = 24000L;

    private MinecraftServer server;
    private DaylightDynamicsConfig config = DaylightDynamicsConfig.defaults();
    private long lastTickNanos;
    private double customTickAccumulator;
    private boolean controlsDaylightRule;
    private boolean previousDaylightRule;

    public void load(MinecraftServer server) {
        this.server = server;
        this.config = DaylightDynamicsConfig.load(server);
        this.lastTickNanos = 0L;
        this.customTickAccumulator = 0.0D;
        this.controlsDaylightRule = false;
        this.previousDaylightRule = server.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
    }

    public void unload() {
        this.server = null;
        this.config = DaylightDynamicsConfig.defaults();
        this.lastTickNanos = 0L;
        this.customTickAccumulator = 0.0D;
        this.controlsDaylightRule = false;
        this.previousDaylightRule = true;
    }

    public DaylightDynamicsConfig snapshot() {
        return config.copy();
    }

    public void update(DaylightDynamicsConfig newConfig) {
        config = newConfig.copy().sanitize();
        lastTickNanos = 0L;
        customTickAccumulator = 0.0D;
        persistAndSync();
    }

    public void applyGameRule(MinecraftServer server) {
        if (!config.running()) {
            restoreGameRule();
            return;
        }

        if (!controlsDaylightRule) {
            previousDaylightRule = server.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
            controlsDaylightRule = true;
        }

        boolean daylightCycleEnabled = config.mode() == DaylightDynamicsConfig.Mode.CUSTOM
                && anySleepingPlayers(server.overworld());
        server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(daylightCycleEnabled, server);
    }

    public void restoreGameRule() {
        if (controlsDaylightRule && server != null) {
            server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(previousDaylightRule, server);
            controlsDaylightRule = false;
        }
    }

    public void tick(ServerLevel world) {
        if (world.dimension() != Level.OVERWORLD) {
            return;
        }

        if (!config.running()) {
            return;
        }

        if (config.mode() == DaylightDynamicsConfig.Mode.CUSTOM) {
            boolean someoneSleeping = anySleepingPlayers(world);
            if (server != null) {
                server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(someoneSleeping, server);
            }
            if (someoneSleeping) {
                return;
            }
        }

        if (config.mode() == DaylightDynamicsConfig.Mode.TIMEZONE) {
            long target = computeTimezoneDayTime();
            long current = world.getDayTime();
            long currentTimeOfDay = Math.floorMod(current, DAY_TICKS);
            if (currentTimeOfDay != target) {
                world.setDayTime(current - currentTimeOfDay + target);
            }
            return;
        }

        long now = System.nanoTime();
        if (lastTickNanos == 0L) {
            lastTickNanos = now;
            return;
        }

        double elapsedSeconds = (now - lastTickNanos) / 1_000_000_000.0D;
        lastTickNanos = now;
        if (elapsedSeconds <= 0.0D) {
            return;
        }

        customTickAccumulator += elapsedSeconds * ticksPerRealSecond();
        long wholeTicks = extractWholeTicks(customTickAccumulator);
        if (wholeTicks != 0L) {
            world.setDayTime(world.getDayTime() + wholeTicks);
            customTickAccumulator -= wholeTicks;
        }
    }

    private void persistAndSync() {
        if (server == null) {
            return;
        }

        applyGameRule(server);
        config.save(server);
        DaylightDynamicsNetwork.broadcastState(server, config);
    }

    private double ticksPerRealSecond() {
        double realSeconds = Math.max(1, config.customDayLengthMinutes()) * 60.0D;
        return DAY_TICKS / realSeconds;
    }

    private long computeTimezoneDayTime() {
        ZonedDateTime realTime = ZonedDateTime.now(ZoneId.of(config.timezoneId()));
        double totalRealSeconds = realTime.toLocalTime().toSecondOfDay()
                + (realTime.getNano() / 1_000_000_000.0D);
        double gameTicks = (totalRealSeconds * DAY_TICKS) / (24.0D * 3600.0D);
        double sunriseOffset = DAY_TICKS / 4.0D;
        return Math.floorMod((long) Math.floor(gameTicks - sunriseOffset), DAY_TICKS);
    }

    private static long extractWholeTicks(double accumulator) {
        if (accumulator >= 1.0D) {
            return (long) Math.floor(accumulator);
        }

        if (accumulator <= -1.0D) {
            return (long) Math.ceil(accumulator);
        }

        return 0L;
    }

    private static boolean anySleepingPlayers(ServerLevel world) {
        if (world == null) {
            return false;
        }

        for (ServerPlayer player : world.players()) {
            if (player.isSleeping()) {
                return true;
            }
        }
        return false;
    }
}
