package net.minecraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

/** Records registration/default/remapping and click queue only, not game key dispatch. */
public final class KeyMapping {
	public record Category(Identifier id) {
		public static final List<Category> registered = new ArrayList<>();
		public static Category register(Identifier id) {
			Category category = new Category(id);
			if (registered.contains(category)) throw new IllegalArgumentException("Duplicate category");
			registered.add(category);
			return category;
		}
		public String labelKey() { return id.toLanguageKey("key.category"); }
	}
	private final String name;
	private final InputConstants.Key defaultKey;
	private InputConstants.Key boundKey;
	private final Category category;
	private int clicks;
	public KeyMapping(String name, int key, Category category) {
		this.name = name; defaultKey = new InputConstants.Key(key); boundKey = defaultKey; this.category = category;
	}
	public String getName() { return name; }
	public InputConstants.Key getDefaultKey() { return defaultKey; }
	public Category getCategory() { return category; }
	public void setKey(InputConstants.Key key) { boundKey = key; }
	public InputConstants.Key boundKey() { return boundKey; }
	public void queueClicks(int count) { clicks += count; }
	public boolean consumeClick() {
		if (clicks == 0) return false;
		clicks--; return true;
	}
}
