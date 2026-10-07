"""Actual initializer/key mapping/HUD behavior with narrow Fabric/Minecraft stubs.

No runtime Mixin application, device dispatch, or GPU rendering is simulated.
"""

import json
import os
from pathlib import Path
import subprocess
import unittest
import uuid
from unittest.mock import patch

from test_crosshair_manager import java_tool
from test_crosshair_persistence import minecraft_metadata, resolved_gson

ROOT = Path(__file__).resolve().parents[1]
CLIENT = ROOT / "src/client/java/site/zvolcan/client"
BOUNDARY = ROOT / "tools/crosshair_wiring_test"
MANAGER = ROOT / "tools/crosshair_manager_test"


def resolved_brigadier():
    cache = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle")) / "caches"
    metadata = minecraft_metadata(cache)
    libraries = json.loads(metadata.read_text(encoding="utf-8"))["libraries"]
    version = next(entry["name"].split(":")[2] for entry in libraries
                   if entry["name"].startswith("com.mojang:brigadier:"))
    jars = sorted((cache / "modules-2/files-2.1/com.mojang/brigadier" / version)
                  .glob(f"*/brigadier-{version}.jar"))
    if len(jars) != 1:
        raise RuntimeError(f"Expected one resolved Brigadier {version} jar, found {jars}")
    return jars[0]


class CrosshairDependencyCacheTests(unittest.TestCase):
    def test_resolvers_use_branch_target_metadata_not_another_cached_game(self):
        properties = dict(line.split("=", 1) for line in
                          (ROOT / "gradle.properties").read_text().splitlines()
                          if "=" in line and not line.startswith("#"))
        cache = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle")) / "caches"
        expected = cache / "fabric-loom" / properties["minecraft_version"] / "mojang_minecraft_info.json"
        metadata = json.dumps({"libraries": [
            {"name": "com.google.code.gson:gson:9.8.7"},
            {"name": "com.mojang:brigadier:6.5.4"},
        ]})
        for resolver, artifact, version in ((resolved_gson, "gson", "9.8.7"),
                                            (resolved_brigadier, "brigadier", "6.5.4")):
            with self.subTest(artifact=artifact):
                reads, lookups = [], []

                def read_text(path, **kwargs):
                    if path == ROOT / "gradle.properties":
                        return f"minecraft_version={properties['minecraft_version']}\n"
                    reads.append(path)
                    return metadata

                def glob(path, pattern):
                    lookups.append((path, pattern))
                    return iter([path / "hash" / f"{artifact}-{version}.jar"])

                with patch.object(Path, "read_text", read_text), patch.object(Path, "glob", glob):
                    jar = resolver()
                self.assertEqual([expected], reads)
                self.assertEqual(1, len(lookups))
                self.assertEqual(version, lookups[0][0].name)
                self.assertEqual(f"*/{artifact}-{version}.jar", lookups[0][1])
                self.assertEqual(f"{artifact}-{version}.jar", jar.name)


class CrosshairWiringTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.classes = BOUNDARY / ".classes/wiring"
        cls.classes.mkdir(parents=True, exist_ok=True)
        cls.classpath = os.pathsep.join((str(cls.classes), str(resolved_gson()), str(resolved_brigadier())))
        sources = [
            CLIENT / "CrosshairEditorClient.java",
            CLIENT / "CrosshairKeyMappings.java",
            CLIENT / "CrosshairCommands.java",
            ROOT / "tools/crosshair_screen_test/net/minecraft/network/chat/Component.java",
            CLIENT / "mixin/InGameHudMixin.java",
            *(CLIENT / "config" / name for name in
              ("CrosshairSettings.java", "CrosshairSettingsStore.java", "CrosshairConfiguration.java")),
            *(CLIENT / "crosshair" / name for name in
              ("Crosshair.java", "CrosshairManager.java", "CrosshairPresets.java")),
            *BOUNDARY.rglob("*.java"),
            MANAGER / "com/mojang/blaze3d/pipeline/RenderPipeline.java",
            MANAGER / "net/minecraft/client/renderer/RenderPipelines.java",
            MANAGER / "net/fabricmc/api/ClientModInitializer.java",
            MANAGER / "net/fabricmc/loader/api/FabricLoader.java",
            MANAGER / "site/zvolcan/CrosshairEditor.java",
        ]
        result = subprocess.run(
            [java_tool("javac"), "--release", "25", "-cp", cls.classpath,
             "-d", str(cls.classes), *(str(path) for path in sources)],
            capture_output=True, text=True, timeout=60,
        )
        if result.returncode:
            raise AssertionError(f"javac failed:\n{result.stdout}{result.stderr}")

    def run_case(self, name):
        directory = BOUNDARY / ".fixtures" / (name + "-" + uuid.uuid4().hex)
        directory.mkdir(parents=True)
        language = ROOT / "src/main/resources/assets/crosshaireditor/lang/en_us.json"
        result = subprocess.run(
            [java_tool("java"), "-ea", f"-Dcrosshair.test.configDir={directory}",
             "-cp", self.classpath, "CrosshairWiringBehaviorTest", name, str(language)],
            capture_output=True, text=True, timeout=30,
        )
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_load_selects_configured_and_keeps_original_default_and_external_entries(self):
        self.run_case("load")

    def test_invalid_startup_preserves_file_warns_and_keeps_live_unsaved_edits(self):
        self.run_case("invalid")

    def test_preinitialization_enabled_fallback_is_safe(self):
        self.run_case("preinit")

    def test_disabled_hud_forwards_original_sprite_and_every_argument(self):
        self.run_case("disabled")

    def test_enabled_hud_draws_selected_programmatic_object_then_configured_edits(self):
        self.run_case("enabled")

    def test_tick_opens_once_only_in_game_without_other_screen_and_drains_clicks(self):
        self.run_case("tick")

    def test_backslash_default_remappable_localized_key_category_and_idempotent_registration(self):
        self.run_case("key")

    def test_command_registration_once_and_literal_on_each_real_dispatcher(self):
        self.run_case("commandRegistration")

    def test_command_defers_until_after_chat_closure_and_shares_disabled_configuration(self):
        self.run_case("commandChat")

    def test_command_and_key_requests_coalesce_and_never_replay_after_menu_or_disconnect(self):
        self.run_case("commandDiscard")

    def test_command_rejects_unavailable_world_client_or_configuration_without_latent_open(self):
        self.run_case("commandUnavailable")

    def test_exact_redirect_annotation_target_anchor_and_ordinal(self):
        self.run_case("anchor")


if __name__ == "__main__":
    unittest.main()
