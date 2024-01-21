package io.pyke.vitri.finorza.inference.util;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.pyke.vitri.finorza.inference.config.Config;
import io.pyke.vitri.finorza.inference.config.ConfigManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

public final class MouseCursor {
    private MouseCursor() {
        throw new UnsupportedOperationException();
    }

    public static void render(PoseStack matrixStack, Screen screen, int x, int y) {
        final int size = ConfigManager.getConfig().cursorSize.size;

        RenderSystem.enableTexture();
        RenderSystem.disableLighting();
        RenderSystem.disableDepthTest();
        if (screen == null) {
            return;
        }
        RenderSystem.pushMatrix();
        bindTexture(size);
        RenderSystem.enableRescaleNormal();
        RenderSystem.enableAlphaTest();

        RenderSystem.alphaFunc(516, 0.1F);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA.value, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA.value);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        screen.blit(matrixStack, x, y, 0, 0, size, size);

        RenderSystem.disableAlphaTest();
        RenderSystem.disableRescaleNormal();
        RenderSystem.disableLighting();
        RenderSystem.popMatrix();
    }

    private static void bindTexture(int size) {
        if (size < 1 || size > 16) {
            throw new RuntimeException("Cursor size should be between 1 and 16 (requested " + size + ")");
        }
        int rounded_size = 4;
        while (rounded_size < size) {
            rounded_size <<= 1;
        }

        final TextureManager tm = Minecraft.getInstance().getTextureManager();
        final ResourceLocation texLocation = new ResourceLocation(
                "vitri",
                "textures/gui/mouse_cursor_white_" + rounded_size + "x" + rounded_size + ".png"
        );

        tm.bind(texLocation);
        tm.getTexture(texLocation).setFilter(false, false);
    }
}
