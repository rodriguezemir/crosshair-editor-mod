package net.minecraft.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;

public final class Minecraft {
	public final Gui gui = new Gui();
	public Screen screen;
	public Object level;
	public int openings;
	public void setScreenAndShow(Screen screen) { this.screen = screen; openings++; }
}
