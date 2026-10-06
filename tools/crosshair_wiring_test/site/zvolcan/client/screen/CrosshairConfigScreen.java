package site.zvolcan.client.screen;

import net.minecraft.client.gui.screens.Screen;
import site.zvolcan.client.config.CrosshairConfiguration;

/** Captures the actual opener's constructor arguments, without duplicating screen behavior. */
public final class CrosshairConfigScreen extends Screen {
	public final Screen previous;
	public final CrosshairConfiguration configuration;
	public CrosshairConfigScreen(Screen previous, CrosshairConfiguration configuration) {
		this.previous = previous; this.configuration = configuration;
	}
}
