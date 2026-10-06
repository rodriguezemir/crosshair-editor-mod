package net.fabricmc.fabric.api.client.command.v2;

import com.mojang.brigadier.CommandDispatcher;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandBuildContext;

public interface ClientCommandRegistrationCallback {
	Event EVENT = new Event();
	void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext context);

	final class Event {
		public final List<ClientCommandRegistrationCallback> callbacks = new ArrayList<>();
		public void register(ClientCommandRegistrationCallback callback) { callbacks.add(callback); }
		public void fire(CommandDispatcher<FabricClientCommandSource> dispatcher) {
			for (ClientCommandRegistrationCallback callback : callbacks) {
				callback.register(dispatcher, new CommandBuildContext());
			}
		}
	}
}
