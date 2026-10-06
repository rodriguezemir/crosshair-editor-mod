package site.zvolcan.client.screen;

import java.util.OptionalInt;

/** Small bounded single-line editor. Invalid intermediate text never becomes settings. */
public final class CrosshairValueEditor {
	private final int min;
	private final int max;
	private final boolean color;
	private String text;
	private int caret;
	private int anchor;

	public CrosshairValueEditor(int min, int max, boolean color, String initial) {
		this.min = min;
		this.max = max;
		this.color = color;
		text = initial;
		caret = anchor = initial.length();
	}

	public String text() { return text; }
	public int caret() { return caret; }
	public int selectionStart() { return Math.min(caret, anchor); }
	public int selectionEnd() { return Math.max(caret, anchor); }
	public boolean hasSelection() { return caret != anchor; }

	public void selectAll() {
		anchor = 0;
		caret = text.length();
	}

	public void move(int direction, boolean select) {
		if (!select && hasSelection()) caret = direction < 0 ? selectionStart() : selectionEnd();
		else caret = Math.clamp(caret + direction, 0, text.length());
		if (!select) anchor = caret;
	}

	public void edge(boolean end, boolean select) {
		caret = end ? text.length() : 0;
		if (!select) anchor = caret;
	}

	public void insert(int codepoint) {
		boolean digit = codepoint >= '0' && codepoint <= '9';
		boolean hex = codepoint >= 'a' && codepoint <= 'f' || codepoint >= 'A' && codepoint <= 'F';
		if (!digit && !(color && (hex || codepoint == '#'))) return;
		int start = selectionStart();
		int end = selectionEnd();
		// Seven ASCII characters bound parsing, caret drawing and malicious input alike.
		if (text.length() - (end - start) >= 7) return;
		text = text.substring(0, start) + (char) codepoint + text.substring(end);
		caret = anchor = start + 1;
	}

	public void backspace() {
		if (deleteSelection()) return;
		if (caret > 0) {
			text = text.substring(0, caret - 1) + text.substring(caret);
			anchor = --caret;
		}
	}

	public void delete() {
		if (deleteSelection()) return;
		if (caret < text.length()) text = text.substring(0, caret) + text.substring(caret + 1);
	}

	private boolean deleteSelection() {
		if (!hasSelection()) return false;
		int start = selectionStart();
		text = text.substring(0, start) + text.substring(selectionEnd());
		caret = anchor = start;
		return true;
	}

	public OptionalInt value() {
		if (color && !text.matches("#[0-9a-fA-F]{6}")) return OptionalInt.empty();
		if (!color && !text.matches("[0-9]+")) return OptionalInt.empty();
		try {
			int value = Integer.parseInt(color ? text.substring(1) : text, color ? 16 : 10);
			return value >= min && value <= max ? OptionalInt.of(value) : OptionalInt.empty();
		} catch (NumberFormatException ignored) {
			return OptionalInt.empty();
		}
	}
}
