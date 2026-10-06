package site.zvolcan.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import site.zvolcan.client.config.*;
import site.zvolcan.client.crosshair.CrosshairManager;

/** Runs real screen/widget/editor/layout and real persistence/controller sources. */
public final class CrosshairScreenBehaviorTest {
	private static Path directory;
	private record Fixture(CrosshairConfiguration config, CrosshairManager manager,
		CrosshairConfigScreen screen, Path file) {}

	public static void main(String[] args) throws Exception {
		directory = Path.of(args[1]);
		switch (args[0]) {
			case "layout" -> layout();
			case "editor" -> editor();
			case "render" -> render();
			case "interaction" -> interaction();
			case "types" -> types();
			case "scroll" -> scroll();
			case "lifecycle" -> lifecycle();
			case "warning" -> warning();
			case "numeric" -> numeric();
			case "labels", "checkboxLabels", "presetLabels" -> labels(args[0]);
			case "output" -> output();
			case "boundary" -> boundary();
			default -> throw new AssertionError(args[0]);
		}
	}

	private static Fixture fixture(int width, int height) {
		Path path = directory.resolve("settings-" + System.nanoTime() + ".json");
		CrosshairManager manager = new CrosshairManager();
		CrosshairConfiguration config = new CrosshairConfiguration(manager, new CrosshairSettingsStore(path));
		CrosshairConfigScreen screen = new CrosshairConfigScreen(null, config);
		screen.init(width, height);
		return new Fixture(config, manager, screen, path);
	}

	private static AbstractWidget child(Screen screen, int index) {
		return (AbstractWidget) screen.children().get(index);
	}

	private static KeyEvent key(int input) { return new KeyEvent(input, input, 0); }
	private static void press(Screen screen, int input) { screen.keyPressed(key(input)); }
	private static void click(Screen screen, AbstractWidget widget) {
		require(screen.mouseClicked(new MouseButtonEvent(widget.getRight() - 10,
			widget.getY() + 13, new MouseButtonInfo(1, 0)), false), "mouse accepted");
	}
	private static void text(Screen screen, String text) {
		text.codePoints().forEach(c -> screen.charTyped(new CharacterEvent(c)));
	}
	private static void replace(Screen screen, String text) {
		screen.keyPressed(new KeyEvent(InputConstants.KEY_A, 97, InputConstants.MOD_CONTROL));
		text(screen, text);
	}
	private static GuiGraphicsExtractor draw(Screen screen) {
		GuiGraphicsExtractor graphics = new GuiGraphicsExtractor();
		screen.extractRenderState(graphics, -1, -1, 0);
		return graphics;
	}
	private static void require(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}

	private static void layout() {
		CrosshairScreenLayout wide = new CrosshairScreenLayout(1000, 600, false);
		require(wide.panelWidth() == 600, "60% wide panel");
		require(new CrosshairScreenLayout(500, 400, false).panelWidth() == 360, "360 minimum when space permits");
		for (int[] viewport : new int[][] {{320, 240}, {240, 160}, {1000, 600}}) {
			CrosshairScreenLayout l = new CrosshairScreenLayout(viewport[0], viewport[1], false);
			require(l.left() + l.panelWidth() <= viewport[0] - 8, "panel within viewport");
			require(l.bottom() <= viewport[1] - 28, "fixed footer protected");
			for (int i = 0; i < 8; i++) {
				int scroll = l.reveal(i, 0);
				require(l.rowY(i, scroll) >= l.top(), "revealed row top " + i);
				require(l.rowY(i, scroll) + 26 <= l.bottom(), "revealed row bottom " + i);
			}
			require(l.rowY(1, 0) - l.rowY(0, 0) == 26, "uniform first panel");
			require(l.rowY(4, 0) - l.rowY(3, 0) == 26, "uniform second panel");
			require(l.rowY(7, 0) - l.rowY(6, 0) == 26, "uniform third panel");
			require(l.clampScroll(Integer.MAX_VALUE) == l.maxScroll(), "bounded scroll");
		}
	}

