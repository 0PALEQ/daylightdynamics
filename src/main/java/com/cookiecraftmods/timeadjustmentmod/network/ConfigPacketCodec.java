package com.cookiecraftmods.timeadjustmentmod.network;

import com.cookiecraftmods.timeadjustmentmod.DaylightDynamicsConfig;
import net.minecraft.network.RegistryByteBuf;

public final class ConfigPacketCodec {
    public static final net.minecraft.network.codec.PacketCodec<RegistryByteBuf, DaylightDynamicsConfig> NETWORK_CODEC =
            net.minecraft.network.codec.PacketCodec.of(
                    (config, buffer) -> {
                        DaylightDynamicsConfig sanitized = config.sanitize();
                        buffer.writeBoolean(sanitized.running());
                        buffer.writeEnumConstant(sanitized.mode());
                        buffer.writeVarInt(sanitized.customDayLengthMinutes());
                        buffer.writeString(sanitized.timezoneId());
                    },
                    buffer -> new DaylightDynamicsConfig(
                            buffer.readBoolean(),
                            buffer.readEnumConstant(DaylightDynamicsConfig.Mode.class),
                            buffer.readVarInt(),
                            buffer.readString()
                    ).sanitize()
            );

    private ConfigPacketCodec() {
    }
}
