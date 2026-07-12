package com.cookiecraftmods.timeadjustmentmod;

import com.mojang.brigadier.CommandDispatcher;
import com.cookiecraftmods.timeadjustmentmod.network.DaylightDynamicsNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class DaylightDynamicsCommand {
    private DaylightDynamicsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("daylightdynamics")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> openScreen(context.getSource())));
    }

    private static int openScreen(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command must be executed by a player."));
            return 0;
        }

        DaylightDynamicsNetwork.openScreen(player, DaylightDynamicsMod.state().snapshot());
        source.sendSuccess(() -> Component.literal("Opened Daylight Dynamics settings."), false);
        return 1;
    }
}
