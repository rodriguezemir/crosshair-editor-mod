package site.zvolcan.client.config;

import java.util.Objects;

/** Immutable, Minecraft-independent values shared by controls, storage and rendering adapters. */
public record CrosshairSettings(
	boolean enabled, boolean inverted, Type type, int size, int thickness, int gap, int rgb, int opacity
) {
	public static final int MIN_SIZE = 1;
	public static final int MAX_SIZE = 32;
	public static final int MIN_THICKNESS = 1;
	public static final int MAX_THICKNESS = 8;
	public static final int MIN_GAP = 0;
	public static final int MAX_GAP = 16;
	public static final int MIN_OPACITY = 0;
	public static final int MAX_OPACITY = 100;

	public enum Type {
		CROSS, DOT, X, CIRCLE, HEART;

		public boolean supportsThickness() {
			return this != DOT;
		}

		public boolean supportsGap() {
			return this == CROSS || this == X;
		}
	}

	public CrosshairSettings {
		Objects.requireNonNull(type, "type");
		validate("size", size, MIN_SIZE, MAX_SIZE);
		validate("thickness", thickness, MIN_THICKNESS, MAX_THICKNESS);
		validate("gap", gap, MIN_GAP, MAX_GAP);
		validate("rgb", rgb, 0, 0xFFFFFF);
		validate("opacity", opacity, MIN_OPACITY, MAX_OPACITY);
	}

	public static CrosshairSettings defaults() {
		return new CrosshairSettings(true, true, Type.CROSS, 4, 1, 0, 0xFFFFFF, 100);
	}

	/** Opacity is a percentage; nearest integer alpha (half values round up). */
	public int alpha() {
		return (opacity * 255 + 50) / 100;
	}

	public int argb() {
		return (alpha() << 24) | rgb;
	}

	public CrosshairSettings withEnabled(boolean value) {
		return new CrosshairSettings(value, inverted, type, size, thickness, gap, rgb, opacity);
	}

	public CrosshairSettings withInverted(boolean value) {
		return new CrosshairSettings(enabled, value, type, size, thickness, gap, rgb, opacity);
	}

	public CrosshairSettings withType(Type value) {
		return new CrosshairSettings(enabled, inverted, value, size, thickness, gap, rgb, opacity);
	}

	public CrosshairSettings withSize(int value) {
		return new CrosshairSettings(enabled, inverted, type, value, thickness, gap, rgb, opacity);
	}

	public CrosshairSettings withThickness(int value) {
		return new CrosshairSettings(enabled, inverted, type, size, value, gap, rgb, opacity);
	}

	public CrosshairSettings withGap(int value) {
		return new CrosshairSettings(enabled, inverted, type, size, thickness, value, rgb, opacity);
	}

	public CrosshairSettings withRgb(int value) {
		return new CrosshairSettings(enabled, inverted, type, size, thickness, gap, value, opacity);
	}

	public CrosshairSettings withOpacity(int value) {
		return new CrosshairSettings(enabled, inverted, type, size, thickness, gap, rgb, value);
	}

	private static void validate(String name, int value, int min, int max) {
		if (value < min || value > max) {
			throw new IllegalArgumentException(name + " must be in [" + min + ", " + max + "]");
		}
	}
}
