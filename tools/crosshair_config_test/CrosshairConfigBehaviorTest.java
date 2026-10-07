import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import site.zvolcan.client.config.CrosshairSettings;
import site.zvolcan.client.config.CrosshairSettings.Type;
import site.zvolcan.client.crosshair.Crosshair;
import site.zvolcan.client.crosshair.Crosshair.Point;
import site.zvolcan.client.crosshair.CrosshairPresets;

public final class CrosshairConfigBehaviorTest {
	private record Pixel(int x, int y) {}

	public static void main(String[] args) {
		switch (args[0]) {
			case "defaults" -> defaults();
			case "settings" -> settings();
			case "limits" -> limits();
			case "geometry" -> geometry();
			case "render" -> render();
			case "zeroOpacity" -> zeroOpacity();
			case "inversionBlend" -> inversionBlend();
			default -> throw new AssertionError("Unknown case");
		}
	}

	private static void defaults() {
		CrosshairSettings settings = CrosshairSettings.defaults();
		check(settings.equals(new CrosshairSettings(true, true, Type.CROSS, 4, 1, 0, 0xFFFFFF, 100)),
			"defaults expose all approved values");
		Crosshair crosshair = CrosshairPresets.create(settings);
		Set<Pixel> expected = new HashSet<>();
		for (int i = -4; i <= 4; i++) {
			expected.add(new Pixel(i, 0));
			expected.add(new Pixel(0, i));
		}
		Set<Pixel> actual = pixels(crosshair);
		check(actual.equals(expected) && actual.size() == 17, "exact default union, center once");
		check(crosshair.pipeline() == RenderPipelines.GUI_INVERT, "default inversion");
		check(crosshair.color() == 0xFFFFFFFF, "default white ARGB");
		expect(UnsupportedOperationException.class, () -> crosshair.points().clear());
	}

	private static void settings() {
		CrosshairSettings original = CrosshairSettings.defaults();
		CrosshairSettings changed = original.withEnabled(false).withInverted(false).withType(Type.X)
			.withSize(32).withThickness(8).withGap(16).withRgb(0x123456).withOpacity(50);
		check(changed.equals(new CrosshairSettings(false, false, Type.X, 32, 8, 16, 0x123456, 50)),
			"every edit preserves other fields");
		check(original.equals(CrosshairSettings.defaults()), "edits never mutate original");
		check(!changed.enabled() && !changed.inverted() && changed.type() == Type.X
			&& changed.size() == 32 && changed.thickness() == 8 && changed.gap() == 16
			&& changed.rgb() == 0x123456 && changed.opacity() == 50, "public record getters");
		check(changed.alpha() == 128 && changed.argb() == 0x80123456, "half opacity rounds up");
		check(CrosshairSettings.MIN_SIZE == 1 && CrosshairSettings.MAX_SIZE == 32
			&& CrosshairSettings.MIN_THICKNESS == 1 && CrosshairSettings.MAX_THICKNESS == 8
			&& CrosshairSettings.MIN_GAP == 0 && CrosshairSettings.MAX_GAP == 16
			&& CrosshairSettings.MIN_OPACITY == 0 && CrosshairSettings.MAX_OPACITY == 100,
			"public bounds for future controls");
		for (Type type : Type.values()) {
			check(type.supportsThickness() == (type != Type.DOT), "thickness relevance");
			check(type.supportsGap() == (type == Type.CROSS || type == Type.X), "gap relevance");
		}
		for (int value : new int[] {Integer.MIN_VALUE, 0, 33, Integer.MAX_VALUE}) {
			expect(IllegalArgumentException.class, () -> original.withSize(value));
		}
		for (int value : new int[] {Integer.MIN_VALUE, 0, 9, Integer.MAX_VALUE}) {
			expect(IllegalArgumentException.class, () -> original.withThickness(value));
		}
		for (int value : new int[] {Integer.MIN_VALUE, -1, 17, Integer.MAX_VALUE}) {
			expect(IllegalArgumentException.class, () -> original.withGap(value));
		}
		for (int value : new int[] {Integer.MIN_VALUE, -1, 101, Integer.MAX_VALUE}) {
			expect(IllegalArgumentException.class, () -> original.withOpacity(value));
		}
		for (int value : new int[] {Integer.MIN_VALUE, -1, 0x1000000, Integer.MAX_VALUE}) {
			expect(IllegalArgumentException.class, () -> original.withRgb(value));
		}
		expect(NullPointerException.class, () -> original.withType(null));
		expect(NullPointerException.class, () -> new CrosshairSettings(true, true, null, 4, 1, 0, 0, 100));
		expect(IllegalArgumentException.class,
			() -> new CrosshairSettings(true, true, Type.CROSS, 0, 1, 0, 0, 100));
		expect(NullPointerException.class, () -> CrosshairPresets.create(null));
	}

