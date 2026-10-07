"""Run real settings/preset Java behavior against existing narrow render stubs."""

from pathlib import Path
import subprocess
import tempfile
import unittest

from test_crosshair_manager import java_tool


ROOT = Path(__file__).resolve().parents[1]
CLIENT = ROOT / "src/client/java/site/zvolcan/client"
STUBS = ROOT / "tools/crosshair_manager_test"


class CrosshairConfigTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.classes = tempfile.TemporaryDirectory(prefix="crosshair-config-test-")
        cls.addClassCleanup(cls.classes.cleanup)
        sources = [
            CLIENT / "config/CrosshairSettings.java",
            CLIENT / "crosshair/CrosshairPresets.java",
            CLIENT / "crosshair/Crosshair.java",
            ROOT / "tools/crosshair_config_test/CrosshairConfigBehaviorTest.java",
            STUBS / "com/mojang/blaze3d/pipeline/RenderPipeline.java",
            STUBS / "net/minecraft/client/renderer/RenderPipelines.java",
            STUBS / "net/minecraft/client/gui/GuiGraphicsExtractor.java",
        ]
        result = subprocess.run(
            [java_tool("javac"), "--release", "25", "-d", cls.classes.name,
             *(str(path) for path in sources)],
            capture_output=True, text=True, timeout=60,
        )
        if result.returncode:
            raise AssertionError(f"javac failed:\n{result.stdout}{result.stderr}")

    def run_case(self, name):
        result = subprocess.run(
            [java_tool("java"), "-ea", "-cp", self.classes.name,
             "CrosshairConfigBehaviorTest", name],
            capture_output=True, text=True, timeout=30,
        )
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_defaults_preserve_exact_seventeen_pixel_cross(self):
        self.run_case("defaults")

    def test_validated_immutable_edits_and_control_relevance(self):
        self.run_case("settings")

    def test_all_presets_have_disjoint_bounded_geometry_at_limits(self):
        self.run_case("limits")

    def test_distinct_shapes_gap_and_even_thickness_conventions(self):
        self.run_case("geometry")

    def test_actual_fills_preserve_pipeline_specific_opacity_color(self):
        self.run_case("render")

    def test_zero_opacity_submits_no_rectangles_or_fills(self):
        self.run_case("zeroOpacity")

    def test_inversion_opacity_matches_blend_interpolation(self):
        self.run_case("inversionBlend")


if __name__ == "__main__":
    unittest.main()
