package site.zvolcan.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

/**
 * Path-bound, synchronous JSON persistence with no Minecraft dependency.
 * Version 1 is a flat object: version plus every CrosshairSettings field. All fields
 * are required and non-null; booleans/strings are never coerced, numeric fields must
 * be exact integers. Type uses its uppercase enum name; rgb is a numeric RGB24.
 * Unknown fields are ignored. UTF-8 input is bounded to 16 KiB and strictly parsed.
 * An invalid/unreadable original permanently locks this store instance: manually
 * fix the file and restart (construct a new store) to recover. No automatic repair.
 */
public final class CrosshairSettingsStore {
	public static final int VERSION = 1;
	private static final int MAX_BYTES = 16 * 1024;
	private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).setPrettyPrinting().create();

	/** Saved means settings came from a valid file; missing defaults are not saved. Warning is never null. */
	public record LoadResult(CrosshairSettings settings, boolean saved, boolean writable, String warning) {}
	public record SaveResult(boolean saved, String warning) {}

	private final Path path;
	private LoadResult loaded;

	public CrosshairSettingsStore(Path path) {
		this.path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
	}

	public Path path() {
		return path;
	}

	/** Load once per session; successful saves refresh the snapshot. External edits are not watched. */
	public LoadResult load() {
		if (loaded != null) return loaded;
		try {
			BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
			if (!attributes.isRegularFile()) throw new IOException("Config path is not a regular file");
			byte[] bytes;
			try (var input = Files.newInputStream(path)) {
				bytes = input.readNBytes(MAX_BYTES + 1);
			}
			if (bytes.length > MAX_BYTES) throw new IOException("Config exceeds 16 KiB");
			String text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
			loaded = new LoadResult(decode(text), true, true, "");
		} catch (NoSuchFileException missing) {
			loaded = new LoadResult(CrosshairSettings.defaults(), false, true, "");
		} catch (IOException | SecurityException | JsonParseException | IllegalArgumentException failure) {
			String warning = "Cannot load crosshair settings from " + path + ": " + failure.getMessage()
				+ ". Using defaults; saving is disabled to preserve the original. Fix the file manually and restart.";
			loaded = new LoadResult(CrosshairSettings.defaults(), false, false, warning);
		}
		return loaded;
	}

	/** Writable is session policy, not a promise that the filesystem will accept the next save. */
	public boolean writable() {
		return load().writable();
	}

	/** Stage a complete sibling file, then replace; never truncate the destination in place. */
	public SaveResult save(CrosshairSettings settings) {
		Objects.requireNonNull(settings, "settings");
		if (!writable()) return new SaveResult(false, "Crosshair settings are active but NOT saved. " + loaded.warning());
		Path temporary = null;
		boolean saved = false;
		String warning = "";
		try {
			Files.createDirectories(path.getParent());
			temporary = Files.createTempFile(path.getParent(), ".crosshair-", ".tmp");
			Files.writeString(temporary, JSON.toJson(encode(settings)) + "\n", StandardCharsets.UTF_8);
			try {
				Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException unsupported) {
				// Same-directory replacement is the best available fallback, not crash-atomic.
				Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
			}
			saved = true;
			loaded = new LoadResult(settings, true, true, "");
		} catch (IOException | SecurityException failure) {
			warning = "Crosshair settings are active but NOT saved to " + path + ": " + failure.getMessage();
		} finally {
			if (temporary != null) {
				try {
					Files.deleteIfExists(temporary);
				} catch (IOException | SecurityException cleanupFailure) {
					warning += " Temporary config cleanup failed: " + cleanupFailure.getMessage();
				}
			}
		}
		return new SaveResult(saved, warning);
	}

	private static CrosshairSettings decode(String text) {
		JsonObject object = JSON.fromJson(text, JsonObject.class);
		if (object == null) throw new IllegalArgumentException("Expected a JSON object");
		if (integer(object, "version") != VERSION) throw new IllegalArgumentException("Unsupported config version");
		return new CrosshairSettings(bool(object, "enabled"), bool(object, "inverted"),
			CrosshairSettings.Type.valueOf(string(object, "type")), integer(object, "size"),
			integer(object, "thickness"), integer(object, "gap"), integer(object, "rgb"), integer(object, "opacity"));
	}

	private static JsonObject encode(CrosshairSettings settings) {
		JsonObject object = new JsonObject();
		object.addProperty("version", VERSION);
		object.addProperty("enabled", settings.enabled());
		object.addProperty("inverted", settings.inverted());
		object.addProperty("type", settings.type().name());
		object.addProperty("size", settings.size());
		object.addProperty("thickness", settings.thickness());
		object.addProperty("gap", settings.gap());
		object.addProperty("rgb", settings.rgb());
		object.addProperty("opacity", settings.opacity());
		return object;
	}

	private static JsonPrimitive primitive(JsonObject object, String field) {
		JsonElement value = object.get(field);
		if (value == null || !value.isJsonPrimitive()) throw new IllegalArgumentException("Missing or invalid field: " + field);
		return value.getAsJsonPrimitive();
	}

	private static int integer(JsonObject object, String field) {
		JsonPrimitive value = primitive(object, field);
		if (!value.isNumber()) throw new IllegalArgumentException(field + " must be a number");
		try {
			return value.getAsBigDecimal().intValueExact();
		} catch (ArithmeticException | NumberFormatException invalid) {
			throw new IllegalArgumentException(field + " must be an exact 32-bit integer", invalid);
		}
	}

	private static boolean bool(JsonObject object, String field) {
		JsonPrimitive value = primitive(object, field);
		if (!value.isBoolean()) throw new IllegalArgumentException(field + " must be a boolean");
		return value.getAsBoolean();
	}

	private static String string(JsonObject object, String field) {
		JsonPrimitive value = primitive(object, field);
		if (!value.isString()) throw new IllegalArgumentException(field + " must be a string");
		return value.getAsString();
	}
}
