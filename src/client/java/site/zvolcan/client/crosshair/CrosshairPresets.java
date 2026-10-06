package site.zvolcan.client.crosshair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.renderer.RenderPipelines;
import site.zvolcan.client.config.CrosshairSettings;

/** Integer pixel masks converted to disjoint row spans: no pixel is filled/inverted twice. */
public final class CrosshairPresets {
	private CrosshairPresets() {}

	/** Enabled is adapter policy; zero opacity returns an immutable crosshair with no fills. */
	public static Crosshair create(CrosshairSettings settings) {
		Objects.requireNonNull(settings, "settings");
		var pipeline = settings.inverted() ? RenderPipelines.GUI_INVERT : RenderPipelines.GUI;
		int color = renderingColor(settings);
		if (settings.opacity() == 0) {
			return new Crosshair(List.of(), pipeline, color);
		}
		List<Crosshair.Point> points = new ArrayList<>();
		// Validated settings bound every scan to at most 113 by 113 pixels.
		int bound = settings.size() + settings.gap() + settings.thickness();
		for (int y = -bound; y <= bound; y++) {
			int x = -bound;
			while (x <= bound) {
				if (!contains(settings, x, y)) {
					x++;
					continue;
				}
				int start = x++;
				while (x <= bound && contains(settings, x, y)) x++;
				points.add(new Crosshair.Point(start, y, x, y + 1));
			}
		}
		return new Crosshair(points, pipeline, color);
	}

	/**
	 * GUI_INVERT uses RGB factors ONE_MINUS_DST_COLOR / ONE_MINUS_SRC_COLOR:
	 * out = d + s * (1 - 2*d). Nonzero alpha does not attenuate RGB; the GUI shader
	 * only discards zero alpha. Scale each source channel by opacity instead, with
	 * nearest-integer rounding and opaque alpha. This interpolates the INVERT effect
	 * (white matches Difference; arbitrary RGB is not generally mathematical Difference).
	 * Normal GUI retains standard ARGB alpha semantics; settings.argb() stays raw.
	 */
	private static int renderingColor(CrosshairSettings settings) {
		if (!settings.inverted()) return settings.argb();
		int color = 0xFF000000;
		for (int shift : new int[] {16, 8, 0}) {
			int channel = (settings.rgb() >>> shift) & 255;
			int scaled = (channel * settings.opacity() + 50) / 100;
			color |= scaled << shift;
		}
		return color;
	}

	private static boolean contains(CrosshairSettings settings, int x, int y) {
		int size = settings.size();
		int thickness = settings.thickness();
		// Even widths are biased toward negative coordinates: width 2 covers [-1, 0].
		int low = -(thickness / 2);
		int high = low + thickness - 1;
		int extent = size + settings.gap();
		return switch (settings.type()) {
			case DOT -> {
				// Size is square side length; gap and thickness do not apply.
				int start = -(size / 2);
				yield between(x, start, start + size - 1) && between(y, start, start + size - 1);
			}
			case CROSS -> {
				// Size is each arm's length beyond the thickness-wide central block.
				// Positive gap excises that block plus gap pixels on each side, shifting arms outward.
				boolean arms = (between(x, low - extent, high + extent) && between(y, low, high))
					|| (between(y, low - extent, high + extent) && between(x, low, high));
				yield arms && !inGap(settings.gap(), low, high, x, y);
			}
			case X -> {
				// Stamp thickness-square brushes along both diagonals from -extent to +extent.
				// Size is diagonal center extent; remove the same central gap square after union.
				boolean diagonal = intersects(x - high, x - low, y - high, y - low, extent)
					|| intersects(x - high, x - low, low - y, high - y, extent);
				yield diagonal && !inGap(settings.gap(), low, high, x, y);
			}
		case CIRCLE -> {
			// Integer Euclidean radius; thickness cuts an inner disk, saturating to solid.
			// Outer boundary is inclusive, inner boundary exclusive; gap does not apply.
			int distanceSquared = x * x + y * y;
			int inner = Math.max(0, size - thickness);
			yield distanceSquared <= size * size && (inner == 0 || distanceSquared > inner * inner);
		}
		case HEART -> {
			// Size is heart radius; thickness cuts an inner heart, saturating to solid.
			// Outer boundary is inclusive, inner boundary exclusive; gap does not apply.
			int inner = Math.max(0, size - thickness);
			yield insideHeart(x, y, size) && (inner == 0 || !insideHeart(x, y, inner));
		}
	};
	}

	private static boolean insideHeart(int x, int y, int radius) {
		// Two round lobes meet at the top center; a triangle closes the lower half.
		int left = (2 * x + radius) * (2 * x + radius) + (2 * y + radius) * (2 * y + radius);
		int right = (2 * x - radius) * (2 * x - radius) + (2 * y + radius) * (2 * y + radius);
		if (left <= radius * radius || right <= radius * radius) return true;
		return 2 * y >= -radius && 2 * y <= 2 * radius && 3 * Math.abs(x) <= 2 * (radius - y);
	}

	private static boolean between(int value, int low, int high) {
		return value >= low && value <= high;
	}

	private static boolean inGap(int gap, int low, int high, int x, int y) {
		return gap > 0 && between(x, low - gap, high + gap) && between(y, low - gap, high + gap);
	}

	/** Whether an integer diagonal brush center belongs to both intervals and the extent. */
	private static boolean intersects(int low1, int high1, int low2, int high2, int extent) {
		return Math.max(-extent, Math.max(low1, low2)) <= Math.min(extent, Math.min(high1, high2));
	}
}
