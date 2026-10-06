package net.minecraft.client.gui;

public final class Font {
	public final int lineHeight = 9;
	public int width(String text) { return text.length() * 6; }
	public String plainSubstrByWidth(String text, int width) {
		return text.substring(0, Math.min(text.length(), Math.max(0, width / 6)));
	}
}
