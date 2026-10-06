package net.fabricmc.api;

/** Lifecycle signature only; the tests invoke initialization explicitly. */
public interface ClientModInitializer {
	void onInitializeClient();
}