	private static void limits() {
		for (Type type : Type.values()) {
			for (int size : new int[] {1, 2, 4, 32}) {
				for (int thickness : new int[] {1, 2, 7, 8}) {
					for (int gap : new int[] {0, 1, 16}) {
						CrosshairSettings settings = CrosshairSettings.defaults().withType(type)
							.withSize(size).withThickness(thickness).withGap(gap);
						Crosshair crosshair = CrosshairPresets.create(settings);
						check(!pixels(crosshair).isEmpty(), "every valid shape has geometry: " + settings);
						check(crosshair.points().equals(CrosshairPresets.create(settings).points()),
							"deterministic rectangle ordering");
						check(crosshair.points().size() <= 113 * 57, "bounded span count");
					}
				}
			}
		}
	}

	private static void geometry() {
		CrosshairSettings base = CrosshairSettings.defaults();
		Set<Set<Pixel>> shapes = new HashSet<>();
		for (Type type : Type.values()) {
			CrosshairSettings settings = base.withType(type);
			Set<Pixel> noGap = pixels(CrosshairPresets.create(settings));
			Set<Pixel> gapped = pixels(CrosshairPresets.create(settings.withGap(2)));
			shapes.add(noGap);
			if (type.supportsGap()) {
				check(!gapped.equals(noGap) && !gapped.contains(new Pixel(0, 0)), "gap removes center");
				check(gapped.stream().noneMatch(p -> Math.abs(p.x()) <= 2 && Math.abs(p.y()) <= 2),
					"gap reserves central square");
				check(gapped.contains(new Pixel(6, type == Type.X ? 6 : 0)), "gap shifts arm end");
			} else {
				check(gapped.equals(noGap), "irrelevant gap does not change shape");
			}
			if (type == Type.DOT) {
				check(noGap.equals(pixels(CrosshairPresets.create(settings.withThickness(8)))),
					"dot ignores thickness");
			} else {
				check(!noGap.equals(pixels(CrosshairPresets.create(settings.withThickness(2)))),
					"supported thickness changes shape");
			}
		}
		check(shapes.size() == 5, "all five geometries distinct");
		Set<Pixel> dot = pixels(CrosshairPresets.create(base.withType(Type.DOT)));
		check(dot.size() == 16 && dot.contains(new Pixel(-2, -2)) && dot.contains(new Pixel(1, 1))
			&& !dot.contains(new Pixel(2, 0)), "even dot biased negative, side equals size");
		Set<Pixel> cross = pixels(CrosshairPresets.create(base.withThickness(2)));
		check(cross.contains(new Pixel(-5, -1)) && cross.contains(new Pixel(4, 0))
			&& !cross.contains(new Pixel(5, 0)), "even cross thickness biased negative");
		Set<Pixel> x = pixels(CrosshairPresets.create(base.withType(Type.X)));
		check(x.size() == 17 && x.contains(new Pixel(-4, 4)) && !x.contains(new Pixel(4, 0)),
			"one-pixel X is exactly two diagonals");
		Set<Pixel> ring = pixels(CrosshairPresets.create(base.withType(Type.CIRCLE)));
		check(ring.contains(new Pixel(4, 0)) && ring.contains(new Pixel(2, 3))
			&& !ring.contains(new Pixel(0, 0)) && !ring.contains(new Pixel(4, 1)),
			"circle uses integer Euclidean annulus including corners");
		check(pixels(CrosshairPresets.create(base.withType(Type.CIRCLE).withThickness(8)))
			.contains(new Pixel(0, 0)), "ring saturates to filled disk when thickness reaches radius");
		Set<Pixel> heart = pixels(CrosshairPresets.create(base.withType(Type.HEART)));
		check(heart.contains(new Pixel(-2, -4)) && heart.contains(new Pixel(2, -4))
			&& heart.contains(new Pixel(0, 4)) && !heart.contains(new Pixel(0, 0)),
			"heart has two lobes and a lower point");
		check(pixels(CrosshairPresets.create(base.withType(Type.HEART).withThickness(8)))
			.contains(new Pixel(0, 0)), "heart saturates when thickness reaches radius");
		for (Type type : new Type[] {Type.CROSS, Type.X}) {
			Set<Pixel> thickGap = pixels(CrosshairPresets.create(
				base.withType(type).withSize(1).withThickness(8).withGap(1)));
			check(!thickGap.contains(new Pixel(0, 0)), "thick brushes cannot refill gap");
		}
	}

