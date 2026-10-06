package site.zvolcan.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import site.zvolcan.client.config.CrosshairConfiguration;
import site.zvolcan.client.config.CrosshairSettings;
import site.zvolcan.client.config.CrosshairSettings.Type;
import site.zvolcan.client.crosshair.Crosshair;
import site.zvolcan.client.crosshair.CrosshairPresets;
import site.zvolcan.client.screen.CrosshairOptionWidget.Field;
import site.zvolcan.client.screen.CrosshairOptionWidget.Palette;

/** Configuration UI only: opening/key registration and HUD policy belong to the client lifecycle. */
public final class CrosshairConfigScreen extends Screen {
	private static final String TITLE = "Crosshair configuration";
	private static final String SETTINGS_TAB = "Settings";
	private static final String PRESETS_TAB = "Presets";
	private static final String SAVED = "Saved";
	private static final String UNSAVED = "Unsaved";
	private static final String LOCKED_WARNING = "Saving unavailable";
	private static final String SAVE_WARNING = "Save failed";
	private static final String STORAGE_WARNING = "Storage warning";
	private static final String HELP = "Enter edit / Esc back";
	private static final String APPLY = "Apply";
	private static final String SAVE = "Save";
	private static final String EXIT = "Exit";

	private final Screen previousScreen;
	private final CrosshairConfiguration configuration;
	private final List<CrosshairOptionWidget> tabs = new ArrayList<>();
	private final List<CrosshairOptionWidget> rows = new ArrayList<>();
	private CrosshairScreenLayout layout;
	private int selectedTab;
	private int scroll;
	private final List<CrosshairOptionWidget> buttons = new ArrayList<>();
	private Crosshair preview;
	private CrosshairSettings previewSettings;

	public CrosshairConfigScreen(Screen previousScreen, CrosshairConfiguration configuration) {
		super(Component.literal(TITLE));
		this.previousScreen = previousScreen;
		this.configuration = Objects.requireNonNull(configuration, "configuration");
	}

	@Override
	protected void init() {
		finishEditors();
		tabs.clear();
		rows.clear();
		buttons.clear();
		layout = new CrosshairScreenLayout(width, height, selectedTab == 1);
		scroll = layout.clampScroll(scroll);
		for (int i = 0; i < 2; i++) {
			int tab = i;
			tabs.add(addRenderableWidget(CrosshairOptionWidget.tab(
				layout.left() + i * (CrosshairScreenLayout.TAB_WIDTH + CrosshairScreenLayout.TAB_GAP),
				font, i == 0 ? SETTINGS_TAB : PRESETS_TAB, () -> selectTab(tab), () -> selectedTab == tab)));
		}
		if (selectedTab == 0) {
			for (Field field : Field.values()) {
				rows.add(addRenderableWidget(CrosshairOptionWidget.option(layout.left(),
					layout.rowY(rows.size(), scroll), layout.panelWidth(), font,
					configuration, field, this::refreshRelevance, this::reveal)));
			}
		} else {
			for (Type type : Type.values()) {
				rows.add(addRenderableWidget(CrosshairOptionWidget.preset(layout.left(),
					layout.rowY(rows.size(), scroll), layout.panelWidth(), font,
					configuration, type, this::refreshRelevance, this::reveal)));
			}
		}
		buttons.add(addRenderableWidget(CrosshairOptionWidget.button(layout.left(), height - 50, 60, font, APPLY, configuration::apply)));
		buttons.add(addRenderableWidget(CrosshairOptionWidget.button(layout.left() + 66, height - 50, 60, font, SAVE, configuration::save)));
		buttons.add(addRenderableWidget(CrosshairOptionWidget.button(layout.left() + 132, height - 50, 60, font, EXIT, this::onClose)));
		refreshRelevance();
		positionRows();
	}

	@Override
	protected void setInitialFocus() {
		if (!rows.isEmpty()) setFocused(rows.getFirst());
	}

	private void selectTab(int tab) {
		if (selectedTab == tab) return;
		finishEditors();
		clearWidgets();
		selectedTab = tab;
		scroll = 0;
		init();
		setFocused(tabs.get(tab));
	}

	private void refreshRelevance() {
		for (CrosshairOptionWidget widget : rows) widget.updateRelevance();
		if (getFocused() instanceof CrosshairOptionWidget focused && !focused.active) {
			setFocused(rows.get(Field.TYPE.ordinal()));
		}
	}

	private void reveal(CrosshairOptionWidget widget) {
		int index = rows.indexOf(widget);
		if (index >= 0) {
			scroll = layout.reveal(index, scroll);
			positionRows();
		}
	}

	private void positionRows() {
		for (int i = 0; i < rows.size(); i++) {
			rows.get(i).setY(layout.rowY(i, scroll));
			rows.get(i).setClip(layout.top(), layout.bottom());
		}
	}

