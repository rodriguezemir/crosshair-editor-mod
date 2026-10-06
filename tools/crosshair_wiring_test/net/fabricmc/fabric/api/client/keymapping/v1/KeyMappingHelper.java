package net.fabricmc.fabric.api.client.keymapping.v1;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;

public final class KeyMappingHelper {
	public static final List<KeyMapping> registered = new ArrayList<>();
	public static KeyMapping registerKeyMapping(KeyMapping key) { registered.add(key); return key; }
}
