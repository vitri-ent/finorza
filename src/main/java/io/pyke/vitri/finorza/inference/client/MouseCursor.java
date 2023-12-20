package io.pyke.vitri.finorza.inference.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

import io.pyke.vitri.finorza.inference.config.Config;

public class MouseCursor {
	private static final MouseCursor instance = new MouseCursor();

	public static MouseCursor getInstance() {
		return instance;
	}

	public void render(PoseStack matrixStack, Screen screen, int x, int y) {
		int size = Config.CURSOR_SIZE.getValue().size;
		GlStateManager._enableTexture();
		GlStateManager._disableLighting();
		GlStateManager._disableDepthTest();
		if (screen == null) {
			return;
		}
		GlStateManager._pushMatrix();
		bindTexture(size);
		GlStateManager._enableRescaleNormal();
		GlStateManager._enableAlphaTest();

		GlStateManager._alphaFunc(516, 0.1F);
		GlStateManager._enableBlend();
		GlStateManager._blendFunc(
			GlStateManager.SourceFactor.SRC_ALPHA.value, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA.value);
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		screen.blit(matrixStack, x, y, 0, 0, size, size);

		GlStateManager._disableAlphaTest();
		GlStateManager._disableRescaleNormal();
		GlStateManager._disableLighting();
		GlStateManager._popMatrix();
	}


	private static void bindTexture(int size) {
		if (size < 1 || size > 16) {
			throw new RuntimeException("Cursor size should be between 1 and 16 (requested " + size + ")");
		}
		int rounded_size = 4;
		while (rounded_size < size) {
			rounded_size <<= 1;
		}

		TextureManager tm = Minecraft.getInstance().getTextureManager();
		ResourceLocation texLocation = new ResourceLocation(
			"vitri", "textures/gui/mouse_cursor_white_" + rounded_size + "x" + rounded_size + ".png");
		tm.bind(texLocation);
		tm.getTexture(texLocation).setFilter(false, false);
	}
}
