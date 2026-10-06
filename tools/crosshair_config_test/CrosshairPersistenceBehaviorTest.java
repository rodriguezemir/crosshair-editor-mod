import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import site.zvolcan.client.config.CrosshairConfiguration;
import site.zvolcan.client.config.CrosshairSettings;
import site.zvolcan.client.config.CrosshairSettings.Type;
import site.zvolcan.client.config.CrosshairSettingsStore;
import site.zvolcan.client.crosshair.Crosshair;
import site.zvolcan.client.crosshair.CrosshairManager;
import site.zvolcan.client.crosshair.CrosshairPresets;

public final class CrosshairPersistenceBehaviorTest {
	private static final String VALID = """
		{"version":1,"enabled":true,"inverted":true,"type":"CROSS",
		 "size":4,"thickness":1,"gap":0,"rgb":16777215,"opacity":100}
		""";

	public static void main(String[] args) throws Exception {
		Path root = Path.of(args[1]);
		switch (args[0]) {
			case "roundTrip" -> roundTrip(root);
			case "invalid" -> invalid(root);
			case "unreadable" -> unreadable(root);
			case "saveFailure" -> saveFailure(root);
			case "controller" -> controller(root);
			case "controllerFailure" -> controllerFailure(root);
			case "fallback" -> fallback(root);
			default -> throw new AssertionError("Unknown case");
		}
	}

	private static CrosshairSettings alternate() {
		return new CrosshairSettings(false, false, Type.CIRCLE, 32, 8, 16, 0x123456, 50);
	}

	private static void roundTrip(Path root) throws Exception {
		Path file = root.resolve("nested/config.json");
		CrosshairSettingsStore store = new CrosshairSettingsStore(file);
		var missing = store.load();
		check(missing.settings().equals(CrosshairSettings.defaults()) && !missing.saved()
			&& missing.writable() && missing.warning().isEmpty(), "missing file uses writable, unsaved defaults");
		check(!Files.exists(file), "loading does not create config");
		for (Type type : Type.values()) {
			CrosshairSettings settings = alternate().withType(type);
			var saved = store.save(settings);
			check(saved.saved() && saved.warning().isEmpty(), "successful save is explicit");
			var loaded = new CrosshairSettingsStore(file).load();
			check(loaded.saved() && loaded.writable() && loaded.warning().isEmpty()
				&& loaded.settings().equals(settings), "round-trip every field and type");
			JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
			check(json.size() == 9 && json.get("version").getAsInt() == 1
				&& json.get("rgb").getAsInt() == 0x123456 && json.get("type").getAsString().equals(type.name()),
				"flat version-one schema, numeric RGB and named type");
			check(directoryEntries(file.getParent()) == 1, "successful save leaves no temporary siblings");
		}
		Files.writeString(file, VALID.replace("\"version\":1", "\"extra\":true,\"version\":1"));
		check(new CrosshairSettingsStore(file).load().saved(), "unknown fields allowed for version one");
	}

	private static void invalid(Path root) throws Exception {
		List<String> invalid = List.of("", "{", "null", "[]", "{}", VALID + " {}",
			VALID.replace("\"version\":1", "\"version\":2"),
			VALID.replace("\"version\":1", "\"version\":\"1\""),
			VALID.replace("\"size\":4", "\"size\":33"),
			VALID.replace("\"size\":4", "\"size\":4.5"),
			VALID.replace("\"size\":4", "\"size\":4294967300"),
			VALID.replace("\"size\":4", "\"size\":\"4\""),
			VALID.replace("\"enabled\":true", "\"enabled\":\"true\""),
			VALID.replace("\"inverted\":true", "\"inverted\":1"),
			VALID.replace("\"type\":\"CROSS\"", "\"type\":\"TRIANGLE\""),
			VALID.replace("\"type\":\"CROSS\"", "\"type\":null"),
			VALID.replace("\"thickness\":1", "\"thickness\":0"),
			VALID.replace("\"gap\":0", "\"gap\":17"),
			VALID.replace("\"rgb\":16777215", "\"rgb\":16777216"),
			VALID.replace("\"opacity\":100", "\"opacity\":-1"),
			VALID.replace("\"size\"", "size"), // Gson must not accept lenient unquoted keys.
			VALID.replace("\"size\":4", "/* comment */\"size\":4"));
		int index = 0;
		for (String document : invalid) assertLocked(root.resolve("invalid" + index++ + ".json"), document);
		for (String field : List.of("version", "enabled", "inverted", "type", "size", "thickness", "gap", "rgb", "opacity")) {
			JsonObject json = JsonParser.parseString(VALID).getAsJsonObject();
			json.remove(field);
			assertLocked(root.resolve("missing-" + field + ".json"), json.toString());
			json.add(field, null);
			assertLocked(root.resolve("null-" + field + ".json"), json.toString());
		}
	}

