package com.cookiecraftmods.timeadjustmentmod.client;

import com.cookiecraftmods.timeadjustmentmod.network.DaylightDynamicsNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class DaylightDynamicsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DaylightDynamicsNetwork.registerClient();
        DaylightDynamicsWorldCreationHooks.initialize();
        ClientTickEvents.END_CLIENT_TICK.register(ClientTimeController::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientState.reset());
    }
}
