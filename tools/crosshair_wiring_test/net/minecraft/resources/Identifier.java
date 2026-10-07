package net.minecraft.resources;

/** Identifier/translation boundary verified separately against the cached 1.21.11 bytecode. */
public record Identifier(String namespace, String path) {
	public static Identifier fromNamespaceAndPath(String namespace, String path) {
		return new Identifier(namespace, path);
	}
	public String toLanguageKey(String prefix) { return prefix + "." + namespace + "." + path.replace('/', '.'); }
}
