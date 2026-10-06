package net.fabricmc.fabric.api.client.command.v2;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

/** The Fabric factory boundary delegates parsing and execution to real Brigadier. */
public final class ClientCommands {
	public static LiteralArgumentBuilder<FabricClientCommandSource> literal(String name) {
		return LiteralArgumentBuilder.literal(name);
	}
}