	private static void editor() {
		CrosshairValueEditor e = new CrosshairValueEditor(1, 32, false, "4");
		e.selectAll(); e.insert('3'); e.insert('2');
		require(e.value().orElseThrow() == 32, "valid boundary");
		e.insert('9'); require(e.value().isEmpty(), "out of range does not commit");
		e.backspace(); e.move(-1, false); e.delete(); e.insert('1');
		require(e.text().equals("31") && e.value().orElseThrow() == 31, "caret/delete insertion");
		e.selectAll(); e.backspace(); require(e.value().isEmpty(), "empty invalid");
		e.insert('-'); e.insert('x'); require(e.text().isEmpty(), "digits only");
		for (int i = 0; i < 30; i++) e.insert('9');
		require(e.value().isEmpty() && e.text().length() <= 7, "overflow bounded and safe");
		CrosshairValueEditor color = new CrosshairValueEditor(0, 0xFFFFFF, true, "#FFFFFF");
		color.selectAll(); for (char c : "#a012fF".toCharArray()) color.insert(c);
		require(color.value().orElseThrow() == 0xA012FF, "hex case accepted");
		color.backspace(); require(color.value().isEmpty(), "exact six hex digits");
		color.insert('G'); require(color.value().isEmpty(), "nonhex rejected");
		color.move(-1, true); require(color.hasSelection(), "shift selection");
	}

	private static void render() {
		Fixture f = fixture(1000, 600);
		GuiGraphicsExtractor g = draw(f.screen);
		require(g.fills.getFirst().color() == 0xFF001C05, "opaque fullscreen palette");
		require(g.fills.getFirst().x2() == 1000 && g.fills.getFirst().y2() == 600, "full viewport");
		require(g.fills.stream().filter(r -> r.color() == 0xFF001006).count() == 5, "two tabs three panels");
		require(g.texts.stream().filter(t -> t.y() < 38).count() == 2, "two visible tab names");
		require(g.texts.stream().noneMatch(GuiGraphicsExtractor.Text::shadow), "no shadows");
		require(g.fills.stream().anyMatch(r -> r.color() == 0xFFBDEFF2 && r.y2() - r.y1() == 1), "cyan thin underline");
		List<GuiGraphicsExtractor.Fill> on = g.fills.stream().filter(r -> r.color() == 0xFFFF8F8F).toList();
		require(on.size() == 2 && on.getFirst().x2() - on.getFirst().x1() == 16, "square red booleans");
		for (var square : on) {
			require(g.fills.stream().anyMatch(r -> r.color() == 0xFF5C2525
				&& r.x1() == square.x1() + 3 && r.x2() == square.x2() - 3), "3px red border");
		}
		for (String value : List.of("Cross", "4", "1", "0", "#FFFFFF", "100")) {
			var t = g.texts.stream().filter(r -> r.value().equals(value)).findFirst().orElseThrow();
			require(t.x() + value.length() * 6 == on.getFirst().x2(), "shared right column " + value);
		}
		click(f.screen, child(f.screen, 2));
		g = draw(f.screen);
		require(g.fills.stream().anyMatch(r -> r.color() == 0xFFFFFFFF && r.x2() - r.x1() == 16), "OFF white border");
		require(g.fills.stream().anyMatch(r -> r.color() == 0xFF000000 && r.x2() - r.x1() == 10), "OFF black interior");
		f.screen.setFocused(child(f.screen, 1));
		g = draw(f.screen);
		require(g.fills.stream().filter(r -> r.color() == 0xFFBDEFF2 && r.y1() == 29).count() == 1,
			"selected tab indicator distinct from unselected tab keyboard focus");
	}

