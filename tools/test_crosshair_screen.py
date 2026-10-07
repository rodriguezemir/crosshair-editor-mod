"""Headless native UI behavior; narrow stubs do not simulate Minecraft/GPU rendering."""

import os
from pathlib import Path
import subprocess
import unittest

from test_crosshair_manager import java_tool
from test_crosshair_persistence import resolved_gson

ROOT = Path(__file__).resolve().parents[1]
CLIENT = ROOT / "src/client/java/site/zvolcan/client"
BOUNDARY = ROOT / "tools/crosshair_screen_test"


class CrosshairScreenTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # All generated output stays inside this task's authorized surface.
        cls.classes = BOUNDARY / ".classes"
        cls.classes.mkdir(exist_ok=True)
        cls.classpath = os.pathsep.join((str(cls.classes), str(resolved_gson())))
        old_boundary = ROOT / "tools/crosshair_manager_test"
        sources = [
            *(CLIENT / "screen" / name for name in
              ("CrosshairConfigScreen.java", "CrosshairScreenLayout.java",
               "CrosshairOptionWidget.java", "CrosshairValueEditor.java")),
            *(CLIENT / "config" / name for name in
              ("CrosshairSettings.java", "CrosshairSettingsStore.java", "CrosshairConfiguration.java")),
            *(CLIENT / "crosshair" / name for name in
              ("Crosshair.java", "CrosshairManager.java", "CrosshairPresets.java")),
            *BOUNDARY.rglob("*.java"),
            old_boundary / "com/mojang/blaze3d/pipeline/RenderPipeline.java",
            old_boundary / "net/minecraft/client/renderer/RenderPipelines.java",
        ]
        result = subprocess.run(
            [java_tool("javac"), "--release", "25", "-cp", cls.classpath,
             "-d", str(cls.classes), *(str(path) for path in sources)],
            capture_output=True, text=True, timeout=60,
        )
        if result.returncode:
            raise AssertionError(f"javac failed:\n{result.stdout}{result.stderr}")

    def run_case(self, name):
        directory = BOUNDARY / ".fixtures" / name
        directory.mkdir(parents=True, exist_ok=True)
        result = subprocess.run(
            [java_tool("java"), "-ea", "-cp", self.classpath,
             "site.zvolcan.client.screen.CrosshairScreenBehaviorTest", name, str(directory)],
            capture_output=True, text=True, timeout=30,
        )
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_responsive_uniform_layout_and_reveal(self):
        self.run_case("layout")

    def test_value_editor_validation_selection_caret_and_delete(self):
        self.run_case("editor")

    def test_native_render_palette_alignment_labelled_tabs_and_no_shadows(self):
        self.run_case("render")

    def test_mouse_keyboard_edit_commit_cancel_invalid_and_live_save(self):
        self.run_case("interaction")

    def test_type_relevance_boolean_keyboard_and_presets(self):
        self.run_case("types")

    def test_scroll_keyboard_reachability_clipping_and_fixed_chrome(self):
        self.run_case("scroll")

    def test_focus_tab_resize_close_commit_or_revert(self):
        self.run_case("lifecycle")

    def test_real_unsaved_warning_tooltip_and_narration(self):
        self.run_case("warning")

    def test_all_numeric_fields_boundaries_and_keyboard_selection(self):
        self.run_case("numeric")

    def test_visible_navigation_labels_all_viewports(self):
        self.run_case("labels")

    def test_checkbox_status_labels_and_narration_all_viewports(self):
        self.run_case("checkboxLabels")

    def test_preset_action_labels_and_narration_all_viewports(self):
        self.run_case("presetLabels")

    def test_labelled_controls_emit_actual_manager_fills_and_reload_json(self):
        self.run_case("output")

    def test_partially_clipped_boundary_row_clicks_and_inactive_hits(self):
        self.run_case("boundary")


if __name__ == "__main__":
    unittest.main()
