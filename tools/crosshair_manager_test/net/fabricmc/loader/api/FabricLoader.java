package net.fabricmc.loader.api;

import java.nio.file.Path;

/** No real Fabric/game/user config fallback is permitted in headless tests. */
public final class FabricLoader {
	private static final FabricLoader INSTANCE = new FabricLoader();
	public static FabricLoader getInstance() { return INSTANCE; }
	public Path getConfigDir() {
		String directory = System.getProperty("crosshair.test.configDir");
		if (directory == null) throw new IllegalStateException("Test config directory must be supplied explicitly");
		return Path.of(directory);
	}
}