	private void finishEditors() {
		for (CrosshairOptionWidget widget : rows) widget.finishEdit();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
		// Screen owns focus before activation; departure resolves the previous edit first.
		for (GuiEventListener child : children()) {
			if (child instanceof CrosshairOptionWidget widget && widget.isMouseOver(event.x(), event.y())) {
				if (getFocused() != widget) setFocused(widget);
				return widget.mouseClicked(event, doubleClick);
			}
		}
		clearFocus();
		return false;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
		if (!layout.contains(x, y) || vertical == 0) return false;
		// Leave editing before its caret could be scrolled out of the visible viewport.
		finishEditors();
		scroll = layout.clampScroll((int) Math.clamp(scroll - vertical * CrosshairScreenLayout.ROW_HEIGHT,
			0, layout.maxScroll()));
		positionRows();
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		// Manual scrolling may hide focus; keyboard interaction must reveal it again.
		if (getFocused() instanceof CrosshairOptionWidget widget) reveal(widget);
		// Unlike the base Screen's Escape-first policy, editing gets first refusal.
		if (getFocused() != null && getFocused().keyPressed(event)) return true;
		if (event.isEscape()) {
			onClose();
			return true;
		}
		if (event.isCycleFocus() || event.isDown() || event.isUp()) {
			moveFocus(event.isUp() || event.isCycleFocus() && event.hasShiftDown() ? -1 : 1);
			return true;
		}
		if (tabs.contains(getFocused()) && (event.isLeft() || event.isRight())) {
			selectTab(event.isLeft() ? 0 : 1);
			return true;
		}
		return false;
	}

	private void moveFocus(int direction) {
		List<CrosshairOptionWidget> candidates = new ArrayList<>(tabs);
		candidates.addAll(buttons);
		for (CrosshairOptionWidget widget : rows) if (widget.active) candidates.add(widget);
		int index = candidates.indexOf(getFocused());
		int next = index < 0 ? direction > 0 ? 0 : candidates.size() - 1
			: Math.floorMod(index + direction, candidates.size());
		setFocused(candidates.get(next));
	}

	@Override
	public void resize(int width, int height) {
		int focusedIndex = children().indexOf(getFocused());
		finishEditors();
		super.resize(width, height);
		if (focusedIndex >= 0 && focusedIndex < children().size()) {
			GuiEventListener candidate = children().get(focusedIndex);
			if (candidate instanceof CrosshairOptionWidget widget && widget.active) setFocused(widget);
		}
	}

	@Override
	public void onClose() {
		finishEditors();
		minecraft.setScreenAndShow(previousScreen);
	}

	@Override
	public void removed() {
		finishEditors();
		super.removed();
	}

    /*@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, width, height, Palette.BACKGROUND);
	}*/

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		refreshRelevance();
		//extractBackground(graphics, mouseX, mouseY, delta);
		for (CrosshairOptionWidget tab : tabs) tab.extractRenderState(graphics, mouseX, mouseY, delta);
		for (CrosshairOptionWidget button : buttons) button.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.enableScissor(layout.left(), layout.top(), layout.left() + layout.panelWidth(), layout.bottom());
		for (int panel = 0; panel < layout.panelCount(); panel++) {
			int y = layout.panelY(panel, scroll);
			graphics.fill(layout.left(), y, layout.left() + layout.panelWidth(), y + layout.panelHeight(panel), Palette.PANEL);
		}
		for (CrosshairOptionWidget widget : rows) widget.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.disableScissor();
		if (!configuration.settings().equals(previewSettings)) {
			previewSettings = configuration.settings();
			preview = CrosshairPresets.create(previewSettings);
		}
		graphics.fill(layout.previewLeft(), layout.previewTop(),
			layout.previewLeft() + layout.previewSize(), layout.previewTop() + layout.previewSize(), 0xCC001006);
		if (preview != null && layout.previewSize() > 0) {
			graphics.enableScissor(layout.previewLeft(), layout.previewTop(),
				layout.previewLeft() + layout.previewSize(), layout.previewTop() + layout.previewSize());
			preview.draw(graphics, layout.previewCenterX(), layout.previewCenterY(), 4);
			graphics.disableScissor();
		}
		graphics.text(font, status(), layout.left(), height - 24, Palette.TEXT, false);
		String hint = footerHint();
		graphics.text(font, font.plainSubstrByWidth(hint, Math.max(0, width - 16)),
			layout.left(), height - 12, Palette.MUTED, false);
		if (!configuration.warning().isEmpty() && mouseY >= height - 28) {
			graphics.setTooltipForNextFrame(font, Component.literal(configuration.warning()), mouseX, mouseY);
		}
	}

	private String status() {
		return configuration.saved() ? SAVED : UNSAVED;
	}

	private String footerHint() {
		if (getFocused() instanceof CrosshairOptionWidget widget && !widget.validationHint().isEmpty()) {
			return widget.validationHint();
		}
		if (!configuration.warning().isEmpty()) {
			if (configuration.saved()) return STORAGE_WARNING;
			return configuration.writable() ? SAVE_WARNING : LOCKED_WARNING;
		}
		return HELP;
	}

	@Override
	public Component getNarrationMessage() {
		return Component.literal(TITLE + ". " + status());
	}

	@Override
	protected void updateNarrationState(NarrationElementOutput output) {
		super.updateNarrationState(output);
		if (!configuration.warning().isEmpty()) {
			output.add(NarratedElementType.HINT, Component.literal(configuration.warning()));
		}
	}
}
