package site.zvolcan.client.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Hud.class)
public abstract class InGameHudMixin {
	@Redirect(
		method = "extractCrosshair(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
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
		int centerX = x + width / 2;
		int centerY = y + height / 2;

		context.fill(RenderPipelines.GUI_INVERT, centerX - 4, centerY, centerX + 5, centerY + 1, 0xFFFFFFFF);
		context.fill(RenderPipelines.GUI_INVERT, centerX, centerY - 4, centerX + 1, centerY, 0xFFFFFFFF);
		context.fill(RenderPipelines.GUI_INVERT, centerX, centerY + 1, centerX + 1, centerY + 5, 0xFFFFFFFF);
	}
}
