package com.cookiecraftmods.timeadjustmentmod.client;

import com.cookiecraftmods.timeadjustmentmod.DaylightDynamicsConfig;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public final class DaylightDynamicsWorldCreationHooks {
    private DaylightDynamicsWorldCreationHooks() {
    }

    public static void initialize() {
        ScreenEvents.AFTER_INIT.register(DaylightDynamicsWorldCreationHooks::onScreenInit);
    }

    private static void onScreenInit(MinecraftClient client, Screen screen, int scaledWidth, int scaledHeight) {
        if (!(screen instanceof CreateWorldScreen createWorldScreen)) {
            return;
        }

        DaylightDynamicsConfig config = DaylightDynamicsConfig.fromGameRules(createWorldScreen.getWorldCreator().getGameRules());

        ClickableWidget existingButton = null;
        for (ClickableWidget widget : Screens.getButtons(screen)) {
            String text = widget.getMessage().getString();
            if (!text.startsWith("Daylight Dynamics: ")) {
                continue;
            }

            if (existingButton == null) {
                existingButton = widget;
                widget.setMessage(worldCreationButtonLabel(config));
            }
        }

        if (existingButton != null) {
            ClickableWidget preservedButton = existingButton;
            Screens.getButtons(screen).removeIf(widget ->
                    widget != preservedButton && widget.getMessage().getString().startsWith("Daylight Dynamics: "));
            return;
        }

        int x = (scaledWidth / 2) - 105;
        int y = (scaledHeight / 2) + 10;
        int buttonWidth = 210;
        int maxRowY = Integer.MIN_VALUE;

        for (ClickableWidget widget : Screens.getButtons(screen)) {
            if (widget.getWidth() != 210) {
                continue;
            }

            int widgetCenter = widget.getX() + (widget.getWidth() / 2);
            if (Math.abs(widgetCenter - (scaledWidth / 2)) > 4) {
                continue;
            }

            if (widget.getY() > maxRowY) {
                maxRowY = widget.getY();
                x = widget.getX();
                y = widget.getY() + 28;
                buttonWidth = widget.getWidth();
            }
        }

        Screens.getButtons(screen).add(ButtonWidget.builder(worldCreationButtonLabel(config), button ->
                        client.setScreen(new DaylightDynamicsWorldCreationScreen(createWorldScreen)))
                .dimensions(x, y, buttonWidth, 20)
                .build());
    }

    private static Text worldCreationButtonLabel(DaylightDynamicsConfig config) {
        return Text.literal(config.running() ? "Daylight Dynamics: ON" : "Daylight Dynamics: OFF");
    }
}
