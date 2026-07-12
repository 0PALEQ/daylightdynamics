package com.cookiecraftmods.timeadjustmentmod.client;

import com.cookiecraftmods.timeadjustmentmod.DaylightDynamicsConfig;
import com.cookiecraftmods.timeadjustmentmod.DaylightDynamicsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = DaylightDynamicsMod.MOD_ID, value = Dist.CLIENT)
public final class DaylightDynamicsWorldCreationHooks {
    private DaylightDynamicsWorldCreationHooks() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof CreateWorldScreen screen)) {
            return;
        }

        DaylightDynamicsConfig config = DaylightDynamicsConfig.fromGameRules(screen.getUiState().getGameRules());
        int x = screen.width / 2 - 105;
        int y = screen.height / 2 + 10;
        int width = 210;
        int maxRowY = Integer.MIN_VALUE;

        for (var listener : event.getListenersList()) {
            if (!(listener instanceof AbstractWidget widget) || widget.getWidth() != 210) {
                continue;
            }
            int widgetCenter = widget.getX() + widget.getWidth() / 2;
            if (Math.abs(widgetCenter - screen.width / 2) <= 4 && widget.getY() > maxRowY) {
                maxRowY = widget.getY();
                x = widget.getX();
                y = widget.getY() + 28;
                width = widget.getWidth();
            }
        }

        event.addListener(Button.builder(worldCreationButtonLabel(config), button ->
                        Minecraft.getInstance().setScreen(new DaylightDynamicsWorldCreationScreen(screen)))
                .bounds(x, y, width, 20)
                .build());
    }

    private static Component worldCreationButtonLabel(DaylightDynamicsConfig config) {
        return Component.literal(config.running() ? "Daylight Dynamics: ON" : "Daylight Dynamics: OFF");
    }
}
