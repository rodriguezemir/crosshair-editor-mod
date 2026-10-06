package net.fabricmc.fabric.api.client.event.lifecycle.v1;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

/** Synchronous event boundary allowing the actual registered callback to run. */
public final class ClientTickEvents {
	public interface EndTick { void onEndTick(Minecraft client); }
	public static final EndTickEvent END_CLIENT_TICK = new EndTickEvent();
	public static final class EndTickEvent {
		public final List<EndTick> callbacks = new ArrayList<>();
		public void register(EndTick callback) { callbacks.add(callback); }
		public void fire(Minecraft client) { for (EndTick callback : callbacks) callback.onEndTick(client); }
	}
}
