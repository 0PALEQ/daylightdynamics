package com.cookiecraftmods.timeadjustmentmod.client;

import com.cookiecraftmods.timeadjustmentmod.DaylightDynamicsConfig;

public final class ClientState {
    private static DaylightDynamicsConfig currentConfig = DaylightDynamicsConfig.defaults();
    private static long lastUpdateNanos;
    private static double customTickAccumulator;

    private ClientState() {
    }

    public static DaylightDynamicsConfig getConfig() {
        return currentConfig;
    }

    public static void setConfig(DaylightDynamicsConfig config) {
        currentConfig = config.copy().sanitize();
        resetTiming();
    }

    public static long lastUpdateNanos() {
        return lastUpdateNanos;
    }

    public static void setLastUpdateNanos(long value) {
        lastUpdateNanos = value;
    }

    public static double customTickAccumulator() {
        return customTickAccumulator;
    }

    public static void setCustomTickAccumulator(double value) {
        customTickAccumulator = value;
    }

    public static void resetTiming() {
        lastUpdateNanos = 0L;
        customTickAccumulator = 0.0D;
    }

    public static void reset() {
        currentConfig = DaylightDynamicsConfig.defaults();
        resetTiming();
    }
}
