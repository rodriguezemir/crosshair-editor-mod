# Crosshair manager

## Objective and rationale
Separate crosshair drawing data from the HUD mixin. Register multiple crosshairs by name in a Map<String, Crosshair>, and select the one drawn by the HUD.

## Scope and constraints
- Client-only Crosshair model: immutable drawing rectangles/points relative to the crosshair center, RenderPipeline (including GUI_INVERT), ARGB color.
- CrosshairManager: named registration, lookup, read-only registry view, selection and selected rendering.
- Initialize the current white GUI_INVERT cross as the default and route the HUD redirect through the manager.
- Preserve vanilla anchor, redirect descriptor/ordinal, visibility and attack-indicator behavior. Default consists of three disjoint fills covering 17 pixels; center inverted exactly once.
- In-memory API only; no disk persistence, editor UI, shader changes or unrelated cleanup.
- Preserve pre-existing modifications in both mixin JSON files and pre-existing untracked tools.
- Branch: feat/crosshair-manager. No commits or publishing without an explicit user request.
- Engram mirror: odd/crosshair-manager/tasks, project crosshair-editor-mod (observation 82). Initial project lookup failed; resolved canonical project identity and saved successfully.

## Tasks
- [x] T1 — Create scope and tracking document. Verified branch and existing dirty paths; source writes have not started.
- [x] T2 — Add Crosshair and CrosshairManager with deterministic tests. Status: done; observed tests and real client compilation successful.
- [x] T3 — Initialize default and route existing HUD rendering through the manager; update usage documentation and geometry regression coverage. Status: done; observed regression tests, compilation and build succeeded.
- [x] T4 — Verify focused tests, client compilation and build; honor native review mode and report unavailable/manual checks. Status: done; independent functional verification passed, native review unavailable, manual live-client checks explicitly pending.

## Acceptance and checks
- Multiple names retrieve their corresponding objects; lookup and selection contracts are explicit and tested.
- Invalid names/null objects rejected; external callers cannot mutate the registry or point list through returned views.
- Drawing applies relative offsets and the configured pipeline/color; selected crosshair determines HUD output.
- Default retains exact 17-pixel cross geometry with no overlapping rectangles.
- Test-first: observe deterministic RED before implementation and GREEN afterward, with focused refactor checks. Prefer existing runners without adding unnecessary dependency/framework setup.
- Run relevant Python regression tests, ./gradlew compileClientJava and ./gradlew build, using available Java 25.
- Headless tests and compilation do not prove live Minecraft rendering/Mixin application; manual game verification remains pending.

## Evidence and progress
- T1: read existing initializer, HUD mixin and build configuration. Existing mixin draws three GUI_INVERT white fills directly.
- Native mode: on (global). inspect blocked with native-status-package-binary-missing; lineage_created=false and mutation_performed=false. No review lineage started. No installation changes attempted.
- assess(nativeReviewOutcome=unavailable): risk unassessable; requires writer self-verification and independent verifier. T4 verifier muwoza1r-3-uacq completed with no blocking defects.
- T4 independent checks: 13 tests passed; compileClientJava/build exit0 (incremental/up-to-date, Gradle test NO-SOURCE); git diff --check clean. Packaged jar includes model/manager/initializer/mixin/config; javap confirms configured relative fills, default registration/selection and HUD manager routing. README API example structurally checked, not separately compiled.
- Pending manual checks: live GPU inversion, runtime Mixin application, GUI scales, spectator/debug visibility, attack indicator. No game launched. No native review approval claimed.
- T2 RED: new headless harness failed because Crosshair.java did not exist; 5 pre-existing tests passed.
- T2 GREEN: all 11 tests passed; bash ./gradlew compileClientJava succeeded with installed JDK25. Parent spot check: all 6 manager behavior tests passed.
- T2 contracts: immutable relative rectangles, pipeline and ARGB color; ordered case-sensitive map, first registration selected, duplicates replace in place, unknown get returns null, unknown select throws without changing selection, empty draw is a no-op. Read-only registry view is live.
- T2 added two client classes and Python/Java headless behavior fixtures. Stub tests do not prove GPU rendering; real API compilation succeeded.
- T3 RED: HUD regression rejected direct fill drawing and actual initializer harness lacked getCrosshairManager().
- T3 GREEN: all 13 tests passed; compileClientJava and build succeeded (JDK25). Parent re-ran all 13 tests successfully and spot-read initializer/mixin integration.
- T3 default: shared client manager registers/selects default, emits three non-overlapping GUI_INVERT white fills. Alternate selection tests verify custom pipeline/color/geometry routing. README includes lifecycle/API usage.
- Work-unit commit identities: none; user has not explicitly requested commits.
- Rollback boundary: new model/manager, initializer/mixin integration, associated tests and README examples only; preserve unrelated dirty files.

## Next step
Automated implementation and independent verification complete. Use CrosshairEditorClient.getCrosshairManager().register(name, crosshair) and select(name); see README example. Next: manually verify the client in Minecraft. Native review remains unavailable; no commits or publishing performed.
