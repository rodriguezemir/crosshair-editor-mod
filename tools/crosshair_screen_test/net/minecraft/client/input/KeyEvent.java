package net.minecraft.client.input;

import com.mojang.blaze3d.platform.InputConstants;

public record KeyEvent(int key, int keycode, int modifiers) {
	public int input() { return key; }
	public boolean isConfirmation() { return key == 40 || key == 88; }
	public boolean isSelection() { return isConfirmation() || key == 44; }
	public boolean isEscape() { return key == 41; }
	public boolean isLeft() { return key == 80; }
	public boolean isRight() { return key == 79; }
	public boolean isUp() { return key == 82; }
	public boolean isDown() { return key == 81; }
	public boolean isCycleFocus() { return key == 43; }
	public boolean hasShiftDown() { return (modifiers & InputConstants.MOD_SHIFT) != 0; }
	public boolean hasControlDown() { return (modifiers & InputConstants.MOD_CONTROL) != 0; }
	public boolean isSelectAll() { return hasControlDown() && keycode == 97; }
}
