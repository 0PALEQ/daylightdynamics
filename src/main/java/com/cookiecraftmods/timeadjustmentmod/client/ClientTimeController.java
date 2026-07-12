package com.cookiecraftmods.timeadjustmentmod.client;

import com.cookiecraftmods.timeadjustmentmod.DaylightDynamicsConfig;
import com.cookiecraftmods.timeadjustmentmod.DaylightDynamicsState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class ClientTimeController {
    private ClientTimeController() {
    }

    public static void tick(Minecraft client) {
        ClientLevel world = client.level;
        if (world == null || world.dimension() != Level.OVERWORLD) {
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
            long currentDayTime = world.getDayTime();
            long currentTimeOfDay = Math.floorMod(currentDayTime, DaylightDynamicsState.DAY_TICKS);
            long dayBase = currentDayTime - currentTimeOfDay;
            world.setDayTime(-(dayBase + target));
            return;
        }

        double ticksPerSecond = DaylightDynamicsState.DAY_TICKS
                / (Math.max(1, config.customDayLengthMinutes()) * 60.0D);
        long now = System.nanoTime();
        if (ClientState.lastUpdateNanos() == 0L) {
            ClientState.setLastUpdateNanos(now);
            world.setDayTime(-world.getDayTime());
            return;
        }

        double elapsedSeconds = (now - ClientState.lastUpdateNanos()) / 1_000_000_000.0D;
        ClientState.setLastUpdateNanos(now);
        if (elapsedSeconds <= 0.0D) {
            return;
        }

        double accumulator = ClientState.customTickAccumulator() + elapsedSeconds * ticksPerSecond;
        long wholeTicks = (long) Math.floor(accumulator);
        if (wholeTicks > 0L) {
            world.setDayTime(-(Math.abs(world.getDayTime()) + wholeTicks));
            accumulator -= wholeTicks;
        } else {
            world.setDayTime(-Math.abs(world.getDayTime()));
        }
        ClientState.setCustomTickAccumulator(accumulator);
    }

    private static long computeTimezoneDayTime(String timezoneId) {
        ZonedDateTime realTime = ZonedDateTime.now(ZoneId.of(timezoneId));
        double totalRealSeconds = realTime.toLocalTime().toSecondOfDay()
                + realTime.getNano() / 1_000_000_000.0D;
        double gameTicks = totalRealSeconds * DaylightDynamicsState.DAY_TICKS / (24.0D * 3600.0D);
        return Math.floorMod(
                (long) Math.floor(gameTicks - DaylightDynamicsState.DAY_TICKS / 4.0D),
                DaylightDynamicsState.DAY_TICKS
        );
    }
}
