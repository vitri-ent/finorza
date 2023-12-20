package io.pyke.vitri.finorza.inference.client;

import java.nio.ByteBuffer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.opengl.GL11;

public class EnvironmentRecorder {
	public static final int ML_WIDTH = 640;
	public static final int ML_HEIGHT = 360;

	private static EnvironmentRecorder instance = null;
	private int width = 0;
	private int height = 0;
	private ByteBuffer framePixels;

	protected Minecraft minecraft = Minecraft.getInstance();

	public static EnvironmentRecorder getInstance() {
		if (instance == null) {
			instance = new EnvironmentRecorder();
		}
		return instance;
	}

	public Frame getFrame() {
		LocalPlayer player = minecraft.player;
		if (player == null || !player.isAlive()) {
			return null;
		}

		RenderTarget target = minecraft.getMainRenderTarget();
		if (target.width != width || target.height != height) {
			this.width = target.width;
			this.height = target.height;
			this.framePixels = ByteBuffer.allocateDirect(width * height * 3);
		}

		this.framePixels.rewind();

		RenderSystem.pushMatrix();
		target.blitToScreen(width, height);
		RenderSystem.popMatrix();
		GL11.glReadPixels(0, 0, width, height, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, this.framePixels);

		return new Frame(width, height, this.framePixels);
	}

	public static class Frame {
		public int width;
		public int height;
		public ByteBuffer pixels;

		Frame(int width, int height, ByteBuffer pixels) {
			this.width = width;
			this.height = height;
			this.pixels = pixels;
		}
	}
}
