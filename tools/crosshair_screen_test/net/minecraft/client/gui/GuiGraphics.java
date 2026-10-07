package net.minecraft.client.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

/** Records Minecraft 1.21.11 drawing commands and scissor state. Not a GPU renderer. */
public final class GuiGraphics {
	public record Clip(int left, int top, int right, int bottom) {}
	public record Fill(int x1, int y1, int x2, int y2, int color, Clip clip, RenderPipeline pipeline) {}
	public record Text(String value, int x, int y, int color, boolean shadow, Clip clip) {}
	public record MenuBackground(int left, int top, int right, int bottom) {}
	public final List<Fill> fills = new ArrayList<>();
	public final List<Text> texts = new ArrayList<>();
	public final List<MenuBackground> menuBackgrounds = new ArrayList<>();
	public final List<String> events = new ArrayList<>();
	public Component tooltip;
	private Clip clip;
	public void fill(int x1, int y1, int x2, int y2, int color) {
		events.add("fill");
		fills.add(new Fill(x1, y1, x2, y2, color, clip, null));
	}
	public void fill(RenderPipeline pipeline, int x1, int y1, int x2, int y2, int color) {
		events.add("fill");
		fills.add(new Fill(x1, y1, x2, y2, color, clip, pipeline));
	}
	public void drawString(Font font, String text, int x, int y, int color, boolean shadow) {
		events.add("text");
		texts.add(new Text(text, x, y, color, shadow, clip));
	}
	public void drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
		drawString(font, text.getString(), x, y, color, shadow);
	}
	public void enableScissor(int left, int top, int right, int bottom) {
		clip = new Clip(left, top, right, bottom);
	}
	public void disableScissor() { clip = null; }
	public void setTooltipForNextFrame(Font font, Component text, int x, int y) { tooltip = text; }
	public void nextStratum() { events.add("nextStratum"); }
	public void blurBeforeThisStratum() { events.add("blur"); }
	public void renderDeferredElements() { events.add("deferredElements"); }

	// Fixture-only observations of inherited Screen behavior; no texture or GPU simulation.
	public void recordMenuBackground(int left, int top, int right, int bottom) {
		events.add("menuBackground");
		menuBackgrounds.add(new MenuBackground(left, top, right, bottom));
	}
	public void recordDeferredSubtitles() { events.add("deferredSubtitles"); }
}
