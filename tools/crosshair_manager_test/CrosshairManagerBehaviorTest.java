import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.GuiGraphics.Fill;
import net.minecraft.client.renderer.RenderPipelines;
import site.zvolcan.client.CrosshairEditorClient;
import site.zvolcan.client.crosshair.Crosshair;
import site.zvolcan.client.crosshair.Crosshair.Point;
import site.zvolcan.client.crosshair.CrosshairManager;

public final class CrosshairManagerBehaviorTest {
	private static final Point RECTANGLE = new Point(-3, -2, 2, 4);

	private static Crosshair crosshair() {
		return new Crosshair(List.of(RECTANGLE), RenderPipelines.GUI_INVERT, 0x7F123456);
	}

	public static void main(String[] args) {
		switch (args[0]) {
			case "registry" -> registry();
			case "selection" -> selection();
			case "immutable" -> immutable();
			case "validation" -> validation();
			case "draw" -> draw();
			case "empty" -> empty();
			case "default" -> defaultCrosshair();
			case "clientSelection" -> clientSelection();
			default -> throw new AssertionError("Unknown test case");
		}
	}

	private static void registry() {
		CrosshairManager manager = new CrosshairManager();
		Crosshair first = crosshair();
		Crosshair second = crosshair();
		manager.register("Aim", first);
		manager.register("aim", second);
		manager.register(" spaced ", first);
		check(manager.get("Aim") == first, "lookup preserves object identity");
		check(manager.get("aim") == second, "keys are case-sensitive");
		check(manager.get(" spaced ") == first, "keys are not trimmed");
		check(manager.get("missing") == null, "missing lookup returns null");
		check(new ArrayList<>(manager.registry().keySet()).equals(List.of("Aim", "aim", " spaced ")),
			"registry retains insertion order");
	}

	private static void selection() {
		CrosshairManager manager = new CrosshairManager();
		Crosshair first = crosshair();
		Crosshair second = new Crosshair(List.of(RECTANGLE), RenderPipelines.GUI, 0xFFABCDEF);
		check(manager.selected() == null, "initial selection is absent");
		expect(IllegalArgumentException.class, () -> manager.select("missing"));
		check(manager.selected() == null, "failed empty selection stays empty");
		manager.register("first", first);
		manager.register("second", second);
		check(manager.selected() == first, "first registration selects; later ones do not");
		manager.select("second");
		expect(IllegalArgumentException.class, () -> manager.select("missing"));
		check(manager.selected() == second, "missing selection preserves previous selection");
		manager.register("second", first);
		check(manager.get("second") == first && manager.selected() == first,
			"duplicate replaces lookup and selected object");
		check(new ArrayList<>(manager.registry().keySet()).equals(List.of("first", "second")),
			"replacement retains order and size");
		manager.select("first");
		manager.register("second", second);
		check(manager.selected() == first, "replacement of unselected key does not change selection");
		manager.select("second");
		GuiGraphics context = new GuiGraphics();
		manager.draw(context, 0, 0);
		check(context.fills.equals(List.of(new Fill(RenderPipelines.GUI, -3, -2, 2, 4, 0xFFABCDEF))),
			"manager draws selected object, not first registration");
	}

	private static void immutable() {
		List<Point> source = new ArrayList<>(List.of(RECTANGLE));
		Crosshair crosshair = new Crosshair(source, RenderPipelines.GUI_INVERT, 0x7F123456);
		source.clear();
		check(crosshair.points().equals(List.of(RECTANGLE)), "constructor copies points");
		expect(UnsupportedOperationException.class, () -> crosshair.points().clear());
		CrosshairManager manager = new CrosshairManager();
		manager.register("first", crosshair);
		Map<String, Crosshair> view = manager.registry();
		expect(UnsupportedOperationException.class, () -> view.put("bad", crosshair));
		expect(UnsupportedOperationException.class, () -> view.remove("first"));
		expect(UnsupportedOperationException.class, () -> view.entrySet().iterator().next().setValue(crosshair()));
		expect(UnsupportedOperationException.class, () -> view.keySet().remove("first"));
		expect(UnsupportedOperationException.class, () -> view.values().clear());
		manager.register("second", crosshair);
		check(view.size() == 2, "registry view is live, but read-only");
	}

	private static void validation() {
		expect(NullPointerException.class, () -> new Crosshair(null, RenderPipelines.GUI, 0));
		expect(NullPointerException.class, () -> new Crosshair(List.of(), null, 0));
		expect(NullPointerException.class, () -> new Crosshair(Arrays.asList(RECTANGLE, null), RenderPipelines.GUI, 0));
		for (int[] bounds : new int[][] {{0, 0, 0, 1}, {0, 0, 1, 0}, {2, 0, 1, 1}, {0, 2, 1, 1}}) {
			expect(IllegalArgumentException.class, () -> new Point(bounds[0], bounds[1], bounds[2], bounds[3]));
		}
		new Point(Integer.MIN_VALUE, -1, Integer.MAX_VALUE, 1);
		CrosshairManager manager = new CrosshairManager();
		Crosshair first = crosshair();
		manager.register("first", first);
		for (String invalid : Arrays.asList(null, "", " \t\n")) {
			Class<? extends Throwable> error = invalid == null ? NullPointerException.class : IllegalArgumentException.class;
			expect(error, () -> manager.register(invalid, first));
			expect(error, () -> manager.get(invalid));
			expect(error, () -> manager.select(invalid));
		}
		expect(NullPointerException.class, () -> manager.register("first", null));
		check(manager.registry().size() == 1 && manager.selected() == first,
			"invalid registration or selection does not mutate registry");
	}

