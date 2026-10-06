import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Redirect;
import site.zvolcan.CrosshairEditor;
import site.zvolcan.client.CrosshairCommands;
import site.zvolcan.client.CrosshairEditorClient;
import site.zvolcan.client.CrosshairKeyMappings;
import site.zvolcan.client.config.*;
import site.zvolcan.client.crosshair.*;
import site.zvolcan.client.mixin.InGameHudMixin;
import site.zvolcan.client.screen.CrosshairConfigScreen;

/** Executes real production lifecycle, callback and redirect logic through narrow boundaries. */
public final class CrosshairWiringBehaviorTest {
	private static final Identifier SPRITE = Identifier.fromNamespaceAndPath("minecraft", "hud/crosshair");
	private static final Crosshair CUSTOM = new Crosshair(List.of(new Crosshair.Point(-2, -1, 3, 2)),
		RenderPipelines.GUI, 0x8044CC22);
	private static final class MixinInstance extends InGameHudMixin {}

	public static void main(String[] args) throws Exception {
		switch (args[0]) {
			case "load" -> load();
			case "invalid" -> invalid();
			case "preinit" -> preinit();
			case "disabled" -> disabled();
			case "enabled" -> enabled();
			case "tick" -> tick();
			case "key" -> key(Path.of(args[1]));
			case "anchor" -> anchor();
			case "commandRegistration" -> commandRegistration();
			case "commandChat" -> commandChat();
			case "commandDiscard" -> commandDiscard();
			case "commandUnavailable" -> commandUnavailable();
			default -> throw new AssertionError(args[0]);
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
	private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("crosshaireditor.json"); }
	private static CrosshairConfiguration initialize() {
		new CrosshairEditorClient().onInitializeClient();
		return CrosshairEditorClient.getConfiguration();
	}
	private static Method redirect() throws Exception {
		Method method = InGameHudMixin.class.getDeclaredMethod("drawCustomCrosshair", GuiGraphicsExtractor.class,
			RenderPipeline.class, Identifier.class, int.class, int.class, int.class, int.class);
		method.setAccessible(true);
		return method;
	}
	private static GuiGraphicsExtractor draw(RenderPipeline pipeline, Identifier sprite,
		int x, int y, int width, int height) throws Exception {
		GuiGraphicsExtractor graphics = new GuiGraphicsExtractor();
		redirect().invoke(new MixinInstance(), graphics, pipeline, sprite, x, y, width, height);
		return graphics;
	}

	private static void load() throws Exception {
		CrosshairSettings persisted = CrosshairSettings.defaults().withEnabled(false).withType(CrosshairSettings.Type.CIRCLE)
			.withSize(12).withGap(9).withThickness(3).withRgb(0x123ABC).withOpacity(65).withInverted(false);
		check(new CrosshairSettingsStore(file()).save(persisted).saved(), "prepare persisted fixture");
		String original = Files.readString(file());
		CrosshairManager manager = CrosshairEditorClient.getCrosshairManager();
		manager.register("external", CUSTOM);
		CrosshairConfiguration config = initialize();
		check(config != null && config == CrosshairEditorClient.getConfiguration(), "single shared configuration");
		check(config.settings().equals(persisted) && config.saved(), "load all persisted settings");
		check(!CrosshairEditorClient.isCustomCrosshairEnabled(), "enabled policy reflects loaded setting");
		check(manager.selected() == manager.get("configured"), "configured selection");
		check(manager.registry().keySet().equals(java.util.Set.of("external", "default", "configured")), "preserve external plus two built-ins");
		check(manager.get("external") == CUSTOM, "preserve external identity");
		Crosshair originalDefault = manager.get("default");
		check(originalDefault.points().equals(List.of(new Crosshair.Point(-4, 0, 5, 1),
			new Crosshair.Point(0, -4, 1, 0), new Crosshair.Point(0, 1, 1, 5))), "retain named original geometry");
		Crosshair expected = CrosshairPresets.create(persisted);
		check(manager.selected().points().equals(expected.points()) && manager.selected().color() == expected.color()
			&& manager.selected().pipeline() == RenderPipelines.GUI, "loaded geometry color and pipeline");
		manager.select("external");
		new CrosshairEditorClient().onInitializeClient();
		check(config == CrosshairEditorClient.getConfiguration() && originalDefault == manager.get("default"), "repeat init preserves both objects");
		check(manager.selected() == CUSTOM, "repeat init preserves programmatic selection");
		check(KeyMappingHelper.registered.size() == 1 && ClientTickEvents.END_CLIENT_TICK.callbacks.size() == 1, "repeat init does not register twice");
		check(Files.readString(file()).equals(original), "startup and repeat init do not rewrite loaded file");
	}

	private static void invalid() throws Exception {
		Files.writeString(file(), "not JSON - preserve me");
		CrosshairConfiguration config = initialize();
		check(config.settings().equals(CrosshairSettings.defaults()) && !config.saved() && !config.writable(), "locked startup defaults");
		check(!config.warning().isEmpty(), "warning available to UI");
		check(CrosshairEditor.LOGGER.warnings.contains(config.warning()), "startup warning logged");
		config.update(config.settings().withType(CrosshairSettings.Type.DOT).withEnabled(false));
		check(!config.saved() && !CrosshairEditorClient.isCustomCrosshairEnabled(), "locked edits remain active unsaved");
		check(Files.readString(file()).equals("not JSON - preserve me"), "invalid original unmodified");
	}

	private static void preinit() throws Exception {
		check(CrosshairEditorClient.getConfiguration() == null && CrosshairEditorClient.isCustomCrosshairEnabled(), "safe explicit preinit policy");
		CrosshairEditorClient.getCrosshairManager().register("external", CUSTOM);
		GuiGraphicsExtractor g = draw(RenderPipelines.GUI_INVERT, SPRITE, 10, -20, 13, 9);
		check(g.blits.isEmpty() && g.fills.equals(List.of(new GuiGraphicsExtractor.Fill(RenderPipelines.GUI, 14, -17, 19, -14, 0x8044CC22))),
			"preinit preserves manager drawing without configuration null crash");
	}

	private static void disabled() throws Exception {
		CrosshairConfiguration config = initialize();
		config.update(config.settings().withEnabled(false));
		CrosshairEditorClient.getCrosshairManager().register("external", CUSTOM);
		CrosshairEditorClient.getCrosshairManager().select("external");
		RenderPipeline incoming = new RenderPipeline();
		Identifier sprite = Identifier.fromNamespaceAndPath("example", "custom_sprite");
		for (int[] rect : new int[][] {{10, -20, 13, 9}, {-3, 7, 8, 14}, {0, 0, 0, 0}}) {
			GuiGraphicsExtractor g = draw(incoming, sprite, rect[0], rect[1], rect[2], rect[3]);
			check(g.fills.isEmpty(), "disabled emits no custom fills");
			check(g.blits.equals(List.of(new GuiGraphicsExtractor.Blit(incoming, sprite, rect[0], rect[1], rect[2], rect[3]))),
				"forward pipeline/sprite/position/extents unchanged exactly once");
			check(g.blits.getFirst().pipeline() == incoming && g.blits.getFirst().sprite() == sprite, "retain original identities");
		}
	}

	private static void enabled() throws Exception {
		CrosshairConfiguration config = initialize();
		check(!Files.exists(file()) && !config.saved(), "missing file startup does not write unsaved defaults");
		CrosshairManager manager = CrosshairEditorClient.getCrosshairManager();
		manager.register("external", CUSTOM); manager.select("external");
		GuiGraphicsExtractor g = draw(RenderPipelines.GUI_INVERT, SPRITE, 10, -20, 13, 9);
		check(g.blits.isEmpty(), "enabled does not also emit vanilla sprite");
		check(g.fills.equals(List.of(new GuiGraphicsExtractor.Fill(RenderPipelines.GUI, 14, -17, 19, -14, 0x8044CC22))),
			"uses selected programmatic object with vanilla center");
		config.update(config.settings().withType(CrosshairSettings.Type.DOT).withSize(1).withInverted(false).withRgb(0x123456));
		check(manager.selected() == manager.get("configured"), "config edit reselects configured");
		g = draw(RenderPipelines.GUI_INVERT, SPRITE, 10, -20, 13, 9);
		check(g.blits.isEmpty() && g.fills.equals(List.of(new GuiGraphicsExtractor.Fill(RenderPipelines.GUI, 16, -16, 17, -15, 0xFF123456))),
			"subsequent HUD draws live configured edit");
	}

	private static void tick() {
		CrosshairKeyMappings.register();
		KeyMapping key = KeyMappingHelper.registered.getFirst();
		Minecraft client = new Minecraft(); client.level = new Object();
		key.queueClicks(2); ClientTickEvents.END_CLIENT_TICK.fire(client);
		check(client.openings == 0 && !key.consumeClick(), "preinit clicks drained safely");
		CrosshairConfiguration config = initialize();
		ClientTickEvents.END_CLIENT_TICK.fire(client);
		check(client.openings == 0, "no stale preinit click");
		client.level = null; key.queueClicks(2); ClientTickEvents.END_CLIENT_TICK.fire(client);
		check(client.openings == 0 && !key.consumeClick(), "no world does not open; clicks drained");
		client.level = new Object();
		Screen typing = new Screen(); client.gui.setScreen(typing);
		key.queueClicks(3); ClientTickEvents.END_CLIENT_TICK.fire(client);
		check(client.gui.screen() == typing && client.openings == 0 && !key.consumeClick(), "typing/other menus preserved");
		client.gui.setScreen(null); ClientTickEvents.END_CLIENT_TICK.fire(client);
		check(client.openings == 0, "menu clicks do not open later");
		config.update(config.settings().withEnabled(false));
		key.queueClicks(4); ClientTickEvents.END_CLIENT_TICK.fire(client);
		check(client.openings == 1 && !key.consumeClick(), "queued clicks open once and all drained, even when custom crosshair disabled");
		check(client.gui.screen() instanceof CrosshairConfigScreen, "native config screen opened");
		CrosshairConfigScreen screen = (CrosshairConfigScreen) client.gui.screen();
		check(screen.previous == null && screen.configuration == config, "same live configuration and nullable previous");
		key.queueClicks(2); ClientTickEvents.END_CLIENT_TICK.fire(client);
		check(client.gui.screen() == screen && client.openings == 1 && !key.consumeClick(), "open screen not replaced");
		client.gui.setScreen(null); ClientTickEvents.END_CLIENT_TICK.fire(client);
		check(client.openings == 1, "closing does not replay consumed clicks");
		key.queueClicks(1); ClientTickEvents.END_CLIENT_TICK.fire(client);
		check(client.openings == 2 && ((CrosshairConfigScreen) client.gui.screen()).configuration == config, "reopen shares single state");
	}

	private static void key(Path language) throws Exception {
		CrosshairKeyMappings.register(); CrosshairKeyMappings.register(); initialize();
		check(KeyMappingHelper.registered.size() == 1 && KeyMapping.Category.registered.size() == 1
			&& ClientTickEvents.END_CLIENT_TICK.callbacks.size() == 1, "category/key/tick registration idempotent");
		KeyMapping key = KeyMappingHelper.registered.getFirst();
		check(key.getDefaultKey().getValue() == InputConstants.UNKNOWN.getValue(), "unbound native UNKNOWN default");
		key.setKey(new InputConstants.Key(79));
		check(key.boundKey().getValue() == 79 && key.getDefaultKey().getValue() == -1, "native mapping remains remappable");
		JsonObject labels = JsonParser.parseString(Files.readString(language)).getAsJsonObject();
		check(labels.get(key.getName()).getAsString().equals("Open crosshair configuration"), "actual binding name localized");
		check(key.getCategory().id().namespace().equals("crosshaireditor"), "namespaced native category");
		check(labels.get(key.getCategory().labelKey()).getAsString().equals("Crosshair Editor"), "verified 26.3 category translation scheme");
	}

	private record Source(Minecraft getClient, List<String> errors) implements FabricClientCommandSource {
		Source(Minecraft client) { this(client, new ArrayList<>()); }
		@Override public void sendError(Component message) { errors.add(message.getString()); }
	}

	private static CommandDispatcher<FabricClientCommandSource> dispatcher() {
		check(ClientCommandRegistrationCallback.EVENT.callbacks.size() == 1, "one client command listener registered");
		CommandDispatcher<FabricClientCommandSource> dispatcher = new CommandDispatcher<>();
		ClientCommandRegistrationCallback.EVENT.fire(dispatcher);
		check(dispatcher.getRoot().getChild("crosshaireditor") != null, "client literal registered on supplied dispatcher");
		return dispatcher;
	}

	private static void commandRegistration() throws Exception {
		initialize();
		CommandDispatcher<FabricClientCommandSource> first = dispatcher();
		CrosshairCommands.register();
		new CrosshairEditorClient().onInitializeClient();
		CommandDispatcher<FabricClientCommandSource> second = dispatcher();
		check(first != second && first.getRoot().getChildren().size() == 1
			&& second.getRoot().getChildren().size() == 1, "each independent dispatcher receives exactly one literal");
		Source source = new Source(new Minecraft());
		try {
			second.execute("crosshaireditor unexpected", source);
			throw new AssertionError("real Brigadier must reject unexpected arguments");
		} catch (CommandSyntaxException expected) {
			check(source.errors.isEmpty(), "invalid syntax never executes command body");
		}
	}

	private static void commandChat() throws Exception {
		CrosshairConfiguration config = initialize();
		config.update(config.settings().withEnabled(false));
		CommandDispatcher<FabricClientCommandSource> dispatcher = dispatcher();
		Minecraft client = new Minecraft(); client.level = new Object();
		Source source = new Source(client);
		Screen chat = new Screen(); client.gui.setScreen(chat);
		check(dispatcher.execute("crosshaireditor", source) == 1, "client command succeeds without permissions");
		check(client.gui.screen() == chat && client.openings == 0, "dispatch only queues, never replaces chat synchronously");
		client.gui.setScreen(null); // Simulated native chat submit closes its own screen after dispatch.
		ClientTickEvents.END_CLIENT_TICK.fire(client);
		check(client.openings == 1 && client.gui.screen() instanceof CrosshairConfigScreen, "next tick survives chat closure");
		CrosshairConfigScreen screen = (CrosshairConfigScreen) client.gui.screen();
		check(screen.configuration == config && screen.previous == null, "shared disabled settings, return to game not chat");
		check(source.errors.isEmpty() && !CrosshairEditorClient.isCustomCrosshairEnabled(), "disabled remains disabled without command errors");
		client.gui.setScreen(null); ClientTickEvents.END_CLIENT_TICK.fire(client);
		check(client.openings == 1, "closing config does not replay command");
	}

	private static void commandDiscard() throws Exception {
		initialize();
		CommandDispatcher<FabricClientCommandSource> dispatcher = dispatcher();
		Minecraft client = new Minecraft(); client.level = new Object();
		Source source = new Source(client);
		KeyMapping key = KeyMappingHelper.registered.getFirst();
		for (int i = 0; i < 3; i++) check(dispatcher.execute("crosshaireditor", source) == 1, "repeat command queued");
		key.queueClicks(3); ClientTickEvents.END_CLIENT_TICK.fire(client);
		check(client.openings == 1 && !key.consumeClick(), "duplicate command and key requests coalesce and drain");
		Screen configScreen = client.gui.screen();
		dispatcher.execute("crosshaireditor", source); ClientTickEvents.END_CLIENT_TICK.fire(client);
		check(client.gui.screen() == configScreen && client.openings == 1, "existing config never replaced");
		client.gui.setScreen(null); ClientTickEvents.END_CLIENT_TICK.fire(client);
		check(client.openings == 1, "request made with config open never replays");
		dispatcher.execute("crosshaireditor", source);
		Screen menu = new Screen(); client.gui.setScreen(menu);
		key.queueClicks(2); ClientTickEvents.END_CLIENT_TICK.fire(client);
		check(client.gui.screen() == menu && client.openings == 1 && !key.consumeClick(), "intervening menu preserved and all requests discarded");
		client.gui.setScreen(null); ClientTickEvents.END_CLIENT_TICK.fire(client);
		check(client.openings == 1, "menu-discarded command never opens later");
		dispatcher.execute("crosshaireditor", source); client.level = null;
		ClientTickEvents.END_CLIENT_TICK.fire(client);
		client.level = new Object(); ClientTickEvents.END_CLIENT_TICK.fire(client);
		check(client.openings == 1, "disconnect discards command across reconnect");
		check(dispatcher.execute("crosshaireditor", source) == 1, "fresh request after discard works");
		ClientTickEvents.END_CLIENT_TICK.fire(client);
		check(client.openings == 2, "fresh request opens once");
	}

	private static void commandUnavailable() throws Exception {
		// Register the command before initialization to exercise unavailable settings honestly.
		CrosshairCommands.register();
		CommandDispatcher<FabricClientCommandSource> dispatcher = dispatcher();
		Minecraft client = new Minecraft(); client.level = new Object();
		Source source = new Source(client);
		check(dispatcher.execute("crosshaireditor", source) == 0 && source.errors.size() == 1, "missing configuration fails gracefully");
		initialize(); ClientTickEvents.END_CLIENT_TICK.fire(client);
		check(client.openings == 0, "failed preinit request not latently queued");
		client.level = null;
		check(dispatcher.execute("crosshaireditor", source) == 0 && source.errors.size() == 2, "missing world fails gracefully");
		Source unavailableClient = new Source(null);
		check(dispatcher.execute("crosshaireditor", unavailableClient) == 0 && unavailableClient.errors.size() == 1, "missing client fails gracefully");
		check(source.errors.stream().allMatch(message -> !message.isBlank())
			&& !unavailableClient.errors.getFirst().isBlank(), "errors contain readable messages");
		client.level = new Object(); ClientTickEvents.END_CLIENT_TICK.fire(client);
		check(client.openings == 0, "unavailable-source requests do not open after recovery");
	}

	private static void anchor() throws Exception {
		check(InGameHudMixin.class.getAnnotation(Mixin.class).value()[0] == Hud.class, "same HUD annotation target");
		Redirect annotation = redirect().getAnnotation(Redirect.class);
		check(annotation.method().equals("extractCrosshair(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"), "exact enclosing descriptor");
		check(annotation.at().value().equals("INVOKE") && annotation.at().ordinal() == 0, "original invoke anchor ordinal");
		check(annotation.at().target().equals("Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"),
			"exact original sprite-call target descriptor");
	}
}
