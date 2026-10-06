package site.zvolcan.client.crosshair;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Immutable client drawing data; rectangles are relative to the supplied center. */
public final class Crosshair {
	/** Fill bounds: inclusive left/top, exclusive right/bottom, with positive extent. */
	public record Point(int x1, int y1, int x2, int y2) {
		public Point {
			if (x1 >= x2 || y1 >= y2) {
				throw new IllegalArgumentException("Rectangle must have positive width and height");
			}
		}
	}

	private final List<Point> points;
	private final RenderPipeline pipeline;
	private final int color;

	public Crosshair(List<Point> points, RenderPipeline pipeline, int color) {
		this.points = List.copyOf(points);
		this.pipeline = Objects.requireNonNull(pipeline, "pipeline");
		this.color = color;
	}

	public List<Point> points() {
		return points;
	}

	public RenderPipeline pipeline() {
		return pipeline;
	}

	/** The packed ARGB color, including its alpha channel. */
	public int color() {
		return color;
	}

	/** Emits fills in point-list order using the configured pipeline (including GUI_INVERT). */
	public void draw(GuiGraphicsExtractor context, int centerX, int centerY) {
		draw(context, centerX, centerY, 1);
	}

	/** Same as draw, but each rectangle extent is multiplied by the integer scale. */
	public void draw(GuiGraphicsExtractor context, int centerX, int centerY, int scale) {
		Objects.requireNonNull(context, "context");
		for (Point point : points) {
			context.fill(pipeline,
				centerX + point.x1() * scale, centerY + point.y1() * scale,
				centerX + point.x2() * scale, centerY + point.y2() * scale, color);
		}
	}
}