	private static void assertLocked(Path file, String document) throws Exception {
		Files.writeString(file, document);
		byte[] original = Files.readAllBytes(file);
		CrosshairSettingsStore store = new CrosshairSettingsStore(file);
		var result = store.load();
		check(result.settings().equals(CrosshairSettings.defaults()) && !result.saved() && !result.writable()
			&& !result.warning().isBlank(), "invalid config uses warned, locked defaults: " + file);
		check(!store.save(alternate()).saved() && !store.writable(), "invalid original cannot be overwritten");
		check(Arrays.equals(original, Files.readAllBytes(file)), "invalid bytes preserved exactly");
	}

	private static void unreadable(Path root) throws Exception {
		Path directory = Files.createDirectory(root.resolve("config.json"));
		Path sentinel = directory.resolve("keep");
		Files.writeString(sentinel, "unchanged");
		CrosshairSettingsStore store = new CrosshairSettingsStore(directory);
		check(!store.load().writable() && !store.load().warning().isBlank(), "non-file config is unreadable");
		check(!store.save(alternate()).saved() && Files.readString(sentinel).equals("unchanged"),
			"unreadable path not replaced");
		assertLocked(root.resolve("oversized.json"), " ".repeat(16 * 1024 + 1));
		Path utf8 = root.resolve("invalid-utf8.json");
		byte[] original = {(byte) 0xC3, (byte) 0x28};
		Files.write(utf8, original);
		CrosshairSettingsStore malformed = new CrosshairSettingsStore(utf8);
		check(!malformed.load().writable() && !malformed.save(alternate()).saved(), "invalid UTF-8 is locked");
		check(Arrays.equals(original, Files.readAllBytes(utf8)), "invalid UTF-8 bytes preserved");
	}

	private static long directoryEntries(Path directory) throws Exception {
		try (var entries = Files.list(directory)) {
			return entries.count();
		}
	}

	private static void saveFailure(Path root) throws Exception {
		Path parent = root.resolve("blocked");
		CrosshairSettingsStore blocked = new CrosshairSettingsStore(parent.resolve("config.json"));
		check(blocked.load().writable(), "initial missing path is writable policy");
		Files.writeString(parent, "not a directory");
		var failed = blocked.save(alternate());
		check(!failed.saved() && !failed.warning().isBlank() && blocked.writable(), "save IO failure surfaces, permits retry");
		check(Files.readString(parent).equals("not a directory"), "failed save does not alter blocking file");

		Path destination = root.resolve("collision.json");
		CrosshairSettingsStore collision = new CrosshairSettingsStore(destination);
		collision.load();
		Files.createDirectory(destination);
		Files.writeString(destination.resolve("sentinel"), "prior content");
		failed = collision.save(alternate());
		check(!failed.saved() && !failed.warning().isBlank(), "failed replacement never reports saved");
		check(Files.readString(destination.resolve("sentinel")).equals("prior content"), "replacement failure preserves target");
		check(directoryEntries(root) == 2, "failed replacement cleans its temporary sibling");
	}

