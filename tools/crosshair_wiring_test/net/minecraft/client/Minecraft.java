package net.minecraft.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;

public final class Minecraft {
	public final Gui gui = new Gui();
	public Object level;
	public int openings;
	public void setScreenAndShow(Screen screen) { gui.setScreen(screen); openings++; }
}
