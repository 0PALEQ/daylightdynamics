package com.cookiecraftmods.timeadjustmentmod.network;

import com.cookiecraftmods.timeadjustmentmod.DaylightDynamicsConfig;
import com.cookiecraftmods.timeadjustmentmod.DaylightDynamicsMod;
import com.cookiecraftmods.timeadjustmentmod.client.ClientState;
import com.cookiecraftmods.timeadjustmentmod.client.DaylightDynamicsScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class DaylightDynamicsNetwork {
    private static final Identifier OPEN_SCREEN = Identifier.of(DaylightDynamicsMod.MOD_ID, "open_screen");
    private static final Identifier SYNC_STATE = Identifier.of(DaylightDynamicsMod.MOD_ID, "sync_state");
    private static final Identifier UPDATE_SETTINGS = Identifier.of(DaylightDynamicsMod.MOD_ID, "update_settings");

    private DaylightDynamicsNetwork() {
    }

    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(OpenScreenPayload.ID, OpenScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncStatePayload.ID, SyncStatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateSettingsPayload.ID, UpdateSettingsPayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(UpdateSettingsPayload.ID, (payload, context) -> {
            if (!context.player().hasPermissionLevel(2)) {
                return;
            }

            DaylightDynamicsMod.state().update(payload.config());
        });
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(OpenScreenPayload.ID, (payload, context) -> {
            DaylightDynamicsConfig config = payload.config();
            var client = context.client();
            client.execute(() -> {
                ClientState.setConfig(config);
                client.setScreen(new DaylightDynamicsScreen(config));
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(SyncStatePayload.ID, (payload, context) -> {
            DaylightDynamicsConfig config = payload.config();
            var client = context.client();
            client.execute(() -> {
                ClientState.setConfig(config);
                if (client.currentScreen instanceof DaylightDynamicsScreen screen) {
                    screen.refreshFromServer(config);
                }
            });
        });
    }

    public static void openScreen(ServerPlayerEntity player, DaylightDynamicsConfig config) {
        if (!ServerPlayNetworking.canSend(player, OpenScreenPayload.ID)) {
            return;
        }

        ServerPlayNetworking.send(player, new OpenScreenPayload(config.sanitize()));
    }

    public static void syncToPlayer(ServerPlayerEntity player, DaylightDynamicsConfig config) {
        if (!ServerPlayNetworking.canSend(player, SyncStatePayload.ID)) {
            return;
        }

        ServerPlayNetworking.send(player, new SyncStatePayload(config.sanitize()));
    }

    public static void broadcastState(MinecraftServer server, DaylightDynamicsConfig config) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            syncToPlayer(player, config);
        }
    }

    public static void sendUpdate(DaylightDynamicsConfig config) {
        ClientPlayNetworking.send(new UpdateSettingsPayload(config.sanitize()));
    }

    public record OpenScreenPayload(DaylightDynamicsConfig config) implements CustomPayload {
        public static final Id<OpenScreenPayload> ID = new Id<>(OPEN_SCREEN);
        public static final net.minecraft.network.codec.PacketCodec<RegistryByteBuf, OpenScreenPayload> CODEC =
                net.minecraft.network.codec.PacketCodec.tuple(
                        ConfigPacketCodec.NETWORK_CODEC,
                        OpenScreenPayload::config,
                        OpenScreenPayload::new
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SyncStatePayload(DaylightDynamicsConfig config) implements CustomPayload {
        public static final Id<SyncStatePayload> ID = new Id<>(SYNC_STATE);
        public static final net.minecraft.network.codec.PacketCodec<RegistryByteBuf, SyncStatePayload> CODEC =
                net.minecraft.network.codec.PacketCodec.tuple(
                        ConfigPacketCodec.NETWORK_CODEC,
                        SyncStatePayload::config,
                        SyncStatePayload::new
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record UpdateSettingsPayload(DaylightDynamicsConfig config) implements CustomPayload {
        public static final Id<UpdateSettingsPayload> ID = new Id<>(UPDATE_SETTINGS);
        public static final net.minecraft.network.codec.PacketCodec<RegistryByteBuf, UpdateSettingsPayload> CODEC =
                net.minecraft.network.codec.PacketCodec.tuple(
                        ConfigPacketCodec.NETWORK_CODEC,
                        UpdateSettingsPayload::config,
                        UpdateSettingsPayload::new
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
