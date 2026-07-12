package com.cookiecraftmods.timeadjustmentmod.network;

import com.cookiecraftmods.timeadjustmentmod.DaylightDynamicsConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public final class ConfigPacketCodec {
    public static final StreamCodec<RegistryFriendlyByteBuf, DaylightDynamicsConfig> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, config) -> {
                        DaylightDynamicsConfig sanitized = config.sanitize();
                        buffer.writeBoolean(sanitized.running());
                        buffer.writeEnum(sanitized.mode());
                        buffer.writeVarInt(sanitized.customDayLengthMinutes());
                        buffer.writeUtf(sanitized.timezoneId());
                    },
                    buffer -> new DaylightDynamicsConfig(
                            buffer.readBoolean(),
                            buffer.readEnum(DaylightDynamicsConfig.Mode.class),
                            buffer.readVarInt(),
                            buffer.readUtf(128)
                    ).sanitize()
            );

    private ConfigPacketCodec() {
    }
}
