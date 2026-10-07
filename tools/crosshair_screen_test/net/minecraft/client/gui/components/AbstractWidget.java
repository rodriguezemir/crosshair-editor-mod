package net.minecraft.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Only the native widget boundary; no crosshair behavior lives here. */
public abstract class AbstractWidget implements GuiEventListener {
	protected int width, height;
	private int x, y;
	private boolean focused;
	protected Component message;
	public boolean active = true, visible = true;
	public AbstractWidget(int x, int y, int width, int height, Component message) {
		this.x = x; this.y = y; this.width = width; this.height = height; this.message = message;
	}
	public int getX() { return x; }
	public int getY() { return y; }
	public int getWidth() { return width; }
	public int getHeight() { return height; }
	public int getRight() { return x + width; }
	public int getBottom() { return y + height; }
	public void setY(int y) { this.y = y; }
	public Component getMessage() { return message; }
	public boolean isFocused() { return focused; }
	public void setFocused(boolean focused) { this.focused = focused; }
	public boolean isMouseOver(double mx, double my) {
		return active && visible && mx >= x && mx < getRight() && my >= y && my < getBottom();
	}
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != 0 || !isMouseOver(event.x(), event.y())) return false;
		onClick(event, doubleClick);
		return true;
	}
	public void onClick(MouseButtonEvent event, boolean doubleClick) {}
	public final void render(GuiGraphics graphics, int mx, int my, float delta) {
		if (visible) renderWidget(graphics, mx, my, delta);
	}
	protected abstract void renderWidget(GuiGraphics graphics, int mx, int my, float delta);
	public final void updateNarration(NarrationElementOutput output) { updateWidgetNarration(output); }
	protected abstract void updateWidgetNarration(NarrationElementOutput output);
}
