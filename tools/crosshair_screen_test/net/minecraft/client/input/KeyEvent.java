package net.minecraft.client.input;

import com.mojang.blaze3d.platform.InputConstants;

/** Narrow Linux GLFW boundary verified against 1.21.11 InputWithModifiers bytecode. */
public record KeyEvent(int key, int scancode, int modifiers) {
	public int input() { return key; }
	public boolean isConfirmation() { return key == 257 || key == 335; }
	public boolean isSelection() { return isConfirmation() || key == 32; }
	public boolean isEscape() { return key == 256; }
	public boolean isLeft() { return key == 263; }
	public boolean isRight() { return key == 262; }
	public boolean isUp() { return key == 265; }
	public boolean isDown() { return key == 264; }
	public boolean isCycleFocus() { return key == 258; }
	public boolean hasShiftDown() { return (modifiers & InputConstants.MOD_SHIFT) != 0; }
	public boolean hasControlDown() { return (modifiers & InputConstants.MOD_CONTROL) != 0; }
	public boolean isSelectAll() { return hasControlDown() && key == 65 && !hasShiftDown() && (modifiers & 4) == 0; }
}
