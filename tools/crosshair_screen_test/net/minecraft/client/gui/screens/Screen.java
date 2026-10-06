package net.minecraft.client.gui.screens;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;

/** Minimal container and lifecycle boundary, deliberately no navigation policy. */
public abstract class Screen {
	protected final Minecraft minecraft = Minecraft.getInstance();
	protected final Font font = minecraft.font;
	protected final Component title;
	public int width, height;
	private final List<AbstractWidget> widgets = new ArrayList<>();
	private GuiEventListener focused;
	private boolean initialized;
	protected Screen(Component title) { this.title = title; }
	public final void init(int width, int height) {
		this.width = width; this.height = height;
		if (initialized) rebuildWidgets();
		else { init(); setInitialFocus(); }
		initialized = true;
	}
	protected void rebuildWidgets() { clearWidgets(); clearFocus(); init(); setInitialFocus(); }
	protected void init() {}
	protected void setInitialFocus() {}
	protected <T extends AbstractWidget> T addRenderableWidget(T widget) { widgets.add(widget); return widget; }
	protected void clearWidgets() { widgets.clear(); }
	public List<? extends GuiEventListener> children() { return widgets; }
	public GuiEventListener getFocused() { return focused; }
	public void setFocused(GuiEventListener widget) {
		if (focused == widget) return;
		if (focused != null) focused.setFocused(false);
		if (widget != null) widget.setFocused(true);
		focused = widget;
	}
	public void clearFocus() { setFocused(null); }
	public boolean keyPressed(KeyEvent event) {
		if (event.isEscape()) { onClose(); return true; }
		return focused != null && focused.keyPressed(event);
	}
	public boolean charTyped(CharacterEvent event) { return focused != null && focused.charTyped(event); }
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) { return false; }
	public boolean mouseScrolled(double x, double y, double dx, double dy) { return false; }
	public void resize(int width, int height) {
		this.width = width; this.height = height; rebuildWidgets();
	}
	public void removed() {}
	public void onClose() {}
	public void extractBackground(GuiGraphicsExtractor graphics, int x, int y, float delta) {}
	public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float delta) {}
	protected void updateNarrationState(NarrationElementOutput output) {}
	public Component getNarrationMessage() { return title; }
}