	private static void interaction() throws Exception {
		Fixture f = fixture(800, 500);
		var before = f.manager.selected();
		click(f.screen, child(f.screen, 5)); replace(f.screen, "12");
		require(f.config.settings().size() == 4, "uncommitted not applied");
		press(f.screen, InputConstants.KEY_RETURN);
		require(f.config.settings().size() == 12 && f.config.saved(), "commit saved");
		require(f.manager.selected() != before, "live manager replacement");
		require(new CrosshairSettingsStore(f.file).load().settings().size() == 12, "actual save reload");
		press(f.screen, InputConstants.KEY_RETURN); replace(f.screen, "999");
		press(f.screen, InputConstants.KEY_RETURN);
		require(f.config.settings().size() == 12, "invalid cannot change");
		require(draw(f.screen).texts.stream().anyMatch(t -> t.value().equals("999")), "invalid remains editable");
		press(f.screen, InputConstants.KEY_ESCAPE);
		require(draw(f.screen).texts.stream().noneMatch(t -> t.value().equals("999")), "Escape cancels first");
		press(f.screen, InputConstants.KEY_LEFT);
		require(f.config.settings().size() == 11, "nonediting left adjust");
		press(f.screen, InputConstants.KEY_RIGHT);
		require(f.config.settings().size() == 12, "right adjust");
		click(f.screen, child(f.screen, 8)); replace(f.screen, "#123aBc");
		press(f.screen, InputConstants.KEY_RETURN);
		require(f.config.settings().rgb() == 0x123ABC, "color edit");
		f.screen.setFocused(child(f.screen, 9)); press(f.screen, InputConstants.KEY_RIGHT);
		require(f.config.settings().opacity() == 100, "step bounded at max");
		f.screen.setFocused(child(f.screen, 5)); press(f.screen, InputConstants.KEY_RETURN);
		replace(f.screen, "24"); press(f.screen, InputConstants.KEY_LEFT);
		press(f.screen, InputConstants.KEY_DELETE); text(f.screen, "1");
		press(f.screen, InputConstants.KEY_RETURN);
		require(f.config.settings().size() == 21, "keyboard caret/delete in widget");
	}

	private static void types() {
		Fixture f = fixture(800, 500);
		f.screen.setFocused(child(f.screen, 3)); press(f.screen, InputConstants.KEY_RIGHT);
		require(f.config.settings().type() == CrosshairSettings.Type.DOT, "type step");
		require(!child(f.screen, 6).active && !child(f.screen, 7).active, "DOT relevance");
		var saved = f.config.settings();
		child(f.screen, 6).keyPressed(key(InputConstants.KEY_RIGHT));
		require(saved.equals(f.config.settings()), "inactive cannot edit");
		press(f.screen, InputConstants.KEY_TAB); require(f.screen.getFocused() == child(f.screen, 4), "next active");
		press(f.screen, InputConstants.KEY_TAB); press(f.screen, InputConstants.KEY_TAB);
		require(f.screen.getFocused() == child(f.screen, 8), "skip irrelevant rows");
		click(f.screen, child(f.screen, 1)); require(f.screen.children().size() == 10, "five presets plus tabs and buttons");
		click(f.screen, child(f.screen, 5));
		require(f.config.settings().type() == CrosshairSettings.Type.CIRCLE, "preset live selection");
		require(draw(f.screen).texts.stream().filter(t -> t.value().equals("Selected")).count() == 1, "plain selected status");
		click(f.screen, child(f.screen, 0));
		require(child(f.screen, 6).active && !child(f.screen, 7).active, "CIRCLE relevance");
		f.screen.setFocused(child(f.screen, 2)); press(f.screen, InputConstants.KEY_SPACE);
		require(!f.config.settings().enabled(), "Space boolean");
		press(f.screen, InputConstants.KEY_RETURN); require(f.config.settings().enabled(), "Enter boolean");
		f.screen.setFocused(child(f.screen, 3)); press(f.screen, InputConstants.KEY_RIGHT);
		require(f.config.settings().type() == CrosshairSettings.Type.HEART, "heart step");
		require(child(f.screen, 6).active && !child(f.screen, 7).active, "HEART relevance");
		press(f.screen, InputConstants.KEY_RIGHT);
		require(f.config.settings().type() == CrosshairSettings.Type.CROSS, "type wrap");
		require(child(f.screen, 6).active && child(f.screen, 7).active, "CROSS restores relevance");
	}

