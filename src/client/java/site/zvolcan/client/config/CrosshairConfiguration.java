package site.zvolcan.client.config;

import java.util.Objects;
import site.zvolcan.client.crosshair.CrosshairManager;
import site.zvolcan.client.crosshair.CrosshairPresets;

/**
 * Single synchronous settings owner for future native controls. Construction loads
 * and applies without writing. Every update applies immediately, even if persistence
 * fails. No vanilla/HUD policy is implemented here: enabled remains a settings value.
 * A store locked during load stays locked until manual file repair and restart.
 */
public final class CrosshairConfiguration {
	public static final String CONFIGURED_NAME = "configured";

	private final CrosshairManager manager;
	private final CrosshairSettingsStore store;
	private CrosshairSettings settings;
	private boolean saved;
	private String warning;

	public CrosshairConfiguration(CrosshairManager manager, CrosshairSettingsStore store) {
		this.manager = Objects.requireNonNull(manager, "manager");
		this.store = Objects.requireNonNull(store, "store");
		CrosshairSettingsStore.LoadResult loaded = store.load();
		settings = loaded.settings();
		saved = loaded.saved();
		warning = loaded.warning();
		apply();
	}

	public CrosshairSettings settings() {
		return settings;
	}

	/** Whether the current in-memory values are known to have been persisted successfully. */
	public boolean saved() {
		return saved;
	}

	/** Session write policy; IO failure alone does not prevent a later retry. */
	public boolean writable() {
		return store.writable();
	}

	/** Empty on ordinary success; otherwise suitable for display, including unsaved/locked state. */
	public String warning() {
		return warning;
	}

	/** Equal saved values avoid IO; an equal unsaved update retries saving. No background writer. */
	public void update(CrosshairSettings replacement) {
		Objects.requireNonNull(replacement, "settings");
		boolean alreadySaved = saved && replacement.equals(settings);
		settings = replacement;
		apply();
		if (alreadySaved) return;
		CrosshairSettingsStore.SaveResult result = store.save(settings);
		saved = result.saved();
		warning = result.warning();
	}

	/** Re-registers and selects the current settings without writing. */
	public void apply() {
		manager.register(CONFIGURED_NAME, CrosshairPresets.create(settings));
		manager.select(CONFIGURED_NAME);
	}

	/** Forces a persistence attempt of the current settings; returns whether they are saved. */
	public boolean save() {
		CrosshairSettingsStore.SaveResult result = store.save(settings);
		saved = result.saved();
		warning = result.warning();
		return saved;
	}
}
