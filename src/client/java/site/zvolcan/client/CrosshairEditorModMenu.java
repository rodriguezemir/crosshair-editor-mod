package site.zvolcan.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import site.zvolcan.client.config.CrosshairConfiguration;
import site.zvolcan.client.screen.CrosshairConfigScreen;

/** Mod Menu entrypoint; optional at runtime, only loaded when Mod Menu is present. */
public final class CrosshairEditorModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> {
			CrosshairConfiguration configuration = CrosshairEditorClient.getConfiguration();
			return configuration == null ? null : new CrosshairConfigScreen(parent, configuration);
		};
	}
}