	private static void scroll() {
		for (int[] size : new int[][] {{320, 240}, {240, 160}}) {
			Fixture f = fixture(size[0], size[1]);
			for (int i = 0; i < 10; i++) {
				press(f.screen, InputConstants.KEY_TAB);
				AbstractWidget row = (AbstractWidget) f.screen.getFocused();
				if (row.getHeight() == 26) require(row.getY() >= 38 && row.getBottom() <= size[1] - 28, "focus reveals row");
			}
			require(f.screen.mouseScrolled(20, 80, 0, -100), "content scroll handled");
			AbstractWidget opacity = child(f.screen, 9);
			require(opacity.getBottom() <= size[1] - 28, "scroll reaches final row");
			click(f.screen, opacity); replace(f.screen, "55"); press(f.screen, InputConstants.KEY_RETURN);
			require(f.config.settings().opacity() == 55, "last row mouse reachable");
			GuiGraphicsExtractor g = draw(f.screen);
			require(g.texts.stream().filter(t -> t.value().equals("Opacity")).allMatch(t -> t.clip() != null), "content scissor");
			require(g.texts.stream().filter(t -> t.value().equals("Saved")).allMatch(t -> t.clip() == null), "footer fixed unclipped");
			require(child(f.screen, 0).getY() == 8, "tab fixed");
			require(!f.screen.mouseScrolled(size[0] - 1, 80, 0, 1), "right empty zone does not scroll");
			require(!child(f.screen, 2).isMouseOver(20, 10), "offscreen row cannot eat tab clicks");
			f.screen.mouseScrolled(20, 80, 0, 100);
			require(opacity.getBottom() > size[1] - 28, "manual scroll can hide focused row");
			press(f.screen, InputConstants.KEY_LEFT);
			require(opacity.getY() >= 38 && opacity.getBottom() <= size[1] - 28, "keyboard adjustment re-reveals focused row");
			require(f.config.settings().opacity() == 54, "revealed keyboard adjustment");
			click(f.screen, child(f.screen, 1));
			f.screen.setFocused(child(f.screen, 5));
			require(child(f.screen, 5).getBottom() <= size[1] - 28, "preset reveal");
		}
	}

	private static void lifecycle() {
		Fixture f = fixture(800, 500);
		click(f.screen, child(f.screen, 5)); replace(f.screen, "9");
		click(f.screen, child(f.screen, 8)); require(f.config.settings().size() == 9, "focus loss valid commit");
		replace(f.screen, "#123"); click(f.screen, child(f.screen, 1));
		require(f.config.settings().rgb() == 0xFFFFFF, "tab loss invalid revert");
		click(f.screen, child(f.screen, 0)); click(f.screen, child(f.screen, 5)); replace(f.screen, "16");
		f.screen.resize(240, 160); require(f.config.settings().size() == 16, "resize valid commit");
		f.screen.setFocused(child(f.screen, 5)); press(f.screen, InputConstants.KEY_RETURN); replace(f.screen, "999");
		f.screen.resize(320, 240); require(f.config.settings().size() == 16, "resize invalid revert");
		f.screen.setFocused(child(f.screen, 5)); press(f.screen, InputConstants.KEY_RETURN); replace(f.screen, "7");
		f.screen.onClose(); require(f.config.settings().size() == 7, "close valid commit");
		require(Minecraft.getInstance().shown == null, "nullable previous");
		Screen previous = new Screen(Component.literal("Previous")) {};
		CrosshairConfigScreen screen = new CrosshairConfigScreen(previous, f.config);
		screen.init(800, 500); click(screen, child(screen, 5)); replace(screen, "5");
		press(screen, InputConstants.KEY_ESCAPE);
		require(f.config.settings().size() == 7, "Escape edit cancels without applying");
		press(screen, InputConstants.KEY_ESCAPE);
		require(Minecraft.getInstance().shown == previous, "second Escape returns previous");
		click(screen, child(screen, 5)); replace(screen, "6"); screen.removed();
		require(f.config.settings().size() == 6, "external removal commits valid");
	}

