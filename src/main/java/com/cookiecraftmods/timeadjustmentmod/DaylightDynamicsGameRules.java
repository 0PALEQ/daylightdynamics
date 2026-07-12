package com.cookiecraftmods.timeadjustmentmod;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DaylightDynamicsGameRules {
    private static final DeferredRegister<GameRule<?>> GAME_RULES =
            DeferredRegister.create(BuiltInRegistries.GAME_RULE, DaylightDynamicsMod.MOD_ID);

    public static final DeferredHolder<GameRule<?>, GameRule<Boolean>> RUNNING =
            GAME_RULES.register("running", () -> booleanRule(false));
    public static final DeferredHolder<GameRule<?>, GameRule<Boolean>> REALTIME =
            GAME_RULES.register("realtime", () -> booleanRule(false));
    public static final DeferredHolder<GameRule<?>, GameRule<Integer>> DAY_LENGTH_MINUTES =
            GAME_RULES.register("day_length_minutes", () -> integerRule(24 * 60, 1));

    private DaylightDynamicsGameRules() {
    }

    public static void register(IEventBus modBus) {
        GAME_RULES.register(modBus);
    }

    public static DaylightDynamicsConfig read(GameRules gameRules) {
        DaylightDynamicsConfig defaults = DaylightDynamicsConfig.defaults();
        DaylightDynamicsConfig.Mode mode = gameRules.get(REALTIME.get())
                ? DaylightDynamicsConfig.Mode.TIMEZONE
                : DaylightDynamicsConfig.Mode.CUSTOM;
        return new DaylightDynamicsConfig(
                gameRules.get(RUNNING.get()),
                mode,
                gameRules.get(DAY_LENGTH_MINUTES.get()),
                defaults.timezoneId()
        ).sanitize();
    }

    public static void write(GameRules gameRules, DaylightDynamicsConfig config, MinecraftServer server) {
        DaylightDynamicsConfig sanitized = config.sanitize();
        gameRules.set(RUNNING.get(), sanitized.running(), server);
        gameRules.set(REALTIME.get(), sanitized.mode() == DaylightDynamicsConfig.Mode.TIMEZONE, server);
        gameRules.set(DAY_LENGTH_MINUTES.get(), sanitized.customDayLengthMinutes(), server);
    }

    private static GameRule<Boolean> booleanRule(boolean defaultValue) {
        return new GameRule<>(
                GameRuleCategory.UPDATES,
                GameRuleType.BOOL,
                BoolArgumentType.bool(),
                GameRuleTypeVisitor::visitBoolean,
                Codec.BOOL,
                value -> value ? 1 : 0,
                defaultValue,
                FeatureFlagSet.of()
        );
    }

    private static GameRule<Integer> integerRule(int defaultValue, int minimum) {
        return new GameRule<>(
                GameRuleCategory.UPDATES,
                GameRuleType.INT,
                IntegerArgumentType.integer(minimum),
                GameRuleTypeVisitor::visitInteger,
                Codec.intRange(minimum, Integer.MAX_VALUE),
                value -> value,
                defaultValue,
                FeatureFlagSet.of()
        );
    }
}
