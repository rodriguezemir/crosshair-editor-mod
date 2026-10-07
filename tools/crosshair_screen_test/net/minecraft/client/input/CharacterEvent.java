package net.minecraft.client.input;

/** Minecraft 1.21.11 character event includes modifier state. */
public record CharacterEvent(int codepoint, int modifiers) {}