	private static void controller(Path root) throws Exception {
		Path file = root.resolve("config.json");
		CrosshairManager manager = new CrosshairManager();
		Crosshair original = CrosshairPresets.create(CrosshairSettings.defaults());
		manager.register("default", original);
		manager.register("external", original);
		CrosshairConfiguration configuration = new CrosshairConfiguration(manager, new CrosshairSettingsStore(file));
		check(CrosshairConfiguration.CONFIGURED_NAME.equals("configured"), "dedicated registry name");
		check(configuration.settings().equals(CrosshairSettings.defaults()) && !configuration.saved()
			&& configuration.writable() && configuration.warning().isEmpty(), "construction exposes load status");
		check(manager.selected() == manager.get("configured"), "construction selects configured object");
		configuration.update(alternate());
		Crosshair expected = CrosshairPresets.create(alternate());
		check(manager.selected().points().equals(expected.points()) && manager.selected().color() == expected.color()
			&& manager.selected().pipeline() == expected.pipeline(), "committed updates apply real preset settings");
		check(!configuration.settings().enabled() && !manager.selected().points().isEmpty(), "disabled state retains geometry");
		check(configuration.saved() && configuration.writable() && configuration.warning().isEmpty(), "update saved successfully");
		check(manager.get("default") == original && manager.get("external") == original && manager.registry().size() == 3,
			"controller preserves default and external registrations");
		FileTime marker = FileTime.fromMillis(1_000_000);
		Files.setLastModifiedTime(file, marker);
		configuration.update(alternate());
		check(Files.getLastModifiedTime(file).equals(marker), "equal saved updates do not write");
		CrosshairManager restarted = new CrosshairManager();
		CrosshairConfiguration reloaded = new CrosshairConfiguration(restarted, new CrosshairSettingsStore(file));
		check(reloaded.settings().equals(alternate()) && reloaded.saved(), "controller reload applies saved settings");
		check(restarted.selected().points().equals(expected.points()), "loaded settings applied to manager");
		try {
			configuration.update(null);
			throw new AssertionError("null update accepted");
		} catch (NullPointerException expectedError) {
			check(configuration.settings().equals(alternate()) && configuration.saved(), "invalid update preserves state");
		}
	}

	private static void controllerFailure(Path root) throws Exception {
		Path file = root.resolve("invalid.json");
		Files.writeString(file, "broken");
		CrosshairManager manager = new CrosshairManager();
		CrosshairConfiguration locked = new CrosshairConfiguration(manager, new CrosshairSettingsStore(file));
		locked.update(alternate());
		check(locked.settings().equals(alternate()) && !locked.saved() && !locked.writable()
			&& !locked.warning().isBlank(), "locked file still permits live edits with explicit unsaved status");
		check(manager.selected().color() == CrosshairPresets.create(alternate()).color()
			&& Files.readString(file).equals("broken"), "locked edit applies without touching original");
		Files.writeString(file, VALID); // Manual repair must not silently unlock this session.
		locked.update(alternate().withSize(2));
		check(!locked.writable() && Files.readString(file).equals(VALID), "lock remains until restart");
		check(new CrosshairConfiguration(new CrosshairManager(), new CrosshairSettingsStore(file)).writable(),
			"restart after manual repair permits persistence");

		Path parent = root.resolve("save-blocker");
		CrosshairConfiguration failed = new CrosshairConfiguration(manager,
			new CrosshairSettingsStore(parent.resolve("config.json")));
		Files.writeString(parent, "block");
		failed.update(alternate());
		check(failed.settings().equals(alternate()) && !failed.saved() && failed.writable()
			&& !failed.warning().isBlank(), "failed save preserves live settings and reports unsaved");
		check(manager.selected().points().equals(CrosshairPresets.create(alternate()).points()), "failed save still live-applies");
		failed.update(alternate().withInverted(true).withOpacity(50));
		check(!failed.saved() && manager.selected().color() == 0xFF091A2B, "later failed save uses corrected inversion opacity");
	}

	private static void fallback(Path root) throws Exception {
		// ZIP provider rejects ATOMIC_MOVE; use real JDK fallback without mocking file APIs.
		try (var zip = FileSystems.newFileSystem(root.resolve("config.zip"), Map.of("create", "true"))) {
			CrosshairSettingsStore store = new CrosshairSettingsStore(zip.getPath("/config.json"));
			check(store.save(alternate()).saved(), "unsupported atomic move falls back to replacement");
			check(store.save(CrosshairSettings.defaults()).saved(), "fallback replaces existing config");
			check(new CrosshairSettingsStore(zip.getPath("/config.json")).load().settings().equals(CrosshairSettings.defaults()),
				"fallback file round-trips");
			check(directoryEntries(zip.getPath("/")) == 1, "fallback leaves no temporary files");
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
