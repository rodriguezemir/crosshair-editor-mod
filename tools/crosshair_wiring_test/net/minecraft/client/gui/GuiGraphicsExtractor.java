package net.minecraft.client.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

/** Records only actual fill and sprite-call boundaries, not GPU rendering. */
public final class GuiGraphicsExtractor {
	public record Fill(RenderPipeline pipeline, int x1, int y1, int x2, int y2, int color) {}
	public record Blit(RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {}
	public final List<Fill> fills = new ArrayList<>();
	public final List<Blit> blits = new ArrayList<>();
	public void fill(RenderPipeline pipeline, int x1, int y1, int x2, int y2, int color) {
		fills.add(new Fill(pipeline, x1, y1, x2, y2, color));
	}
	public void blitSprite(RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
		blits.add(new Blit(pipeline, sprite, x, y, width, height));
	}
}