	private static void warning() throws Exception {
		Path path = directory.resolve("invalid-" + System.nanoTime() + ".json");
		Files.writeString(path, "not json");
		CrosshairConfiguration config = new CrosshairConfiguration(new CrosshairManager(), new CrosshairSettingsStore(path));
		CrosshairConfigScreen screen = new CrosshairConfigScreen(null, config); screen.init(240, 160);
		screen.setFocused(child(screen, 2)); press(screen, InputConstants.KEY_SPACE);
		require(!config.saved() && !config.writable(), "locked live update unsaved");
		GuiGraphicsExtractor g = new GuiGraphicsExtractor(); screen.extractRenderState(g, 20, 145, 0);
		require(g.texts.stream().anyMatch(t -> t.value().equals("Unsaved")), "real unsaved footer");
		require(g.texts.stream().noneMatch(t -> t.value().contains(path.toString())), "path not dumped into footer");
		require(g.tooltip != null && g.tooltip.getString().contains(config.warning()), "full warning tooltip");
		List<String> narration = new ArrayList<>();
		screen.updateNarrationState((type, component) -> narration.add(component.getString()));
		require(narration.stream().anyMatch(t -> t.contains(config.warning())), "full warning narration");
		child(screen, 0).updateNarration((type, component) -> narration.add(component.getString()));
		require(narration.stream().anyMatch(t -> t.contains("Settings")), "labelled tab accessible name");
		require(Files.readString(path).equals("not json"), "invalid original preserved");

		Path parent = directory.resolve("blocked-" + System.nanoTime());
		CrosshairConfiguration failed = new CrosshairConfiguration(new CrosshairManager(),
			new CrosshairSettingsStore(parent.resolve("settings.json")));
		Files.writeString(parent, "block writes after load");
		CrosshairConfigScreen failedScreen = new CrosshairConfigScreen(null, failed);
		failedScreen.init(240, 160); press(failedScreen, InputConstants.KEY_SPACE);
		require(!failed.saved() && failed.writable() && !failed.settings().enabled(), "save failure keeps live edit retryable");
		require(draw(failedScreen).texts.stream().anyMatch(t -> t.value().equals("Save failed")), "failed save not claimed saved");
		// A successful save may still report a temporary-file cleanup warning. Inject only
		// this rare controller boundary state, not any UI logic or a replacement controller.
		var savedField = CrosshairConfiguration.class.getDeclaredField("saved");
		savedField.setAccessible(true); savedField.setBoolean(failed, true);
		var warningField = CrosshairConfiguration.class.getDeclaredField("warning");
		warningField.setAccessible(true); warningField.set(failed, "Temporary config cleanup failed");
		g = draw(failedScreen);
		require(g.texts.stream().anyMatch(t -> t.value().equals("Saved")), "cleanup warning does not negate successful save");
		require(g.texts.stream().noneMatch(t -> t.value().equals("Save failed")), "cleanup warning not misreported as failed save");
	}

	private static void labels(String section) {
		for (int[] viewport : new int[][] {{240, 160}, {320, 240}, {1000, 600}}) {
			Fixture f = fixture(viewport[0], viewport[1]);
			GuiGraphicsExtractor g = draw(f.screen);
			if (section.equals("labels")) for (int i = 0; i < 2; i++) {
				AbstractWidget tab = child(f.screen, i);
				String name = i == 0 ? "Sett" : "Pres";
				var label = g.texts.stream().filter(t -> t.value().equals(name) && t.y() < 38).findFirst().orElseThrow();
				require(label.x() == tab.getX() + (tab.getWidth() - name.length() * 6) / 2, "centered tab label");
				require(label.x() >= tab.getX() + 4 && label.x() + name.length() * 6 <= tab.getRight() - 4, "tab text bounded");
				require(label.color() == CrosshairOptionWidget.Palette.TEXT && !label.shadow(), "warm white tab without shadow");
			}
			if (section.equals("checkboxLabels")) for (int index : new int[] {2, 4}) {
				AbstractWidget row = child(f.screen, index);
				f.screen.setFocused(row);
				for (String state : List.of("On", "Off")) {
					g = draw(f.screen);
					int y = row.getY() + (row.getHeight() - 9) / 2;
					var status = g.texts.stream().filter(t -> t.value().equals(state) && t.y() == y).findFirst().orElseThrow();
					var label = g.texts.stream().filter(t -> t.y() == y && t.x() == row.getX() + 12).findFirst().orElseThrow();
					require(status.x() + state.length() * 6 == row.getRight() - 12 - 16 - 8, "8px status/square gap");
					require(label.x() + label.value().length() * 6 + 8 <= status.x(), "checkbox label never overlaps status");
					require(status.color() == CrosshairOptionWidget.Palette.TEXT && !status.shadow(), "plain warm white status");
					List<String> narration = new ArrayList<>();
					row.updateNarration((type, component) -> narration.add(component.getString()));
					require(narration.contains(row.getMessage().getString() + ": " + state), "visible/narrated checkbox state");
					press(f.screen, InputConstants.KEY_SPACE);
				}
			}
			click(f.screen, child(f.screen, 1));
			if (section.equals("presetLabels")) for (int index = 2; index < 6; index++) {
				AbstractWidget row = child(f.screen, index);
				f.screen.setFocused(row); press(f.screen, InputConstants.KEY_RETURN);
				g = draw(f.screen);
				require(g.texts.stream().filter(t -> t.value().equals("Selected")).count() == 1, "one selected preset");
				// Scroll to each remaining row before checking its action text and narration.
				for (int other = 2; other < 6; other++) {
					AbstractWidget option = child(f.screen, other);
					f.screen.setFocused(option);
					String action = other == index ? "Selected" : "Select";
					int y = option.getY() + 8;
					require(draw(f.screen).texts.stream().anyMatch(t -> t.value().equals(action) && t.y() == y), "visible preset action");
					List<String> narration = new ArrayList<>();
					option.updateNarration((type, component) -> narration.add(component.getString()));
					require(narration.contains(option.getMessage().getString() + ": " + action), "visible/narrated preset action");
				}
			}
		}
	}

