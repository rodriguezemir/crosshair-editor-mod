"""HUD wiring checks; initializer drawing geometry is exercised by the Java harness."""

import unittest
from pathlib import Path


MIXIN = Path(__file__).parents[1] / "src/client/java/site/zvolcan/client/mixin/InGameHudMixin.java"


class CrosshairInversionTest(unittest.TestCase):
    def test_handler_draws_selected_crosshair_through_shared_client_manager(self):
        source = MIXIN.read_text()
        handler = source.split("private void drawCustomCrosshair(", 1)[1]
        self.assertEqual(1, handler.count("context.blitSprite(pipeline, sprite, x, y, width, height);"),
                         "disabled branch must forward the original vanilla sprite arguments")
        self.assertNotIn("context.fill(", handler, "HUD must not bypass selected crosshair")
        self.assertEqual(
            1,
            handler.count("CrosshairEditorClient.getCrosshairManager().draw(context, centerX, centerY);"),
            "HUD must draw exactly once through the shared client manager",
        )
        self.assertIn("import site.zvolcan.client.CrosshairEditorClient;", source)

    def test_vanilla_redirect_target_and_anchor_are_preserved(self):
        source = MIXIN.read_text()
        self.assertIn('method = "extractCrosshair(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"', source)
        self.assertIn('target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"', source)
        self.assertIn("ordinal = 0", source)
        self.assertIn("int centerX = x + width / 2;", source)
        self.assertIn("int centerY = y + height / 2;", source)


if __name__ == "__main__":
    unittest.main()
