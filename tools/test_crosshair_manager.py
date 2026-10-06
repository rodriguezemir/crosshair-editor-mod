"""Behavior tests against actual Java sources with narrow Minecraft/Fabric stubs.

These do not validate real client API compatibility; compileClientJava does that.
"""

import os
from pathlib import Path
import shutil
import subprocess
import unittest


ROOT = Path(__file__).resolve().parents[1]
FIXTURES = ROOT / "tools/crosshair_manager_test"
SOURCES = ROOT / "src/client/java/site/zvolcan/client/crosshair"


def java_tool(name):
    home = os.environ.get("JAVA_HOME")
    path = str(Path(home) / "bin" / name) if home else shutil.which(name)
    if not path or not Path(path).is_file():
        raise RuntimeError(f"Required JDK tool unavailable: {name}")
    return path


class CrosshairManagerTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # Import lazily: persistence's runner also imports java_tool from this module.
        from test_crosshair_persistence import resolved_gson

        cls.classes = ROOT / "tools/crosshair_wiring_test/.classes/manager"
        cls.classes.mkdir(parents=True, exist_ok=True)
        cls.classpath = os.pathsep.join((str(cls.classes), str(resolved_gson())))
        command = [
            java_tool("javac"), "--release", "25", "-cp", cls.classpath, "-d", str(cls.classes),
            str(SOURCES / "Crosshair.java"),
            str(SOURCES / "CrosshairManager.java"),
            str(SOURCES / "CrosshairPresets.java"),
            *(str(SOURCES.parent / "config" / name) for name in
              ("CrosshairSettings.java", "CrosshairSettingsStore.java", "CrosshairConfiguration.java")),
            str(SOURCES.parent / "CrosshairEditorClient.java"),
            *(str(path) for path in sorted(FIXTURES.rglob("*.java"))),
        ]
        result = subprocess.run(command, capture_output=True, text=True, timeout=60)
        if result.returncode:
            raise AssertionError(f"javac failed:\n{result.stdout}{result.stderr}")

    def run_case(self, name):
        # Fresh directory without deleting previous fixtures; never use run/config.
        import uuid
        directory = ROOT / "tools/crosshair_wiring_test/.fixtures" / ("manager-" + name + "-" + uuid.uuid4().hex)
        directory.mkdir(parents=True)
        result = subprocess.run(
            [java_tool("java"), "-ea", f"-Dcrosshair.test.configDir={directory}", "-cp", self.classpath,
             "CrosshairManagerBehaviorTest", name],
            capture_output=True, text=True, timeout=30,
        )
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_initializer_default_is_exact_disjoint_white_inverted_cross(self):
        self.run_case("default")

    def test_shared_client_manager_routes_selected_alternate(self):
        self.run_case("clientSelection")

    def test_registration_lookup_and_order(self):
        self.run_case("registry")

    def test_selection_and_replacement(self):
        self.run_case("selection")

    def test_registry_and_point_list_are_read_only(self):
        self.run_case("immutable")

    def test_invalid_inputs_are_rejected(self):
        self.run_case("validation")

    def test_relative_fills_preserve_order_pipeline_and_argb(self):
        self.run_case("draw")

    def test_empty_manager_and_empty_crosshair_draw_nothing(self):
        self.run_case("empty")


if __name__ == "__main__":
    unittest.main()
