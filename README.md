# CrosshairEditor

Configure a native Cross, Dot, X, Circle, or Heart crosshair with live, saved settings. Turn **Enabled** off to restore the vanilla crosshair.

## Open configuration

In a world, enter **`/crosshaireditor`** in chat. This client-only command needs no OP or server permission and opens configuration after chat closes, even with the custom crosshair disabled.

Alternatively, press **backslash (`\`)**, the default **Open crosshair configuration** binding. Rebind it in **Options > Controls > Key Binds > Crosshair Editor** if needed. Close chat and other menus before pressing the key. Both openers share the same settings and never replace another screen on the opening tick.

Use the top-left **Settings** and **Presets** tabs (names shorten to fit). Presets offers **Cross**, **Dot**, **X**, **Circle**, and **Heart** with **Select** actions and a **Selected** status. **Enabled** and **Inverted** show **On/Off** beside their checkbox squares. A right-hand preview shows the configured crosshair at four-times scale; it shrinks or disappears when there is insufficient space. The screen is an overlay with no fullscreen background fill; its preview has a translucent backing.

| Action | Control |
| --- | --- |
| Edit a plain numeric/color value | Click it or focus it and press Enter; type, then Enter to confirm |
| Cancel editing | Escape; a second Escape closes the screen |
| Toggle a checkbox | Click, Space, or Enter |
| Navigate | Tab / Shift+Tab or Up / Down; focused rows scroll into view |
| Adjust without editing | Left / Right on a numeric value or Type |
| Scroll small viewports | Mouse wheel over the left panels |

Editing supports a caret, Left / Right, Backspace, Delete, and Ctrl+A. Leaving a field, switching tabs, resizing, or closing commits valid text and reverts invalid text. Invalid Enter leaves the field editable without changing settings.

## Settings and saving

| Setting | Values |
| --- | --- |
| Type | Cross, Dot, X, Circle, Heart |
| Size | 1–32 |
| Thickness | 1–8; inactive for Dot |
| Gap | 0–16; active only for Cross and X |
| Color | RGB hex, exactly `#RRGGBB` |
| Opacity | 0–100 percent |

Valid commits apply immediately and save to Fabric's config directory, normally `config/crosshaireditor.json`. The footer actions are **Apply** (reselect the configured crosshair without writing), **Save** (retry saving the current settings), and **Exit** (close, resolving any active edit). The footer reports **Saved** or **Unsaved** and any storage warning; hover the footer or use narration for the full warning.

Missing files use unsaved defaults until a commit. Invalid, unsupported, or unreadable files are preserved, and writes are blocked for that session. Repair the file manually and restart to re-enable saving. Edits still apply live while locked or after a save failure, but remain visibly unsaved; an IO failure can be retried by committing again.

## Inverted crosshair

Initialization retains the original named `default` cross and then selects `configured`, generated from the saved settings. The original uses three disjoint white `GUI_INVERT` fills covering 17 unique pixels in a 9×9 cross. Generated presets also avoid overlapping fills, so the shared center is not inverted twice. No Gaussian blur or texture is used at runtime; the historical generator, tests, and PNG are inactive.

At full opacity, white Difference blending produces `1 - background` per RGB channel. Arbitrary RGB colors use the `GUI_INVERT` equation `d + s * (1 - 2d)`; they are not a luminance-threshold effect or necessarily complete inversion. This pipeline ignores nonzero source alpha, so configured inversion opacity correctly attenuates source RGB instead; zero opacity emits no fills. With inversion off, `GUI` uses normal ARGB opacity.

## Crosshair API

Use the shared manager after client initialization, on the client/render thread only. Programmatic registrations and selections remain in-memory; the GUI settings are persisted separately. The HUD uses the selected object while custom crosshairs are enabled, and each settings commit reselects `configured`. For example:

```java
import java.util.List;
import net.minecraft.client.renderer.RenderPipelines;
import site.zvolcan.client.CrosshairEditorClient;
import site.zvolcan.client.crosshair.Crosshair;
import site.zvolcan.client.crosshair.Crosshair.Point;
import site.zvolcan.client.crosshair.CrosshairManager;

// Inside client/render-thread code, after client initialization:
CrosshairManager manager = CrosshairEditorClient.getCrosshairManager();
manager.register("green-dot", new Crosshair(
    List.of(new Point(-1, -1, 2, 2)), RenderPipelines.GUI, 0xFF00FF00));
manager.select("green-dot");
```

Points describe fill rectangles relative to the vanilla crosshair center: left/top inclusive, right/bottom exclusive. Colors are packed `0xAARRGGBB` (ARGB); `FF` alpha is opaque. Raw `GUI_INVERT` objects retain their pipeline's alpha behavior; the settings adapter performs the opacity correction described above.

Names are exact and case-sensitive. Registering an existing name replaces its crosshair; selecting that name uses the replacement. Missing lookup returns `null`; selecting an unknown name throws `IllegalArgumentException` without changing the selection. `registry()` is a live read-only view. Select `"default"` to restore the original custom cross while Enabled is on. `CrosshairEditorClient.getConfiguration()` exposes the shared settings owner after initialization and returns `null` before initialization.

## Verification

This `v26.1.2` branch targets **Minecraft 26.1.2 only**, with Java 25, Fabric Loader 0.19.5,
Fabric API 0.155.3+26.1.2, Mod Menu 18.0.2, and stable Fabric Loom 1.18.3.
Its HUD and active-screen APIs differ from Minecraft 26.2 and 26.3.

Minecraft 26.1.2 / Java 25 checks (compile first to populate the target dependency cache):

```bash
bash ./gradlew compileClientJava --rerun-tasks
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tools -p 'test_crosshair*.py' -v
bash ./gradlew build --rerun-tasks
```

Headless Java harnesses exercise actual settings, persistence, presets, manager, screen controls, initializer, client command dispatch, key callback, and HUD redirect logic against narrow Minecraft/Fabric boundaries, cached Gson, and real cached Brigadier. They test default geometry, visible labels/status, control-driven manager fills and JSON reloads, editing, small-viewport scrolling and clipped-row clicks, click draining, menu protection, and exact vanilla fallback arguments. Gradle compiles against the real client API; its test task may be `NO-SOURCE`, not a behavior-test run.

These checks do **not** prove runtime Mixin application, native device dispatch, GPU blending, or live screen visuals. Manual checks on dark/light/colored backgrounds, GUI scales, HUD visibility, and the attack indicator remain pending. No live Minecraft/GPU check has been performed.

## Development setup

Install a JDK 25 and set `JAVA_HOME` to it. Use the checked-in Gradle wrapper;
no separate Gradle installation is needed. Build with `bash ./gradlew build`.
The mod artifact is `build/libs/crosshaireditor-26.1.2-v0.0.1.jar` (not the sources jar).
Use Minecraft 26.1.2 and the dependency versions above for a development client.
The headless harnesses resolve Gson and Brigadier from the Loom cache for the
`minecraft_version` in `gradle.properties`, respecting `GRADLE_USER_HOME`.

See the [Fabric setup documentation](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) for your IDE.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
