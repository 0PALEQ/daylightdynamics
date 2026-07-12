package com.cookiecraftmods.timeadjustmentmod;

import com.cookiecraftmods.timeadjustmentmod.network.DaylightDynamicsNetwork;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

public class DaylightDynamicsMod implements ModInitializer {
    public static final String MOD_ID = "daylightdynamics";
    private static final DaylightDynamicsState STATE = new DaylightDynamicsState();

    @Override
    public void onInitialize() {
        DaylightDynamicsGameRules.initialize();
        DaylightDynamicsNetwork.registerPayloads();
        DaylightDynamicsNetwork.registerServer();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                DaylightDynamicsCommand.register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> STATE.unload());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                DaylightDynamicsNetwork.syncToPlayer(handler.getPlayer(), STATE.snapshot()));
        ServerTickEvents.END_WORLD_TICK.register(STATE::tick);
        EntitySleepEvents.ALLOW_SLEEP_TIME.register((player, sleepingPos, vanillaResult) -> {
            handleAllowSleepTime(player);
            return ActionResult.PASS;
        });
    }

    public static DaylightDynamicsState state() {
        return STATE;
    }

    private void onServerStarted(MinecraftServer server) {
        STATE.load(server);
        STATE.applyGameRule(server);
        DaylightDynamicsNetwork.broadcastState(server, STATE.snapshot());
    }

    private void handleAllowSleepTime(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        DaylightDynamicsConfig config = STATE.snapshot();
        if (!config.running() || config.mode() != DaylightDynamicsConfig.Mode.CUSTOM) {
            return;
        }

        ServerWorld world = serverPlayer.getServerWorld();
        if (world.getRegistryKey() != World.OVERWORLD) {
            return;
        }

        long timeOfDay = Math.floorMod(world.getTimeOfDay(), DaylightDynamicsState.DAY_TICKS);
        if (timeOfDay < 13000L) {
            return;
        }

        MinecraftServer server = world.getServer();
        server.getGameRules().get(net.minecraft.world.GameRules.DO_DAYLIGHT_CYCLE).set(true, server);
    }
}
