package com.cookiecraftmods.timeadjustmentmod;

import com.cookiecraftmods.timeadjustmentmod.network.DaylightDynamicsNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DaylightDynamicsState {
    public static final long DAY_TICKS = 24000L;

    private MinecraftServer server;
    private DaylightDynamicsConfig config = DaylightDynamicsConfig.defaults();
    private long lastTickNanos;
    private double customTickAccumulator;

    public void load(MinecraftServer server) {
        this.server = server;
        this.config = DaylightDynamicsConfig.load(server);
        this.lastTickNanos = 0L;
        this.customTickAccumulator = 0.0D;
    }

    public void unload() {
        this.server = null;
        this.config = DaylightDynamicsConfig.defaults();
        this.lastTickNanos = 0L;
        this.customTickAccumulator = 0.0D;
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
            return;
        }

        boolean daylightCycleEnabled = config.mode() == DaylightDynamicsConfig.Mode.CUSTOM
                && anySleepingPlayers(server.getOverworld());
        server.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(daylightCycleEnabled, server);
    }

    public void tick(ServerWorld world) {
        if (world.getRegistryKey() != World.OVERWORLD) {
            return;
        }

        if (!config.running()) {
            return;
        }

        if (config.mode() == DaylightDynamicsConfig.Mode.CUSTOM) {
            boolean someoneSleeping = anySleepingPlayers(world);
            if (server != null) {
                server.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(someoneSleeping, server);
            }
            if (someoneSleeping) {
                return;
            }
        }

        if (config.mode() == DaylightDynamicsConfig.Mode.TIMEZONE) {
            long target = computeTimezoneDayTime();
            if (world.getTimeOfDay() % DAY_TICKS != target) {
                world.setTimeOfDay(world.getTimeOfDay() - (world.getTimeOfDay() % DAY_TICKS) + target);
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
            world.setTimeOfDay(world.getTimeOfDay() + wholeTicks);
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

    private static boolean anySleepingPlayers(ServerWorld world) {
        if (world == null) {
            return false;
        }

        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSleeping()) {
                return true;
            }
        }
        return false;
    }
}
