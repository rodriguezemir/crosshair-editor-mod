package site.zvolcan.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import site.zvolcan.client.config.CrosshairConfiguration;
import site.zvolcan.client.config.CrosshairSettings;
import site.zvolcan.client.config.CrosshairSettings.Type;

/** Native focus/narration boundary with intentionally plain pixel-art extraction. */
public final class CrosshairOptionWidget extends AbstractWidget {
	/** Centralized English UI vocabulary, ready for the later translation integration. */
	public enum Field {
		ENABLED("Enabled", 0, 1), TYPE("Type", 0, Type.values().length - 1), INVERTED("Inverted", 0, 1),
		SIZE("Size", CrosshairSettings.MIN_SIZE, CrosshairSettings.MAX_SIZE),
		THICKNESS("Thickness", CrosshairSettings.MIN_THICKNESS, CrosshairSettings.MAX_THICKNESS),
		GAP("Gap", CrosshairSettings.MIN_GAP, CrosshairSettings.MAX_GAP),
		COLOR("Color", 0, 0xFFFFFF),
		OPACITY("Opacity", CrosshairSettings.MIN_OPACITY, CrosshairSettings.MAX_OPACITY);

		final String label;
		final int min;
		final int max;
		Field(String label, int min, int max) {
			this.label = label;
			this.min = min;
			this.max = max;
		}
		boolean checkbox() { return this == ENABLED || this == INVERTED; }
		boolean editable() { return ordinal() >= SIZE.ordinal(); }
	}

	public static final class Palette {
		public static final int BACKGROUND = 0xFF001C05;
		public static final int PANEL = 0xFF001006;
		public static final int TEXT = 0xFFF4F4F4;
		public static final int MUTED = 0xFFB8B8B8;
		public static final int INACTIVE = 0xFF777777;
		public static final int FOCUS = 0xFFBDEFF2;
		public static final int ON_BORDER = 0xFFFF8F8F;
		public static final int ON_INSIDE = 0xFF5C2525;
		public static final int OFF_BORDER = 0xFFFFFFFF;
		public static final int OFF_INSIDE = 0xFF000000;
		private Palette() {}
	}

	static final String SELECTED = "Selected";
	private static final String SELECT = "Select";
	private static final String TOGGLE_USAGE = "Space or Enter to toggle";
	private static final String TYPE_USAGE = "Left or Right to choose type; Enter for next";
	private static final String EDIT_USAGE = "Enter to edit; Left or Right to adjust";
	private static final String COLOR_USAGE = "Enter to edit #RRGGBB";
	private static final String EDITING_USAGE = "Enter to apply; Escape to cancel; Ctrl+A to select all";
	private static final String SELECT_USAGE = "Space or Enter to select";
	private static final String UNAVAILABLE = "Unavailable for this type";

	private final Font font;
	private final CrosshairConfiguration configuration;
	private final Field field;
	private final Type preset;
	private final boolean tab;
	private final Runnable activation;
	private final BooleanSupplier selected;
	private final Runnable changed;
	private final Consumer<CrosshairOptionWidget> reveal;
	private CrosshairValueEditor editor;
	private int clipTop = Integer.MIN_VALUE;
	private int clipBottom = Integer.MAX_VALUE;

	private CrosshairOptionWidget(int x, int y, int width, int height, Font font,
		CrosshairConfiguration configuration, Field field, Type preset, boolean tab,
		String label, Runnable activation, BooleanSupplier selected, Runnable changed,
		Consumer<CrosshairOptionWidget> reveal) {
		super(x, y, width, height, Component.literal(label));
		this.font = font;
		this.configuration = configuration;
		this.field = field;
		this.preset = preset;
		this.tab = tab;
		this.activation = activation;
		this.selected = selected;
		this.changed = changed;
		this.reveal = reveal;
	}

	static CrosshairOptionWidget option(int x, int y, int width, Font font,
		CrosshairConfiguration configuration, Field field, Runnable changed,
		Consumer<CrosshairOptionWidget> reveal) {
		return new CrosshairOptionWidget(x, y, width, CrosshairScreenLayout.ROW_HEIGHT, font,
			configuration, field, null, false, field.label, null, () -> false, changed, reveal);
	}

	static CrosshairOptionWidget tab(int x, Font font, String name, Runnable activation,
		BooleanSupplier selected) {
		return new CrosshairOptionWidget(x, CrosshairScreenLayout.TAB_Y,
			CrosshairScreenLayout.TAB_WIDTH, CrosshairScreenLayout.TAB_HEIGHT, font,
			null, null, null, true, name, activation, selected, () -> {}, widget -> {});
	}

