package net.fabricmc.fabric.api.client.command.v2;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public interface FabricClientCommandSource {
	Minecraft getClient();
	void sendError(Component message);
}
