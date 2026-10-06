package site.zvolcan.client;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Client-only entry point; opening waits until chat has finished submitting the command. */
public final class CrosshairCommands {
	private static boolean registered;

	private CrosshairCommands() {}

	public static void register() {
		if (registered) return;
		registered = true;
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) ->
			dispatcher.register(ClientCommands.literal("crosshaireditor").executes(command -> {
				Minecraft client = command.getSource().getClient();
				if (client == null || client.level == null) {
					command.getSource().sendError(Component.literal("Open a world before using /crosshaireditor."));
					return 0;
				}
				if (CrosshairEditorClient.getConfiguration() == null) {
					command.getSource().sendError(Component.literal("Crosshair configuration is not available yet."));
					return 0;
				}
				CrosshairKeyMappings.requestOpenConfiguration();
				return 1;
			})));
	}
}
