package com.cookiecraftmods.timeadjustmentmod.network;

import com.cookiecraftmods.timeadjustmentmod.DaylightDynamicsConfig;
import com.cookiecraftmods.timeadjustmentmod.DaylightDynamicsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.function.Consumer;

public final class DaylightDynamicsNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static Consumer<DaylightDynamicsConfig> openScreenHandler = config -> { };
    private static Consumer<DaylightDynamicsConfig> syncStateHandler = config -> { };

    private DaylightDynamicsNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(OpenScreenPayload.TYPE, OpenScreenPayload.STREAM_CODEC,
                (payload, context) -> openScreenHandler.accept(payload.config()));
        registrar.playToClient(SyncStatePayload.TYPE, SyncStatePayload.STREAM_CODEC,
                (payload, context) -> syncStateHandler.accept(payload.config()));
        registrar.playToServer(UpdateSettingsPayload.TYPE, UpdateSettingsPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                        DaylightDynamicsMod.state().update(payload.config());
                    }
                });
    }

    public static void installClientHandlers(
            Consumer<DaylightDynamicsConfig> openScreen,
            Consumer<DaylightDynamicsConfig> syncState
    ) {
        openScreenHandler = openScreen;
        syncStateHandler = syncState;
    }

    public static void openScreen(ServerPlayer player, DaylightDynamicsConfig config) {
        PacketDistributor.sendToPlayer(player, new OpenScreenPayload(config.sanitize()));
    }

    public static void syncToPlayer(ServerPlayer player, DaylightDynamicsConfig config) {
        PacketDistributor.sendToPlayer(player, new SyncStatePayload(config.sanitize()));
    }

    public static void broadcastState(MinecraftServer server, DaylightDynamicsConfig config) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncToPlayer(player, config);
        }
    }

    public static void sendUpdate(DaylightDynamicsConfig config) {
        ClientPacketDistributor.sendToServer(new UpdateSettingsPayload(config.sanitize()));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(DaylightDynamicsMod.MOD_ID, path);
    }

    public record OpenScreenPayload(DaylightDynamicsConfig config) implements CustomPacketPayload {
        public static final Type<OpenScreenPayload> TYPE = new Type<>(id("open_screen"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenScreenPayload> STREAM_CODEC =
                StreamCodec.composite(ConfigPacketCodec.STREAM_CODEC, OpenScreenPayload::config, OpenScreenPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SyncStatePayload(DaylightDynamicsConfig config) implements CustomPacketPayload {
        public static final Type<SyncStatePayload> TYPE = new Type<>(id("sync_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncStatePayload> STREAM_CODEC =
                StreamCodec.composite(ConfigPacketCodec.STREAM_CODEC, SyncStatePayload::config, SyncStatePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record UpdateSettingsPayload(DaylightDynamicsConfig config) implements CustomPacketPayload {
        public static final Type<UpdateSettingsPayload> TYPE = new Type<>(id("update_settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UpdateSettingsPayload> STREAM_CODEC =
                StreamCodec.composite(ConfigPacketCodec.STREAM_CODEC, UpdateSettingsPayload::config, UpdateSettingsPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
