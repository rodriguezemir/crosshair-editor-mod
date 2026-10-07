package net.minecraft.client.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;

/** Records only the Minecraft 1.21.11 fill boundary used by Crosshair. */
public final class GuiGraphics {
	public record Fill(RenderPipeline pipeline, int x1, int y1, int x2, int y2, int color) {}

	public final List<Fill> fills = new ArrayList<>();

	public void fill(RenderPipeline pipeline, int x1, int y1, int x2, int y2, int color) {
		fills.add(new Fill(pipeline, x1, y1, x2, y2, color));
	}
}
