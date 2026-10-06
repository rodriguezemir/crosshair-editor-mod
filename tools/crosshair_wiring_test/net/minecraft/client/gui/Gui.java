package net.minecraft.client.gui;

import net.minecraft.client.gui.screens.Screen;

public final class Gui {
	private Screen screen;
	public Screen screen() { return screen; }
	public void setScreen(Screen screen) { this.screen = screen; }
}
