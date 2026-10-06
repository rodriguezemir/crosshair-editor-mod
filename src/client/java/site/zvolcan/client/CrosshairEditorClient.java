package site.zvolcan.client;

import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.RenderPipelines;
import site.zvolcan.CrosshairEditor;
import site.zvolcan.client.config.CrosshairConfiguration;
import site.zvolcan.client.config.CrosshairSettingsStore;
import site.zvolcan.client.crosshair.Crosshair;
import site.zvolcan.client.crosshair.Crosshair.Point;
import site.zvolcan.client.crosshair.CrosshairManager;

public class CrosshairEditorClient implements ClientModInitializer {
	private static final CrosshairManager CROSSHAIR_MANAGER = new CrosshairManager();
	private static CrosshairConfiguration configuration;

	/** Shared in-memory registry; use only on the client/render thread. */
	public static CrosshairManager getCrosshairManager() {
		return CROSSHAIR_MANAGER;
	}

	/** Shared live/persisted settings owner; null before client initialization. */
	public static CrosshairConfiguration getConfiguration() {
		return configuration;
	}

	/** Preserve manager drawing safely before configuration is initialized. */
	public static boolean isCustomCrosshairEnabled() {
		return configuration == null || configuration.settings().enabled();
	}

	@Override
	public void onInitializeClient() {
		if (configuration != null) return;
		CROSSHAIR_MANAGER.register("default", new Crosshair(
			List.of(new Point(-4, 0, 5, 1), new Point(0, -4, 1, 0), new Point(0, 1, 1, 5)),
			RenderPipelines.GUI_INVERT, 0xFFFFFFFF));
		CROSSHAIR_MANAGER.select("default");
		configuration = new CrosshairConfiguration(CROSSHAIR_MANAGER,
			new CrosshairSettingsStore(FabricLoader.getInstance().getConfigDir().resolve("crosshaireditor.json")));
		if (!configuration.warning().isEmpty()) {
			CrosshairEditor.LOGGER.warn("{}", configuration.warning());
		}
		CrosshairKeyMappings.register();
		CrosshairCommands.register();
	}
}