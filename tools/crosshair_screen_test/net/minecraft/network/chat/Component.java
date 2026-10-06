package net.minecraft.network.chat;

public record Component(String getString) {
	public static Component literal(String value) { return new Component(value); }
}
