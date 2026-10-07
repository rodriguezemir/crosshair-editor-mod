package site.zvolcan.client.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import site.zvolcan.client.CrosshairEditorClient;

@Mixin(Gui.class)
public abstract class InGameHudMixin {
	@Redirect(
		method = "extractCrosshair(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
			ordinal = 0
		)
	)
	private void drawCustomCrosshair(
		GuiGraphicsExtractor context,
		RenderPipeline pipeline,
		Identifier sprite,
		int x,
		int y,
		int width,
		int height
	) {
		if (!CrosshairEditorClient.isCustomCrosshairEnabled()) {
			context.blitSprite(pipeline, sprite, x, y, width, height);
			return;
		}
		int centerX = x + width / 2;
		int centerY = y + height / 2;

		CrosshairEditorClient.getCrosshairManager().draw(context, centerX, centerY);
	}
}
