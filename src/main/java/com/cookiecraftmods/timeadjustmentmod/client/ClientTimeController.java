package com.cookiecraftmods.timeadjustmentmod.client;

import com.cookiecraftmods.timeadjustmentmod.DaylightDynamicsConfig;
import com.cookiecraftmods.timeadjustmentmod.DaylightDynamicsState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class ClientTimeController {
    private ClientTimeController() {
    }

    public static void tick(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world == null || world.getRegistryKey() != World.OVERWORLD) {
            ClientState.resetTiming();
            return;
        }

        DaylightDynamicsConfig config = ClientState.getConfig();
        if (!config.running()) {
            ClientState.resetTiming();
            return;
        }

        if (config.mode() == DaylightDynamicsConfig.Mode.TIMEZONE) {
            long target = computeTimezoneDayTime(config.timezoneId());
            long currentDayTime = world.getTimeOfDay();
            long currentTimeOfDay = Math.floorMod(currentDayTime, DaylightDynamicsState.DAY_TICKS);
            if (currentTimeOfDay != target) {
                long dayBase = currentDayTime - currentTimeOfDay;
                world.setTimeOfDay(-(dayBase + target));
            } else {
                world.setTimeOfDay(-currentDayTime);
            }
            return;
        }

        double ticksPerSecond = DaylightDynamicsState.DAY_TICKS / (Math.max(1, config.customDayLengthMinutes()) * 60.0D);
        if (ticksPerSecond <= 0.0D) {
            return;
        }

        long now = System.nanoTime();
        if (ClientState.lastUpdateNanos() == 0L) {
            ClientState.setLastUpdateNanos(now);
            world.setTimeOfDay(-world.getTimeOfDay());
            return;
        }

        double elapsedSeconds = (now - ClientState.lastUpdateNanos()) / 1_000_000_000.0D;
        ClientState.setLastUpdateNanos(now);
        if (elapsedSeconds <= 0.0D) {
            return;
        }

        double accumulator = ClientState.customTickAccumulator() + (elapsedSeconds * ticksPerSecond);
        long wholeTicks = (long) Math.floor(accumulator);
        if (wholeTicks > 0L) {
            world.setTimeOfDay(-(world.getTimeOfDay() + wholeTicks));
            accumulator -= wholeTicks;
        } else {
            world.setTimeOfDay(-world.getTimeOfDay());
        }

        ClientState.setCustomTickAccumulator(accumulator);
    }

    private static long computeTimezoneDayTime(String timezoneId) {
        ZonedDateTime realTime = ZonedDateTime.now(ZoneId.of(timezoneId));
        double totalRealSeconds = realTime.toLocalTime().toSecondOfDay()
                + (realTime.getNano() / 1_000_000_000.0D);
        double gameTicks = (totalRealSeconds * DaylightDynamicsState.DAY_TICKS) / (24.0D * 3600.0D);
        double sunriseOffset = DaylightDynamicsState.DAY_TICKS / 4.0D;
        return Math.floorMod((long) Math.floor(gameTicks - sunriseOffset), DaylightDynamicsState.DAY_TICKS);
    }
}
