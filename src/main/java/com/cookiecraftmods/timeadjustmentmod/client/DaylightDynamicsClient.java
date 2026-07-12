package com.cookiecraftmods.timeadjustmentmod.client;

import com.cookiecraftmods.timeadjustmentmod.DaylightDynamicsConfig;
import com.cookiecraftmods.timeadjustmentmod.DaylightDynamicsMod;
import com.cookiecraftmods.timeadjustmentmod.network.DaylightDynamicsNetwork;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

public final class DaylightDynamicsClient {
    private DaylightDynamicsClient() {
    }

    @EventBusSubscriber(modid = DaylightDynamicsMod.MOD_ID, value = Dist.CLIENT)
    public static final class ModEvents {
        private ModEvents() {
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            DaylightDynamicsNetwork.installClientHandlers(
                    DaylightDynamicsClient::openScreen,
                    DaylightDynamicsClient::syncState
            );
        }
    }

    @EventBusSubscriber(modid = DaylightDynamicsMod.MOD_ID, value = Dist.CLIENT)
    public static final class GameEvents {
        private GameEvents() {
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientState.reset();
        }
    }

    private static void openScreen(DaylightDynamicsConfig config) {
        Minecraft client = Minecraft.getInstance();
        ClientState.setConfig(config);
        client.setScreen(new DaylightDynamicsScreen(config));
    }

    private static void syncState(DaylightDynamicsConfig config) {
        Minecraft client = Minecraft.getInstance();
        ClientState.setConfig(config);
        if (client.screen instanceof DaylightDynamicsScreen screen) {
            screen.refreshFromServer(config);
        }
    }
}