	/** Independent pixel oracle: builds masks by stamping arms/brushes, not production row spans. */
	private static Set<String> expectedPixels(CrosshairSettings s) {
		Set<String> pixels = new HashSet<>();
		if (s.opacity() == 0) return pixels;
		int low = -s.thickness() / 2, high = low + s.thickness() - 1;
		int extent = s.size() + s.gap();
		if (s.type() == CrosshairSettings.Type.CROSS || s.type() == CrosshairSettings.Type.X) {
			for (int step = -extent; step <= extent; step++) {
				for (int a = low; a <= high; a++) {
					if (s.type() == CrosshairSettings.Type.CROSS) {
						pixels.add((step + low) + "," + a);
						pixels.add((step + high) + "," + a);
						pixels.add(a + "," + (step + low));
						pixels.add(a + "," + (step + high));
					} else for (int b = low; b <= high; b++) {
						pixels.add((step + a) + "," + (step + b));
						pixels.add((step + a) + "," + (-step + b));
					}
				}
			}
			if (s.gap() > 0) for (int x = low - s.gap(); x <= high + s.gap(); x++)
				for (int y = low - s.gap(); y <= high + s.gap(); y++) pixels.remove(x + "," + y);
		} else if (s.type() == CrosshairSettings.Type.DOT) {
			int start = -s.size() / 2;
			for (int x = start; x < start + s.size(); x++)
				for (int y = start; y < start + s.size(); y++) pixels.add(x + "," + y);
		} else if (s.type() == CrosshairSettings.Type.HEART) {
			int inner = Math.max(0, s.size() - s.thickness());
			for (int x = -s.size(); x <= s.size(); x++) for (int y = -s.size(); y <= s.size(); y++) {
				if (heart(x, y, s.size()) && (inner == 0 || !heart(x, y, inner))) pixels.add(x + "," + y);
			}
		} else {
			int inner = Math.max(0, s.size() - s.thickness());
			for (int x = -s.size(); x <= s.size(); x++) for (int y = -s.size(); y <= s.size(); y++) {
				int d = x * x + y * y;
				if (d <= s.size() * s.size() && (inner == 0 || d > inner * inner)) pixels.add(x + "," + y);
			}
		}
		return pixels;
	}

	private static boolean heart(int x, int y, int r) {
		int left = (2 * x + r) * (2 * x + r) + (2 * y + r) * (2 * y + r);
		int right = (2 * x - r) * (2 * x - r) + (2 * y + r) * (2 * y + r);
		return left <= r * r || right <= r * r
			|| 2 * y >= -r && 2 * y <= 2 * r && 3 * Math.abs(x) <= 2 * (r - y);
	}

