package site.zvolcan.client.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import site.zvolcan.client.CrosshairEditorClient;

@Mixin(Gui.class)
public abstract class InGameHudMixin {
	@Redirect(
		method = "renderCrosshair(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
			ordinal = 0
		)
	)
	private void drawCustomCrosshair(
		GuiGraphics context,
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
