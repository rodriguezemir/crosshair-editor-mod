package com.mojang.blaze3d.platform;

/** Native input boundary; no actual device or global key handling. */
public final class InputConstants {
	public record Key(int getValue) {}
	public static final Key UNKNOWN = new Key(-1);
	public static final int KEY_BACKSLASH = 92;
}