	private static void draw() {
		Crosshair crosshair = new Crosshair(
			List.of(RECTANGLE, new Point(4, 5, 9, 7)), RenderPipelines.GUI_INVERT, 0x7F123456);
		check(crosshair.pipeline() == RenderPipelines.GUI_INVERT, "pipeline accessor");
		check(crosshair.color() == 0x7F123456, "ARGB accessor preserves alpha");
		GuiGraphics context = new GuiGraphics();
		crosshair.draw(context, 12, -7);
		check(context.fills.equals(List.of(
			new Fill(RenderPipelines.GUI_INVERT, 9, -9, 14, -3, 0x7F123456),
			new Fill(RenderPipelines.GUI_INVERT, 16, -2, 21, 0, 0x7F123456))),
			"draw preserves rectangles, order, relative offsets, pipeline and color");
	}

	private static void empty() {
		GuiGraphics context = new GuiGraphics();
		new CrosshairManager().draw(context, 12, -7);
		new Crosshair(List.of(), RenderPipelines.GUI, 0).draw(context, 12, -7);
		check(context.fills.isEmpty(), "empty manager and model issue no fills");
	}

	private static void defaultCrosshair() {
		CrosshairManager manager = CrosshairEditorClient.getCrosshairManager();
		check(manager == CrosshairEditorClient.getCrosshairManager(), "getter returns single manager");
		check(manager.registry().isEmpty(), "manager is empty before client initialization");
		new CrosshairEditorClient().onInitializeClient();
		check(manager.registry().size() == 2, "initializer retains default and adds configured preset");
		check(manager.get("configured") == manager.selected(), "persisted configuration is selected");
		check(CrosshairEditorClient.getConfiguration() != null, "single configuration initialized");
		check(manager.get("default").points().equals(List.of(
			new Point(-4, 0, 5, 1), new Point(0, -4, 1, 0), new Point(0, 1, 1, 5))),
			"default preserves original rectangles and order");

		manager.select("default");
		GuiGraphics context = new GuiGraphics();
		manager.draw(context, 12, -7);
		check(context.fills.size() == 3, "named original default emits three fills");
		Set<List<Integer>> actual = new HashSet<>();
		for (Fill fill : context.fills) {
			check(fill.pipeline() == RenderPipelines.GUI_INVERT, "default uses GUI_INVERT");
			check(fill.color() == 0xFFFFFFFF, "default uses opaque white ARGB");
			check(fill.x1() < fill.x2() && fill.y1() < fill.y2(), "fills have positive extent");
			for (int x = fill.x1(); x < fill.x2(); x++) {
				for (int y = fill.y1(); y < fill.y2(); y++) {
					check(actual.add(List.of(x - 12, y + 7)), "no pixel is inverted twice");
				}
			}
		}
		Set<List<Integer>> expected = new HashSet<>();
		for (int offset = -4; offset <= 4; offset++) {
			expected.add(List.of(offset, 0));
			expected.add(List.of(0, offset));
		}
		check(actual.equals(expected) && actual.size() == 17,
			"default covers exactly the original 17 pixels including center once");
	}

	private static void clientSelection() {
		CrosshairManager manager = CrosshairEditorClient.getCrosshairManager();
		Crosshair alternate = new Crosshair(
			List.of(new Point(-2, -1, 3, 2)), RenderPipelines.GUI, 0x8044CC22);
		manager.register("alternate", alternate);
		new CrosshairEditorClient().onInitializeClient();
		check(manager.selected() == manager.get("configured"), "initializer selects loaded configured preset");
		manager.select("alternate");
		GuiGraphics context = new GuiGraphics();
		CrosshairEditorClient.getCrosshairManager().draw(context, 12, -7);
		check(context.fills.equals(List.of(new Fill(RenderPipelines.GUI, 10, -8, 15, -5, 0x8044CC22))),
			"shared manager routes selected alternate geometry, pipeline and ARGB");
		manager.select("default");
		check(manager.selected() != alternate, "default remains available after switching");
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}

	private static void expect(Class<? extends Throwable> type, Runnable action) {
		try {
			action.run();
		} catch (Throwable error) {
			if (type.isInstance(error)) return;
			throw new AssertionError("Expected " + type.getSimpleName() + ", got " + error, error);
		}
		throw new AssertionError("Expected " + type.getSimpleName());
	}
}
