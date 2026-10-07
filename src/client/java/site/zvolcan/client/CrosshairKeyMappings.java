package site.zvolcan.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import site.zvolcan.CrosshairEditor;
import site.zvolcan.client.config.CrosshairConfiguration;
import site.zvolcan.client.screen.CrosshairConfigScreen;

/** Client-thread native Controls binding; defaults to backslash, rebindable in Controls. */
public final class CrosshairKeyMappings {
	private static KeyMapping openConfiguration;
	private static boolean openRequested;

	private CrosshairKeyMappings() {}

	public static void register() {
		if (openConfiguration != null) return;
		KeyMapping.Category category = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(CrosshairEditor.MOD_ID, "crosshair_editor"));
		openConfiguration = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.crosshaireditor.open_configuration", InputConstants.KEY_BACKSLASH, category));
		ClientTickEvents.END_CLIENT_TICK.register(CrosshairKeyMappings::onEndTick);
	}

	/** Queue on the client thread; the next tick coalesces this with native key clicks. */
	public static void requestOpenConfiguration() {
		openRequested = true;
	}

	private static void onEndTick(Minecraft client) {
		boolean requested = openRequested;
		openRequested = false;
		// Discard all queued clicks even while typing/in another menu: never reopen later.
		while (openConfiguration.consumeClick()) requested = true;
		if (!requested || client.level == null || client.screen != null) return;
		CrosshairConfiguration configuration = CrosshairEditorClient.getConfiguration();
		if (configuration != null) {
			client.setScreenAndShow(new CrosshairConfigScreen(null, configuration));
		}
	}
}
