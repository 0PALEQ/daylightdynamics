package com.cookiecraftmods.timeadjustmentmod.client;

import com.cookiecraftmods.timeadjustmentmod.DaylightDynamicsConfig;
import com.cookiecraftmods.timeadjustmentmod.network.DaylightDynamicsNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class DaylightDynamicsScreen extends Screen {
    private static final int PANEL_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_SPACING = 8;
    private static final int SECTION_SPACING = 10;

    private DaylightDynamicsConfig workingCopy;
    private CycleButton<DaylightDynamicsConfig.Mode> modeButton;
    private Button runningButton;
    private TimeSlider hoursSlider;
    private TimeSlider minutesSlider;
    private Button applyButton;

    public DaylightDynamicsScreen(DaylightDynamicsConfig config) {
        super(Component.literal("Daylight Dynamics"));
        this.workingCopy = config.copy().sanitize();
    }

    @Override
    protected void init() {
        int left = this.width / 2 - (PANEL_WIDTH / 2);
        int top = this.height / 2 - 70;
        int controlsTop = top + this.font.lineHeight + SECTION_SPACING;
        int customMinutes = workingCopy.customDayLengthMinutes();
        int hours = customMinutes / 60;
        int minutes = customMinutes % 60;

        modeButton = addRenderableWidget(CycleButton.builder(this::modeLabel)
                .withValues(DaylightDynamicsConfig.Mode.values())
                .withInitialValue(workingCopy.mode())
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

        applyButton = addRenderableWidget(Button.builder(Component.literal("Apply"), button -> {
                    DaylightDynamicsConfig updated = new DaylightDynamicsConfig(
                            workingCopy.running(),
                            workingCopy.mode(),
                            Math.max(1, (hoursSlider.intValue() * 60) + minutesSlider.intValue()),
                            workingCopy.timezoneId()
                    ).sanitize();
                    workingCopy = updated;
                    DaylightDynamicsNetwork.sendUpdate(updated);
                })
                .bounds(left, controlsTop + ((BUTTON_HEIGHT + ROW_SPACING) * 4) + SECTION_SPACING, 105, BUTTON_HEIGHT)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
                .bounds(left + 115, controlsTop + ((BUTTON_HEIGHT + ROW_SPACING) * 4) + SECTION_SPACING, 105, BUTTON_HEIGHT)
                .build());

        updateWidgetState();
    }

    public void refreshFromServer(DaylightDynamicsConfig config) {
        workingCopy = config.copy().sanitize();
        if (hoursSlider != null && minutesSlider != null) {
            hoursSlider.setValue(workingCopy.customDayLengthMinutes() / 60);
            minutesSlider.setValue(workingCopy.customDayLengthMinutes() % 60);
        }
        if (modeButton != null) {
            modeButton.setValue(workingCopy.mode());
        }
        updateWidgetState();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int top = this.height / 2 - 70;
        context.drawCenteredString(this.font, this.title, centerX, top, 0xFFFFFF);
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
        if (applyButton != null) {
            applyButton.active = true;
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

        private void setValue(int rawValue) {
            this.value = Mth.clamp(rawValue / (double) maxValue, 0.0D, 1.0D);
            updateMessage();
        }
    }
}
