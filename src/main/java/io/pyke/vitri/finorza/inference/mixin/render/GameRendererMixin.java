package io.pyke.vitri.finorza.inference.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.pyke.vitri.finorza.inference.util.MouseCursor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
            method = "render(FJZ)V",
            at = @At(
                    value = "INVOKE",
                    shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/client/gui/screens/Screen;render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void injectRender(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci, int i, int j, PoseStack poseStack) {
        MouseCursor.render(poseStack, this.minecraft.screen, i, j);
    }
}
