# Crosshair inversion with fill

Render the original 9x9, one-pixel cross through GUI_INVERT fills, replacing the Gaussian sprite interpretation. The user clarified that “blur” meant Difference Blend Mode / background-dependent inversion, and explicitly requested preserving .fill geometry.

## Scope and rationale
- Minecraft26.3: keep Hud.extractCrosshair ordinal-0 redirect, preserving vanilla visibility/attack-indicator logic.
- Use the verified public RenderPipelines.GUI_INVERT with GuiGraphicsExtractor.fill(RenderPipeline, int,int,int,int,int). No custom shader/pipeline, scene sampling, sprite draw or Gaussian blur at runtime.
- Opaque white INVERT produces per-channel 1-background, equivalent to white Difference; not a luminance-threshold black/white shader.
- Same original17-pixel union (9x9 one-pixel cross). Three disjoint fills: horizontal9, upper vertical4, lower vertical4. Shared center is drawn exactly once, avoiding double inversion.
- Anchor center to incoming vanilla x + width/2, y + height/2; keep original bounds/descriptor/ordinal.
- Existing offline Gaussian generator/tests/PNG stay preserved but inactive; do not delete untracked artifacts automatically.
- Preserve user edits to gradlew/main mixin config/.codegraph and existing client mixin registration. Remain on feat/crosshair-blur; no commit/publishing without explicit request.

## Tasks
- [x] T1 — Historical Gaussian generator/tests. Status: done but superseded/inactive after user clarification; no runtime usage intended.
- [x] T2 — Restore fill-based cross using GUI_INVERT and update README. Status: done (automated source/build checks). Previous sprite effect superseded.
  - Add focused deterministic structural geometry regression tests first; observe RED against current sprite-only source, then GREEN.
  - Allowed edits: src/client/java/site/zvolcan/client/mixin/InGameHudMixin.java; README.md; tools/test_crosshair_inversion.py.
  - Verify exact17 unique pixels, no overlapping fills or gap, white color and GUI_INVERT pipeline, unchanged injection/vanilla anchor.
- [x] T3 — Independently verify corrected inversion. Status: done (source/API/build/package checks).
  - Run both regression suites, compileClientJava/build; inspect cached pipeline factors and compiled calls/packaged class.
  - Runtime appearance/GPU blend and Mixin application require manual game checks; do not claim them tested.

## Checks and evidence
- Tests: PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tools -p 'test_crosshair*.py' -v.
- Build: bash ./gradlew compileClientJava; bash ./gradlew build (Java25).
- Verified API: GUI_INVERT is public untextured POSITION_COLOR/QUADS GUI pipeline. BlendFunction.INVERT RGB factors ONE_MINUS_DST_COLOR, ONE_MINUS_SRC_COLOR; alpha ONE,ZERO. fill supplies no texture. CROSSHAIR is textured and unsuitable for fill.
- Prior Gaussian tests3/compile/build passed; centering correction independently closed. That behavior is now superseded, not acceptance evidence for inversion.
- T2 RED: structural tests failed against sprite-only handler (no fills/no anchor); observed before source implementation.
- T2 GREEN:5 tests passed, compileClientJava/build succeeded; parent independently reran5 tests successfully. Worker javap confirmed3GUI_INVERT fill calls, original anchor, no sprite rendering.
- T3 independent verification PASS:5tests, compileClientJava/build, pipeline/factors/vertexformat,3disjoint fills/17pixels/one center, packaged class+client config; no source defect. Active handler contains no sprite draw.
- Corrected candidate native inspect/assessment unavailable (package-local-binary-missing); no lineage/authority mutation. Assessment requires independent verifier; T3 provides that fallback. No package/install changes.
- Manual checks: pending appearance on dark/light/color backgrounds, GUI scales, spectator/debug and attack indicator; no game launched.
- Commits: none; explicit user authorization absent.

## Next step
Automated correction complete. Next: manually launch Minecraft to inspect inversion on dark/light/color backgrounds, GUI scaling, spectator/debug visibility and attack indicator; no liveGPU/Mixin runtime proof yet. Keep Gaussian artifacts inactive and unrelated files untouched.