	private static void render() {
		for (boolean inverted : new boolean[] {false, true}) {
			for (int opacity : new int[] {0, 1, 50, 99, 100}) {
				for (int rgb : new int[] {0, 0x123456, 0xFFFFFF}) {
					CrosshairSettings settings = CrosshairSettings.defaults().withInverted(inverted)
						.withRgb(rgb).withOpacity(opacity);
					Crosshair crosshair = CrosshairPresets.create(settings);
					int alpha = switch (opacity) {
						case 0 -> 0;
						case 1 -> 3;
						case 50 -> 128;
						case 99 -> 252;
						default -> 255;
					};
					check(settings.argb() == ((alpha << 24) | rgb), "raw ARGB remains pipeline-independent");
					int expectedColor = settings.argb();
					if (inverted) {
						expectedColor = 0xFF000000;
						for (int shift : new int[] {16, 8, 0}) {
							int channel = (rgb >>> shift) & 255;
							expectedColor |= (int) Math.round(channel * opacity / 100.0) << shift;
						}
					}
					check(crosshair.color() == expectedColor, "pipeline-specific opacity/RGB");
					check(crosshair.pipeline() == (inverted ? RenderPipelines.GUI_INVERT : RenderPipelines.GUI),
						"real pipeline constants selected");
					GuiGraphics context = new GuiGraphics();
					crosshair.draw(context, 12, -7);
					check(context.fills.size() == crosshair.points().size(), "every span reaches fill boundary");
					check(opacity == 0 ? context.fills.isEmpty() : !context.fills.isEmpty(),
						"zero opacity emits no fills, nonzero opacity retains geometry");
					for (int i = 0; i < context.fills.size(); i++) {
						var fill = context.fills.get(i);
						Point point = crosshair.points().get(i);
						check(fill.pipeline() == crosshair.pipeline() && fill.color() == crosshair.color()
							&& fill.x1() == point.x1() + 12 && fill.x2() == point.x2() + 12
							&& fill.y1() == point.y1() - 7 && fill.y2() == point.y2() - 7,
							"actual fills preserve configured color, pipeline and translation");
					}
					check(pixels(crosshair).equals(pixels(CrosshairPresets.create(settings.withEnabled(false)))),
						"enabled is adapter policy, not lost preset geometry");
				}
			}
		}
	}

	private static void zeroOpacity() {
		for (Type type : Type.values()) {
			for (boolean inverted : new boolean[] {false, true}) {
				CrosshairSettings settings = CrosshairSettings.defaults().withType(type).withInverted(inverted)
					.withSize(32).withThickness(8).withGap(16).withOpacity(0);
				Crosshair crosshair = CrosshairPresets.create(settings);
				check(crosshair.points().isEmpty(), "transparent preset has no rectangles: " + settings);
				expect(UnsupportedOperationException.class, () -> crosshair.points().add(new Point(0, 0, 1, 1)));
				GuiGraphics context = new GuiGraphics();
				crosshair.draw(context, 12, -7);
				check(context.fills.isEmpty(), "transparent preset submits no fills for either pipeline");
			}
		}
	}

	private static void inversionBlend() {
		CrosshairSettings base = CrosshairSettings.defaults();
		check(CrosshairPresets.create(base.withOpacity(50)).color() == 0xFF808080,
			"half white inversion scales RGB, not just alpha");
		check(CrosshairPresets.create(base.withRgb(0x123456).withOpacity(50)).color() == 0xFF091A2B,
			"inverted color scales each channel independently");
		check(CrosshairPresets.create(base.withInverted(false).withOpacity(50)).color() == 0x80FFFFFF,
			"normal GUI retains raw alpha and unscaled RGB");
		for (int rgb : new int[] {0, 0xFFFFFF, 0x123456, 0x0180FE, 0xFF0041}) {
			check(CrosshairPresets.create(base.withRgb(rgb)).color() == (0xFF000000 | rgb),
				"full inversion opacity preserves original RGB");
			for (int opacity = 1; opacity <= 100; opacity++) {
				int color = CrosshairPresets.create(base.withRgb(rgb).withOpacity(opacity)).color();
				check((color >>> 24) == 255, "nonzero inversion uses opaque alpha");
				for (int background : new int[] {0, 0xFFFFFF, 0x102030, 0xD08018}) {
					for (int shift : new int[] {16, 8, 0}) {
						double source = ((rgb >>> shift) & 255) / 255.0;
						double scaled = ((color >>> shift) & 255) / 255.0;
						double destination = ((background >>> shift) & 255) / 255.0;
						// Actual INVERT factors: ONE_MINUS_DST_COLOR, ONE_MINUS_SRC_COLOR.
						double actual = scaled * (1 - destination) + destination * (1 - scaled);
						double full = source * (1 - destination) + destination * (1 - source);
						double amount = opacity / 100.0;
						double expected = (1 - amount) * destination + amount * full;
						check(Math.abs(actual - expected) <= 0.5 / 255 + 1e-12,
							"inversion interpolates effect within nearest-channel quantization tolerance");
					}
				}
			}
		}
	}

	private static Set<Pixel> pixels(Crosshair crosshair) {
		Set<Pixel> result = new HashSet<>();
		for (Point point : crosshair.points()) {
			check(point.x1() < point.x2() && point.y2() == point.y1() + 1, "positive row spans");
			check(point.x1() >= -56 && point.x2() <= 57 && point.y1() >= -56 && point.y2() <= 57,
				"geometry bounded before pixel iteration");
			for (int x = point.x1(); x < point.x2(); x++) {
				check(result.add(new Pixel(x, point.y1())), "no pixel drawn twice, even thick X/ring corners");
			}
		}
		return result;
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}

	private static void expect(Class<? extends Throwable> type, Runnable action) {
		try {
			action.run();
		} catch (Throwable error) {
			if (type.isInstance(error)) return;
			throw new AssertionError("Expected " + type.getSimpleName() + ", got " + error, error);
		}
		throw new AssertionError("Expected " + type.getSimpleName());
	}
}