	static CrosshairOptionWidget button(int x, int y, int width, Font font, String name,
		Runnable activation) {
		return new CrosshairOptionWidget(x, y, width, CrosshairScreenLayout.TAB_HEIGHT, font,
			null, null, null, true, name, activation, () -> false, () -> {}, widget -> {});
	}

	static CrosshairOptionWidget preset(int x, int y, int width, Font font,
		CrosshairConfiguration configuration, Type preset, Runnable changed,
		Consumer<CrosshairOptionWidget> reveal) {
		return new CrosshairOptionWidget(x, y, width, CrosshairScreenLayout.ROW_HEIGHT, font,
			configuration, null, preset, false, typeName(preset), null,
			() -> configuration.settings().type() == preset, changed, reveal);
	}

	static String typeName(Type type) {
		return switch (type) {
			case CROSS -> "Cross";
			case DOT -> "Dot";
			case X -> "X";
			case CIRCLE -> "Circle";
			case HEART -> "Heart";
		};
	}

	void updateRelevance() {
		active = field != Field.THICKNESS && field != Field.GAP
			|| field == Field.THICKNESS && configuration.settings().type().supportsThickness()
			|| field == Field.GAP && configuration.settings().type().supportsGap();
	}

	void setClip(int top, int bottom) {
		clipTop = top;
		clipBottom = bottom;
	}

	@Override
	public boolean isMouseOver(double x, double y) {
		return y >= clipTop && y < clipBottom && super.isMouseOver(x, y);
	}

	@Override
	public void setFocused(boolean focused) {
		if (!focused) finishEdit();
		super.setFocused(focused);
		if (focused) reveal.accept(this);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		if (active) activate();
	}

