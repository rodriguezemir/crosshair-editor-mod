package net.minecraft.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;

public final class Minecraft {
	private static final Minecraft INSTANCE = new Minecraft();
	public final Font font = new Font();
	public Screen shown;
	public static Minecraft getInstance() { return INSTANCE; }
	public void setScreenAndShow(Screen screen) { shown = screen; }
}
