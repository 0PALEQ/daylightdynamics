package com.cookiecraftmods.timeadjustmentmod.client;

import com.cookiecraftmods.timeadjustmentmod.DaylightDynamicsConfig;

public final class ClientState {
    private static DaylightDynamicsConfig currentConfig = DaylightDynamicsConfig.defaults();

    private ClientState() {
    }

    public static DaylightDynamicsConfig getConfig() {
        return currentConfig;
    }

    public static void setConfig(DaylightDynamicsConfig config) {
        currentConfig = config.copy().sanitize();
    }

    public static void reset() {
        currentConfig = DaylightDynamicsConfig.defaults();
    }
}
