package site.zvolcan.client.crosshair;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** In-memory client registry with exact, case-sensitive names and insertion order. */
public final class CrosshairManager {
	private final Map<String, Crosshair> crosshairs = new LinkedHashMap<>();
	private final Map<String, Crosshair> registry = Collections.unmodifiableMap(crosshairs);
	private String selectedName;

	/** Replaces duplicates without reordering; the first registration becomes selected. */
	public void register(String name, Crosshair crosshair) {
		validateName(name);
		Objects.requireNonNull(crosshair, "crosshair");
		crosshairs.put(name, crosshair);
		if (selectedName == null) {
			selectedName = name;
		}
	}

	/** Returns the registered object, or null for an unknown valid name. */
	public Crosshair get(String name) {
		validateName(name);
		return crosshairs.get(name);
	}

	/** Live read-only view; callers cannot mutate the registry through entries or collections. */
	public Map<String, Crosshair> registry() {
		return registry;
	}

	/** Unknown names throw IllegalArgumentException without changing the selection. */
	public void select(String name) {
		validateName(name);
		if (!crosshairs.containsKey(name)) {
			throw new IllegalArgumentException("Unknown crosshair: " + name);
		}
		selectedName = name;
	}

	/** Returns null while empty; replacing the selected name resolves to the new object. */
	public Crosshair selected() {
		return selectedName == null ? null : crosshairs.get(selectedName);
	}

	/** Draws the current selection, or does nothing while the registry is empty. */
	public void draw(GuiGraphicsExtractor context, int centerX, int centerY) {
		Crosshair selected = selected();
		if (selected != null) {
			selected.draw(context, centerX, centerY);
		}
	}

	private static void validateName(String name) {
		Objects.requireNonNull(name, "name");
		if (name.isBlank()) {
			throw new IllegalArgumentException("Crosshair name must not be blank");
		}
	}
}
