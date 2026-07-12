package com.cookiecraftmods.timeadjustmentmod.client;

import com.cookiecraftmods.timeadjustmentmod.DaylightDynamicsConfig;
import com.cookiecraftmods.timeadjustmentmod.DaylightDynamicsGameRules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.gamerules.GameRules;

public class DaylightDynamicsWorldCreationScreen extends Screen {
    private static final int PANEL_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_SPACING = 8;
    private static final int SECTION_SPACING = 10;

    private final CreateWorldScreen parent;
    private DaylightDynamicsConfig workingCopy;
    private CycleButton<DaylightDynamicsConfig.Mode> modeButton;
    private Button runningButton;
    private TimeSlider hoursSlider;
    private TimeSlider minutesSlider;

    public DaylightDynamicsWorldCreationScreen(CreateWorldScreen parent) {
        super(Component.literal("Daylight Dynamics"));
        this.parent = parent;
        this.workingCopy = DaylightDynamicsConfig.fromGameRules(parent.getUiState().getGameRules());
    }

    @Override
    protected void init() {
        int left = this.width / 2 - (PANEL_WIDTH / 2);
        int top = this.height / 2 - 70;
        int controlsTop = top + this.font.lineHeight + SECTION_SPACING;
        int customMinutes = workingCopy.customDayLengthMinutes();
        int hours = customMinutes / 60;
        int minutes = customMinutes % 60;

        modeButton = addRenderableWidget(CycleButton.builder(this::modeLabel, workingCopy.mode())
                .withValues(DaylightDynamicsConfig.Mode.values())
                .create(left, controlsTop, PANEL_WIDTH, BUTTON_HEIGHT, Component.literal("Mode"),
                        (button, value) -> {
                            workingCopy = new DaylightDynamicsConfig(
                                    workingCopy.running(),
                                    value,
                                    workingCopy.customDayLengthMinutes(),
                                    workingCopy.timezoneId()
                            ).sanitize();
                            updateWidgetState();
                        }));

        runningButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
                    workingCopy = workingCopy.withRunning(!workingCopy.running());
                    updateWidgetState();
                })
                .bounds(left, controlsTop + BUTTON_HEIGHT + ROW_SPACING, PANEL_WIDTH, BUTTON_HEIGHT)
                .build());

        hoursSlider = addRenderableWidget(new TimeSlider(left, controlsTop + ((BUTTON_HEIGHT + ROW_SPACING) * 2), PANEL_WIDTH, BUTTON_HEIGHT, 24, hours, "Hours"));
        minutesSlider = addRenderableWidget(new TimeSlider(left, controlsTop + ((BUTTON_HEIGHT + ROW_SPACING) * 3), PANEL_WIDTH, BUTTON_HEIGHT, 59, minutes, "Minutes"));

        addRenderableWidget(Button.builder(Component.literal("Apply"), button -> {
                    workingCopy = new DaylightDynamicsConfig(
                            workingCopy.running(),
                            workingCopy.mode(),
                            Math.max(1, (hoursSlider.intValue() * 60) + minutesSlider.intValue()),
                            workingCopy.timezoneId()
                    ).sanitize();
                    GameRules updatedRules = parent.getUiState().getGameRules().copy(
                            parent.getUiState().getSettings().dataConfiguration().enabledFeatures());
                    DaylightDynamicsGameRules.write(updatedRules, workingCopy, null);
                    parent.getUiState().setGameRules(updatedRules);
                    onClose();
                })
                .bounds(left, controlsTop + ((BUTTON_HEIGHT + ROW_SPACING) * 4) + SECTION_SPACING, 105, BUTTON_HEIGHT)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(left + 115, controlsTop + ((BUTTON_HEIGHT + ROW_SPACING) * 4) + SECTION_SPACING, 105, BUTTON_HEIGHT)
                .build());

        updateWidgetState();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int top = this.height / 2 - 70;
        context.drawCenteredString(this.font, this.title, centerX, top, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    private void updateWidgetState() {
        if (runningButton != null) {
            runningButton.setMessage(Component.literal(workingCopy.running() ? "Enable mod: ON" : "Enable mod: OFF"));
        }

        boolean customMode = workingCopy.mode() == DaylightDynamicsConfig.Mode.CUSTOM;
        if (hoursSlider != null) {
            hoursSlider.active = customMode;
        }
        if (minutesSlider != null) {
            minutesSlider.active = customMode;
        }
    }

    private Component modeLabel(DaylightDynamicsConfig.Mode mode) {
        if (mode == DaylightDynamicsConfig.Mode.TIMEZONE) {
            return Component.literal("Timezone (" + workingCopy.timezoneId() + ")");
        }
        return Component.literal("Custom length");
    }

    private static final class TimeSlider extends AbstractSliderButton {
        private final int maxValue;
        private final String prefix;

        private TimeSlider(int x, int y, int width, int height, int maxValue, int initialValue, String prefix) {
            super(x, y, width, height, Component.empty(), initialValue / (double) maxValue);
            this.maxValue = maxValue;
            this.prefix = prefix;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(prefix + ": " + intValue()));
        }

        @Override
        protected void applyValue() {
        }

        private int intValue() {
            return Mth.clamp((int) Math.round(this.value * maxValue), 0, maxValue);
        }
    }
}