	private void activate() {
		if (tab) {
			activation.run();
		} else if (preset != null) {
			update(configuration.settings().withType(preset));
		} else if (field.editable()) {
			if (editor == null) {
				editor = new CrosshairValueEditor(field.min, field.max, field == Field.COLOR, valueText());
				editor.selectAll();
			}
		} else {
			adjust(1);
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (!active || !isFocused()) return false;
		if (editor != null) {
			if (event.isEscape()) editor = null;
			else if (event.isConfirmation()) {
				if (editor.value().isPresent()) finishEdit();
			} else if (event.isSelectAll()) editor.selectAll();
			else if (event.key() == InputConstants.KEY_BACKSPACE) editor.backspace();
			else if (event.key() == InputConstants.KEY_DELETE) editor.delete();
			else if (event.isLeft()) editor.move(-1, event.hasShiftDown());
			else if (event.isRight()) editor.move(1, event.hasShiftDown());
			else if (event.key() == InputConstants.KEY_HOME) editor.edge(false, event.hasShiftDown());
			else if (event.key() == InputConstants.KEY_END) editor.edge(true, event.hasShiftDown());
			else return false;
			return true;
		}
		if (event.isConfirmation() || event.key() == InputConstants.KEY_SPACE && (tab || preset != null || field.checkbox())) {
			activate();
			return true;
		}
		if (field != null && field != Field.COLOR && !field.checkbox() && (event.isLeft() || event.isRight())) {
			adjust(event.isLeft() ? -1 : 1);
			return true;
		}
		return false;
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (!active || !isFocused() || editor == null) return false;
		editor.insert(event.codepoint());
		return true;
	}

	/** Every non-Escape departure commits valid text and discards invalid text. */
	void finishEdit() {
		if (editor == null) return;
		var value = editor.value();
		editor = null;
		if (value.isPresent()) update(withValue(value.getAsInt()));
	}

	String validationHint() {
		if (editor == null || editor.value().isPresent()) return "";
		return field == Field.COLOR ? "Use #RRGGBB" : "Use " + field.min + ".." + field.max;
	}

	private void update(CrosshairSettings settings) {
		configuration.update(settings);
		changed.run();
	}

	private void adjust(int direction) {
		CrosshairSettings settings = configuration.settings();
		switch (field) {
			case ENABLED -> update(settings.withEnabled(!settings.enabled()));
			case INVERTED -> update(settings.withInverted(!settings.inverted()));
			case TYPE -> update(settings.withType(Type.values()[Math.floorMod(settings.type().ordinal() + direction, Type.values().length)]));
			default -> update(withValue(Math.clamp(intValue() + direction, field.min, field.max)));
		}
	}

	private int intValue() {
		CrosshairSettings s = configuration.settings();
		return switch (field) {
			case ENABLED -> s.enabled() ? 1 : 0;
			case INVERTED -> s.inverted() ? 1 : 0;
			case TYPE -> s.type().ordinal();
			case SIZE -> s.size();
			case THICKNESS -> s.thickness();
			case GAP -> s.gap();
			case COLOR -> s.rgb();
			case OPACITY -> s.opacity();
		};
	}

	private CrosshairSettings withValue(int value) {
		CrosshairSettings s = configuration.settings();
		return switch (field) {
			case SIZE -> s.withSize(value);
			case THICKNESS -> s.withThickness(value);
			case GAP -> s.withGap(value);
			case COLOR -> s.withRgb(value);
			case OPACITY -> s.withOpacity(value);
			default -> throw new IllegalStateException("Not a value editor: " + field);
		};
	}

	private String valueText() {
		if (tab || preset != null) return selected.getAsBoolean() ? SELECTED : tab ? "" : SELECT;
		if (editor != null) return editor.text();
		if (field == Field.TYPE) return typeName(configuration.settings().type());
		if (field.checkbox()) return intValue() == 1 ? "On" : "Off";
		if (field == Field.COLOR) return String.format(java.util.Locale.ROOT, "#%06X", intValue());
		return Integer.toString(intValue());
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		if (getBottom() <= clipTop || getY() >= clipBottom) return;
		if (tab) {
			graphics.fill(getX(), getY(), getRight(), getBottom(), Palette.PANEL);
			String label = font.plainSubstrByWidth(getMessage().getString(), Math.max(0, width - 8));
			graphics.text(font, label, getX() + (width - font.width(label)) / 2,
				getY() + (height - font.lineHeight) / 2, Palette.TEXT, false);
			if (selected.getAsBoolean()) {
				graphics.fill(getX(), getBottom() - 1, getRight(), getBottom(), Palette.FOCUS);
			}
			if (isFocused()) {
				graphics.fill(getX() + 8, getBottom() - 4, getRight() - 8, getBottom() - 3, Palette.FOCUS);
			}
			return;
		}
		int right = getRight() - 12;
		int y = getY() + (height - font.lineHeight) / 2;
		int color = active ? Palette.TEXT : Palette.INACTIVE;
		String label = font.plainSubstrByWidth(getMessage().getString(), Math.max(0, width - 92));
		graphics.text(font, label, getX() + 12, y, color, false);
		if (field != null && field.checkbox()) {
			boolean on = intValue() == 1;
			int x = right - 16;
			int boxY = getY() + 5;
			String state = valueText();
			graphics.text(font, state, x - 8 - font.width(state), y, Palette.TEXT, false);
			graphics.fill(x, boxY, right, boxY + 16, on ? Palette.ON_BORDER : Palette.OFF_BORDER);
			graphics.fill(x + 3, boxY + 3, right - 3, boxY + 13, on ? Palette.ON_INSIDE : Palette.OFF_INSIDE);
			if (isFocused()) graphics.fill(x, boxY + 18, right, boxY + 19, Palette.FOCUS);
			return;
		}
		String value = valueText();
		int x = right - font.width(value);
		graphics.text(font, value, x, y, color, false);
		if (isFocused()) graphics.fill(Math.min(x, right - 16), getBottom() - 3, right, getBottom() - 2, Palette.FOCUS);
		if (editor != null) {
			int caretX = x + font.width(value.substring(0, editor.caret()));
			graphics.fill(caretX, y - 1, caretX + 1, y + font.lineHeight + 1, Palette.FOCUS);
			if (editor.hasSelection()) {
				int start = x + font.width(value.substring(0, editor.selectionStart()));
				int end = x + font.width(value.substring(0, editor.selectionEnd()));
				graphics.fill(start, y + font.lineHeight + 1, end, y + font.lineHeight + 2, Palette.FOCUS);
			}
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		String value = valueText();
		output.add(NarratedElementType.TITLE, Component.literal(getMessage().getString()
			+ (value.isEmpty() ? "" : ": " + value) + (active ? "" : ". " + UNAVAILABLE)));
		String usage = SELECT_USAGE;
		if (editor != null) usage = EDITING_USAGE + ". " + validationHint();
		else if (field != null) {
			usage = field.checkbox() ? TOGGLE_USAGE : field == Field.TYPE ? TYPE_USAGE
				: field == Field.COLOR ? COLOR_USAGE : EDIT_USAGE;
		}
		if (active) output.add(NarratedElementType.USAGE, Component.literal(usage));
	}
}
