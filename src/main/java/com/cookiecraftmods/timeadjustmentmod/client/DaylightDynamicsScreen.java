package com.cookiecraftmods.timeadjustmentmod.client;

import com.cookiecraftmods.timeadjustmentmod.DaylightDynamicsConfig;
import com.cookiecraftmods.timeadjustmentmod.network.DaylightDynamicsNetwork;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class DaylightDynamicsScreen extends Screen {
    private static final int PANEL_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_SPACING = 8;
    private static final int SECTION_SPACING = 10;

    private DaylightDynamicsConfig workingCopy;
    private CyclingButtonWidget<DaylightDynamicsConfig.Mode> modeButton;
    private ButtonWidget runningButton;
    private TimeSlider hoursSlider;
    private TimeSlider minutesSlider;
    private ButtonWidget applyButton;

    public DaylightDynamicsScreen(DaylightDynamicsConfig config) {
        super(Text.literal("Daylight Dynamics"));
        this.workingCopy = config.copy().sanitize();
    }

    @Override
    protected void init() {
        int left = this.width / 2 - (PANEL_WIDTH / 2);
        int top = this.height / 2 - 70;
        int controlsTop = top + this.textRenderer.fontHeight + SECTION_SPACING;
        int customMinutes = workingCopy.customDayLengthMinutes();
        int hours = customMinutes / 60;
        int minutes = customMinutes % 60;

        modeButton = addDrawableChild(CyclingButtonWidget.builder(this::modeLabel)
                .values(DaylightDynamicsConfig.Mode.values())
                .initially(workingCopy.mode())
                .build(left, controlsTop, PANEL_WIDTH, BUTTON_HEIGHT, Text.literal("Mode"),
                        (button, value) -> {
                            workingCopy = new DaylightDynamicsConfig(
                                    workingCopy.running(),
                                    value,
                                    workingCopy.customDayLengthMinutes(),
                                    workingCopy.timezoneId()
                            ).sanitize();
                            updateWidgetState();
                        }));

        runningButton = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
                    workingCopy = workingCopy.withRunning(!workingCopy.running());
                    updateWidgetState();
                })
                .dimensions(left, controlsTop + BUTTON_HEIGHT + ROW_SPACING, PANEL_WIDTH, BUTTON_HEIGHT)
                .build());

        hoursSlider = addDrawableChild(new TimeSlider(left, controlsTop + ((BUTTON_HEIGHT + ROW_SPACING) * 2), PANEL_WIDTH, BUTTON_HEIGHT, 24, hours, "Hours"));
        minutesSlider = addDrawableChild(new TimeSlider(left, controlsTop + ((BUTTON_HEIGHT + ROW_SPACING) * 3), PANEL_WIDTH, BUTTON_HEIGHT, 59, minutes, "Minutes"));

        applyButton = addDrawableChild(ButtonWidget.builder(Text.literal("Apply"), button -> {
                    DaylightDynamicsConfig updated = new DaylightDynamicsConfig(
                            workingCopy.running(),
                            workingCopy.mode(),
                            Math.max(1, (hoursSlider.intValue() * 60) + minutesSlider.intValue()),
                            workingCopy.timezoneId()
                    ).sanitize();
                    workingCopy = updated;
                    DaylightDynamicsNetwork.sendUpdate(updated);
                })
                .dimensions(left, controlsTop + ((BUTTON_HEIGHT + ROW_SPACING) * 4) + SECTION_SPACING, 105, BUTTON_HEIGHT)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> close())
                .dimensions(left + 115, controlsTop + ((BUTTON_HEIGHT + ROW_SPACING) * 4) + SECTION_SPACING, 105, BUTTON_HEIGHT)
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int top = this.height / 2 - 70;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, top, 0xFFFFFF);
    }

    private void updateWidgetState() {
        if (runningButton != null) {
            runningButton.setMessage(Text.literal(workingCopy.running() ? "Enable mod: ON" : "Enable mod: OFF"));
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

    private Text modeLabel(DaylightDynamicsConfig.Mode mode) {
        if (mode == DaylightDynamicsConfig.Mode.TIMEZONE) {
            return Text.literal("Timezone (" + workingCopy.timezoneId() + ")");
        }
        return Text.literal("Custom length");
    }

    private static final class TimeSlider extends SliderWidget {
        private final int maxValue;
        private final String prefix;

        private TimeSlider(int x, int y, int width, int height, int maxValue, int initialValue, String prefix) {
            super(x, y, width, height, Text.empty(), initialValue / (double) maxValue);
            this.maxValue = maxValue;
            this.prefix = prefix;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(prefix + ": " + intValue()));
        }

        @Override
        protected void applyValue() {
        }

        private int intValue() {
            return MathHelper.clamp((int) Math.round(this.value * maxValue), 0, maxValue);
        }

        private void setValue(int rawValue) {
            this.value = MathHelper.clamp(rawValue / (double) maxValue, 0.0D, 1.0D);
            updateMessage();
        }
    }
}
