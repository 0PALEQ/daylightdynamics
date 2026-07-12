package com.cookiecraftmods.timeadjustmentmod;

import com.cookiecraftmods.timeadjustmentmod.network.DaylightDynamicsNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DaylightDynamicsState {
    public static final long DAY_TICKS = 24000L;
    private static final float GAME_TICKS_PER_SECOND = 20.0F;
    private static final float REALTIME_DAY_TIME_PER_TICK =
            DAY_TICKS / (24.0F * 60.0F * 60.0F * GAME_TICKS_PER_SECOND);

    private MinecraftServer server;
    private DaylightDynamicsConfig config = DaylightDynamicsConfig.defaults();
    private boolean timezoneAligned;
    private boolean controlsDaylightRule;
    private boolean previousDaylightRule;

    public void load(MinecraftServer server) {
        this.server = server;
        this.config = DaylightDynamicsConfig.load(server);
        this.timezoneAligned = false;
        this.controlsDaylightRule = false;
        this.previousDaylightRule = server.overworld().getGameRules().get(GameRules.ADVANCE_TIME);
    }

    public void unload() {
        this.server = null;
        this.config = DaylightDynamicsConfig.defaults();
        this.timezoneAligned = false;
        this.controlsDaylightRule = false;
        this.previousDaylightRule = true;
    }

    public DaylightDynamicsConfig snapshot() {
        return config.copy();
    }

    public void update(DaylightDynamicsConfig newConfig) {
        config = newConfig.copy().sanitize();
        timezoneAligned = false;
        persistAndSync();
    }

    public void applyGameRule(MinecraftServer server) {
        if (!config.running()) {
            restoreGameRule();
            return;
        }

        if (!controlsDaylightRule) {
            previousDaylightRule = server.overworld().getGameRules().get(GameRules.ADVANCE_TIME);
            controlsDaylightRule = true;
        }

        ServerLevel overworld = server.overworld();
        overworld.getGameRules().set(GameRules.ADVANCE_TIME, true, server);
        overworld.setDayTimePerTick(dayTimePerGameTick());
    }

    public void restoreGameRule() {
        if (controlsDaylightRule && server != null) {
            ServerLevel overworld = server.overworld();
            overworld.setDayTimePerTick(-1.0F);
            overworld.getGameRules().set(GameRules.ADVANCE_TIME, previousDaylightRule, server);
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

        if (config.mode() == DaylightDynamicsConfig.Mode.TIMEZONE) {
            long target = computeTimezoneDayTime();
            long current = world.getDayTime();
            long currentTimeOfDay = Math.floorMod(current, DAY_TICKS);
            if (!timezoneAligned) {
                world.setDayTime(current + Math.floorMod(target - currentTimeOfDay, DAY_TICKS));
                timezoneAligned = true;
                return;
            }

            long drift = Math.floorMod(target - currentTimeOfDay + (DAY_TICKS / 2L), DAY_TICKS)
                    - (DAY_TICKS / 2L);
            if (Math.abs(drift) > 2L) {
                world.setDayTime(current + drift);
            }
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

    private float dayTimePerGameTick() {
        if (config.mode() == DaylightDynamicsConfig.Mode.TIMEZONE) {
            return REALTIME_DAY_TIME_PER_TICK;
        }
        float realSeconds = Math.max(1, config.customDayLengthMinutes()) * 60.0F;
        return DAY_TICKS / (realSeconds * GAME_TICKS_PER_SECOND);
    }

    private long computeTimezoneDayTime() {
        ZonedDateTime realTime = ZonedDateTime.now(ZoneId.of(config.timezoneId()));
        double totalRealSeconds = realTime.toLocalTime().toSecondOfDay()
                + (realTime.getNano() / 1_000_000_000.0D);
        double gameTicks = (totalRealSeconds * DAY_TICKS) / (24.0D * 3600.0D);
        double sunriseOffset = DAY_TICKS / 4.0D;
        return Math.floorMod((long) Math.floor(gameTicks - sunriseOffset), DAY_TICKS);
    }

}
