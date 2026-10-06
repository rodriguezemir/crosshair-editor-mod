package net.minecraft.client.gui.components.events;

import net.minecraft.client.input.*;

public interface GuiEventListener {
	default boolean keyPressed(KeyEvent event) { return false; }
	default boolean charTyped(CharacterEvent event) { return false; }
	default boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) { return false; }
	default boolean isMouseOver(double x, double y) { return false; }
	void setFocused(boolean focused);
	boolean isFocused();
}
