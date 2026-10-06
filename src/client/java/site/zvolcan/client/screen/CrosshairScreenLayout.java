package site.zvolcan.client.screen;

import site.zvolcan.client.config.CrosshairSettings;

/** Pixel geometry shared by drawing, hit testing and keyboard scroll reveal. */
public final class CrosshairScreenLayout {
	public static final int ROW_HEIGHT = 26;
	public static final int MARGIN = 8;
	public static final int TAB_Y = 8;
	public static final int TAB_WIDTH = 34;
	public static final int TAB_HEIGHT = 22;
	public static final int TAB_GAP = 6;
	private static final int PANEL_PADDING = 4;
	private static final int PANEL_GAP = 8;

	private final int panelWidth;
	private final int bottom;
	private final boolean presets;
	private final int previewLeft;
	private final int previewSize;

	public CrosshairScreenLayout(int width, int height, boolean presets) {
		panelWidth = Math.max(0, Math.min(width - MARGIN * 2, Math.max(260, width * 2 / 5)));
		bottom = Math.max(top(), height - 56);
		this.presets = presets;
		previewLeft = left() + panelWidth + PANEL_GAP;
		int previewWidth = Math.max(0, width - MARGIN - previewLeft);
		previewSize = Math.max(0, Math.min(previewWidth, bottom - top()));
	}

	public int left() { return MARGIN; }
	public int panelWidth() { return panelWidth; }
	public int top() { return 38; }
	public int bottom() { return bottom; }
	public int previewLeft() { return previewLeft; }
	public int previewTop() { return top(); }
	public int previewSize() { return previewSize; }
	public int previewCenterX() { return previewLeft + previewSize / 2; }
	public int previewCenterY() { return top() + previewSize / 2; }
	public int panelCount() { return presets ? 1 : 3; }
	public int rowCount() { return presets ? CrosshairSettings.Type.values().length : 8; }

	public int panelHeight(int panel) {
		int rows = presets ? CrosshairSettings.Type.values().length : panel == 2 ? 2 : 3;
		return rows * ROW_HEIGHT + PANEL_PADDING * 2;
	}

	public int panelY(int panel, int scroll) {
		return top() + panel * (3 * ROW_HEIGHT + PANEL_PADDING * 2 + PANEL_GAP) - scroll;
	}

	public int rowY(int row, int scroll) {
		int panel = presets ? 0 : row / 3;
		return top() + PANEL_PADDING + row * ROW_HEIGHT
			+ panel * (PANEL_PADDING * 2 + PANEL_GAP) - scroll;
	}

	public int maxScroll() {
		int content = rowCount() * ROW_HEIGHT + panelCount() * PANEL_PADDING * 2
			+ (panelCount() - 1) * PANEL_GAP;
		return Math.max(0, content - (bottom - top()));
	}

	public int clampScroll(int scroll) {
		return Math.clamp(scroll, 0, maxScroll());
	}

	/** Reveal the entire focused row, not just its label or its top edge. */
	public int reveal(int row, int scroll) {
		int y = rowY(row, scroll);
		if (y < top()) scroll -= top() - y;
		else if (y + ROW_HEIGHT > bottom) scroll += y + ROW_HEIGHT - bottom;
		return clampScroll(scroll);
	}

	public boolean contains(double x, double y) {
		return x >= left() && x < left() + panelWidth && y >= top() && y < bottom;
	}
}
