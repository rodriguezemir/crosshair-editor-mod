package net.fabricmc.fabric.api.client.keybinding.v1;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;

public final class KeyBindingHelper {
	public static final List<KeyMapping> registered = new ArrayList<>();
	public static KeyMapping registerKeyBinding(KeyMapping key) { registered.add(key); return key; }
}