	private static void assertOutput(Fixture f, CrosshairSettings expected) {
		GuiGraphicsExtractor g = new GuiGraphicsExtractor();
		f.manager.selected().draw(g, 100, 80);
		Set<String> pixels = new HashSet<>();
		int color = expected.argb();
		if (expected.inverted()) {
			color = 0xFF000000;
			for (int shift : new int[] {16, 8, 0})
				color |= (((expected.rgb() >>> shift & 255) * expected.opacity() + 50) / 100) << shift;
		}
		for (var fill : g.fills) {
			require(fill.pipeline() == (expected.inverted() ? RenderPipelines.GUI_INVERT : RenderPipelines.GUI), "selected object emits expected pipeline");
			require(fill.color() == color, "selected object emits expected rendering color");
			for (int x = fill.x1(); x < fill.x2(); x++) for (int y = fill.y1(); y < fill.y2(); y++)
				require(pixels.add((x - 100) + "," + (y - 80)), "fills never overlap");
		}
		require(pixels.equals(expectedPixels(expected)), "actual manager emitted geometry: " + expected);
		require(f.config.settings().equals(expected), "controls share one settings owner");
		require(f.config.saved(), "actual save succeeded");
		CrosshairManager reloaded = new CrosshairManager();
		CrosshairConfiguration fresh = new CrosshairConfiguration(reloaded, new CrosshairSettingsStore(f.file));
		require(fresh.settings().equals(expected), "fresh controller/store reloads JSON");
		GuiGraphicsExtractor reloadDraw = new GuiGraphicsExtractor();
		reloaded.selected().draw(reloadDraw, 100, 80);
		require(g.fills.equals(reloadDraw.fills), "reloaded manager emits identical fills");
	}

	private static void edit(Fixture f, int index, String value) {
		f.screen.setFocused(child(f.screen, index));
		press(f.screen, InputConstants.KEY_RETURN); replace(f.screen, value);
		press(f.screen, InputConstants.KEY_RETURN);
	}

	private static void output() {
		for (int[] viewport : new int[][] {{240, 160}, {320, 240}, {1000, 600}}) {
			Fixture f = fixture(viewport[0], viewport[1]);
			CrosshairSettings expected = CrosshairSettings.defaults();
			click(f.screen, child(f.screen, 1));
			for (CrosshairSettings.Type type : CrosshairSettings.Type.values()) {
				AbstractWidget preset = child(f.screen, 2 + type.ordinal());
				f.screen.setFocused(preset);
				if (type.ordinal() % 2 == 0) click(f.screen, preset);
				else press(f.screen, InputConstants.KEY_SPACE);
				expected = expected.withType(type); assertOutput(f, expected);
				click(f.screen, child(f.screen, 0));
				int size = 6 + type.ordinal();
				edit(f, 5, Integer.toString(size)); expected = expected.withSize(size); assertOutput(f, expected);
				if (type.supportsThickness()) {
					edit(f, 6, "2"); expected = expected.withThickness(2); assertOutput(f, expected);
				}
				if (type.supportsGap()) {
					edit(f, 7, "2"); expected = expected.withGap(2); assertOutput(f, expected);
				}
				click(f.screen, child(f.screen, 1));
			}
			click(f.screen, child(f.screen, 0));
			edit(f, 8, "#123ABC"); expected = expected.withRgb(0x123ABC); assertOutput(f, expected);
			edit(f, 9, "50"); expected = expected.withOpacity(50); assertOutput(f, expected);
			for (int index : new int[] {8, 9}) {
				edit(f, index, index == 8 ? "#123" : "101");
				assertOutput(f, expected); press(f.screen, InputConstants.KEY_ESCAPE); assertOutput(f, expected);
			}
			f.screen.setFocused(child(f.screen, 4)); press(f.screen, InputConstants.KEY_SPACE);
			expected = expected.withInverted(false); assertOutput(f, expected);
			f.screen.setFocused(child(f.screen, 2)); click(f.screen, child(f.screen, 2));
			expected = expected.withEnabled(false); assertOutput(f, expected);
			press(f.screen, InputConstants.KEY_RETURN); expected = expected.withEnabled(true); assertOutput(f, expected);
			edit(f, 9, "0"); expected = expected.withOpacity(0); assertOutput(f, expected);
		}
	}

