package net.minecraft.client.gui;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

/** Records native extraction commands, including scissor state. Not a GPU renderer. */
public final class GuiGraphicsExtractor {
	public record Clip(int left, int top, int right, int bottom) {}
	public record Fill(int x1, int y1, int x2, int y2, int color, Clip clip, RenderPipeline pipeline) {}
	public record Text(String value, int x, int y, int color, boolean shadow, Clip clip) {}
	public final List<Fill> fills = new ArrayList<>();
	public final List<Text> texts = new ArrayList<>();
	public Component tooltip;
	private Clip clip;
	public void fill(int x1, int y1, int x2, int y2, int color) {
		fills.add(new Fill(x1, y1, x2, y2, color, clip, null));
	}
	public void fill(RenderPipeline pipeline, int x1, int y1, int x2, int y2, int color) {
		fills.add(new Fill(x1, y1, x2, y2, color, clip, pipeline));
	}
	public void text(Font font, String text, int x, int y, int color, boolean shadow) {
		texts.add(new Text(text, x, y, color, shadow, clip));
	}
	public void text(Font font, Component text, int x, int y, int color, boolean shadow) {
		text(font, text.getString(), x, y, color, shadow);
	}
	public void enableScissor(int left, int top, int right, int bottom) {
		clip = new Clip(left, top, right, bottom);
	}
	public void disableScissor() { clip = null; }
	public void setTooltipForNextFrame(Font font, Component text, int x, int y) { tooltip = text; }
}
