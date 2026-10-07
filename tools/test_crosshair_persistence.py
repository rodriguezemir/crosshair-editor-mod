"""Exercise actual persistence/controller sources with Minecraft's resolved Gson."""

import json
import os
from pathlib import Path
import subprocess
import uuid
import unittest

from test_crosshair_manager import java_tool


ROOT = Path(__file__).resolve().parents[1]
CLIENT = ROOT / "src/client/java/site/zvolcan/client"
STUBS = ROOT / "tools/crosshair_manager_test"


def minecraft_metadata(cache):
    properties = dict(line.split("=", 1) for line in
                      (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines()
                      if "=" in line and not line.startswith("#"))
    return cache / "fabric-loom" / properties["minecraft_version"] / "mojang_minecraft_info.json"


def resolved_gson():
    cache = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle")) / "caches"
    metadata = minecraft_metadata(cache)
    libraries = json.loads(metadata.read_text(encoding="utf-8"))["libraries"]
    version = next(entry["name"].split(":")[2] for entry in libraries
                   if entry["name"].startswith("com.google.code.gson:gson:"))
    jars = sorted((cache / "modules-2/files-2.1/com.google.code.gson/gson" / version)
                  .glob(f"*/gson-{version}.jar"))
    if len(jars) != 1:
        raise RuntimeError(f"Expected one resolved Gson {version} jar, found {jars}")
    return jars[0]


class CrosshairPersistenceTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.classes = ROOT / "tools/crosshair_wiring_test/.classes/persistence"
        cls.classes.mkdir(parents=True, exist_ok=True)
        cls.classpath = os.pathsep.join((str(cls.classes), str(resolved_gson())))
        sources = [
            CLIENT / "config/CrosshairSettings.java",
            CLIENT / "config/CrosshairSettingsStore.java",
            CLIENT / "config/CrosshairConfiguration.java",
            *(CLIENT / "crosshair" / name for name in
              ("Crosshair.java", "CrosshairManager.java", "CrosshairPresets.java")),
            ROOT / "tools/crosshair_config_test/CrosshairPersistenceBehaviorTest.java",
            STUBS / "com/mojang/blaze3d/pipeline/RenderPipeline.java",
            STUBS / "net/minecraft/client/renderer/RenderPipelines.java",
            STUBS / "net/minecraft/client/gui/GuiGraphicsExtractor.java",
        ]
        result = subprocess.run(
            [java_tool("javac"), "--release", "25", "-cp", cls.classpath,
             "-d", str(cls.classes), *(str(path) for path in sources)],
            capture_output=True, text=True, timeout=60,
        )
        if result.returncode:
            raise AssertionError(f"javac failed:\n{result.stdout}{result.stderr}")

    def run_case(self, name):
        directory = ROOT / "tools/crosshair_wiring_test/.fixtures" / ("persistence-" + name + "-" + uuid.uuid4().hex)
        directory.mkdir(parents=True)
        result = subprocess.run(
            [java_tool("java"), "-ea", "-cp", self.classpath,
             "CrosshairPersistenceBehaviorTest", name, str(directory)],
            capture_output=True, text=True, timeout=30,
        )
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_missing_file_defaults_and_all_fields_round_trip(self):
        self.run_case("roundTrip")

    def test_invalid_documents_preserve_bytes_and_lock_writes(self):
        self.run_case("invalid")

    def test_unreadable_and_oversized_files_use_locked_defaults(self):
        self.run_case("unreadable")

    def test_save_failures_are_unsaved_and_clean_temporary_siblings(self):
        self.run_case("saveFailure")

    def test_live_controller_updates_preserve_existing_entries(self):
        self.run_case("controller")

    def test_controller_keeps_live_edits_when_locked_or_save_fails(self):
        self.run_case("controllerFailure")

    def test_atomic_move_fallback_with_zip_filesystem(self):
        self.run_case("fallback")


if __name__ == "__main__":
    unittest.main()