	private static void boundary() {
		Fixture f = fixture(240, 160);
		AbstractWidget inverted = child(f.screen, 4);
		// Scroll 65px so Inverted straddles the upper clip boundary.
		f.screen.mouseScrolled(20, 80, 0, -2.5);
		require(inverted.getY() < 38 && inverted.getBottom() > 38, "partially visible upper row");
		require(f.screen.mouseClicked(new MouseButtonEvent(20, 39, new MouseButtonInfo(1, 0)), false), "upper clipped row click accepted after reveal");
		require(!f.config.settings().inverted(), "upper boundary click toggles once");
		require(inverted.getY() == 38, "focus reveals upper row");
		f.screen.mouseScrolled(20, 80, 0, 100);
		f.screen.mouseScrolled(20, 80, 0, -0.5);
		AbstractWidget size = child(f.screen, 5);
		require(size.getY() < 132 && size.getBottom() > 132, "partially visible lower row");
		require(f.screen.mouseClicked(new MouseButtonEvent(20, 131, new MouseButtonInfo(1, 0)), false), "lower clipped row click accepted after reveal");
		replace(f.screen, "7"); press(f.screen, InputConstants.KEY_RETURN);
		require(f.config.settings().size() == 7, "lower boundary click starts editor");
		require(!size.isMouseOver(20, 132) && !inverted.isMouseOver(20, 37), "clip boundary remains exclusive");
		f.screen.setFocused(child(f.screen, 3)); press(f.screen, InputConstants.KEY_RIGHT);
		AbstractWidget thickness = child(f.screen, 6);
		require(!thickness.active && !thickness.mouseClicked(new MouseButtonEvent(20, thickness.getY() + 10, new MouseButtonInfo(1, 0)), false), "irrelevant row rejects mouse");
	}

	private static void numeric() {
		Fixture f = fixture(800, 500);
		int[][] fields = {{5, 1, 32}, {6, 1, 8}, {7, 0, 16}, {9, 0, 100}};
		for (int[] spec : fields) {
			AbstractWidget row = child(f.screen, spec[0]);
			click(f.screen, row); replace(f.screen, Integer.toString(spec[2])); press(f.screen, InputConstants.KEY_RETURN);
			CrosshairSettings high = f.config.settings();
			press(f.screen, InputConstants.KEY_RIGHT);
			require(f.config.settings().equals(high), "bounded maximum for " + row.getMessage());
			press(f.screen, InputConstants.KEY_RETURN); replace(f.screen, Integer.toString(spec[1])); press(f.screen, InputConstants.KEY_RETURN);
			CrosshairSettings low = f.config.settings();
			press(f.screen, InputConstants.KEY_LEFT);
			require(f.config.settings().equals(low), "bounded minimum for " + row.getMessage());
		}
		f.screen.setFocused(child(f.screen, 5)); press(f.screen, InputConstants.KEY_RETURN); replace(f.screen, "24");
		f.screen.keyPressed(new KeyEvent(InputConstants.KEY_LEFT, 0, InputConstants.MOD_SHIFT));
		press(f.screen, InputConstants.KEY_BACKSPACE); text(f.screen, "1"); press(f.screen, InputConstants.KEY_RETURN);
		require(f.config.settings().size() == 21, "shift arrow selected deletion");
		press(f.screen, InputConstants.KEY_RETURN); press(f.screen, InputConstants.KEY_HOME);
		press(f.screen, InputConstants.KEY_DELETE); press(f.screen, InputConstants.KEY_RETURN);
		require(f.config.settings().size() == 1, "Home cancels selection before Delete");
		f.screen.keyPressed(new KeyEvent(InputConstants.KEY_TAB, 0, InputConstants.MOD_SHIFT));
		require(f.screen.getFocused() == child(f.screen, 4), "Shift Tab backwards");
		f.screen.setFocused(child(f.screen, 1)); press(f.screen, InputConstants.KEY_RETURN);
		require(f.screen.children().size() == 10, "keyboard tab activation");
		press(f.screen, InputConstants.KEY_LEFT); require(f.screen.children().size() == 13, "keyboard tab switch back");
		f.screen.setFocused(child(f.screen, 3)); press(f.screen, InputConstants.KEY_RIGHT); press(f.screen, InputConstants.KEY_RIGHT);
		require(f.config.settings().type() == CrosshairSettings.Type.X && child(f.screen, 6).active && child(f.screen, 7).active, "X relevance");
	}
}
