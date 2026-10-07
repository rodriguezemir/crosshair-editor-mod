package net.fabricmc.fabric.api.client.command.v2;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

/** The Minecraft 1.21.11 Fabric factory delegates parsing/execution to real Brigadier. */
public final class ClientCommandManager {
	public static LiteralArgumentBuilder<FabricClientCommandSource> literal(String name) {
		return LiteralArgumentBuilder.literal(name);
	}
}
