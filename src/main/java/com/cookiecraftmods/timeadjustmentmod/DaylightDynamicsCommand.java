package com.cookiecraftmods.timeadjustmentmod;

import com.cookiecraftmods.timeadjustmentmod.network.DaylightDynamicsNetwork;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class DaylightDynamicsCommand {
    private DaylightDynamicsCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("daylightdynamics")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> openScreen(context.getSource())));
    }

    private static int openScreen(ServerCommandSource source) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("This command must be executed by a player."));
            return 0;
        }

        DaylightDynamicsNetwork.openScreen(player, DaylightDynamicsMod.state().snapshot());
        source.sendFeedback(() -> Text.literal("Opened Daylight Dynamics settings."), false);
        return 1;
    }
}
