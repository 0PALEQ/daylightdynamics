package com.cookiecraftmods.timeadjustmentmod;

import com.cookiecraftmods.timeadjustmentmod.network.DaylightDynamicsNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@Mod(DaylightDynamicsMod.MOD_ID)
public final class DaylightDynamicsMod {
    public static final String MOD_ID = "daylightdynamics";
    private static final DaylightDynamicsState STATE = new DaylightDynamicsState();

    public DaylightDynamicsMod(IEventBus modBus) {
        DaylightDynamicsGameRules.initialize();
        modBus.addListener(DaylightDynamicsNetwork::registerPayloads);

        IEventBus gameBus = NeoForge.EVENT_BUS;
        gameBus.addListener(this::onRegisterCommands);
        gameBus.addListener(this::onServerStarted);
        gameBus.addListener(this::onServerStopped);
        gameBus.addListener(this::onServerStopping);
        gameBus.addListener(this::onPlayerLoggedIn);
        gameBus.addListener(this::onLevelTick);
        gameBus.addListener(this::onCanPlayerSleep);
    }

    public static DaylightDynamicsState state() {
        return STATE;
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        DaylightDynamicsCommand.register(event.getDispatcher());
    }

    private void onServerStarted(ServerStartedEvent event) {
        STATE.load(event.getServer());
        STATE.applyGameRule(event.getServer());
        DaylightDynamicsNetwork.broadcastState(event.getServer(), STATE.snapshot());
    }

    private void onServerStopped(ServerStoppedEvent event) {
        STATE.unload();
    }

    private void onServerStopping(ServerStoppingEvent event) {
        STATE.restoreGameRule();
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DaylightDynamicsNetwork.syncToPlayer(player, STATE.snapshot());
        }
    }

    private void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            STATE.tick(level);
        }
    }

    private void onCanPlayerSleep(CanPlayerSleepEvent event) {
        DaylightDynamicsConfig config = STATE.snapshot();
        if (!config.running() || config.mode() != DaylightDynamicsConfig.Mode.CUSTOM) {
            return;
        }

        ServerPlayer player = event.getEntity();
        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        long timeOfDay = Math.floorMod(level.getDayTime(), DaylightDynamicsState.DAY_TICKS);
        if (timeOfDay >= 13000L) {
            level.getServer().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT)
                    .set(true, level.getServer());
        }
    }
}
