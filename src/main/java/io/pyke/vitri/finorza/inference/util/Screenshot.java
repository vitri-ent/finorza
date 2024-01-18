package io.pyke.vitri.finorza.inference.util;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

public class Screenshot { // NOT thread safe!
	public static final int RESIZE_WIDTH = 640;
	public static final int RESIZE_HEIGHT = 360;

	private final int resizeWidth, resizeHeight;
	private ByteBuffer resizeBuffer;
	private int width, height = 0;
	private ByteBuffer buffer;

	public Screenshot() {
		this(RESIZE_WIDTH, RESIZE_HEIGHT);
	}

	public Screenshot(int resizeWidth, int resizeHeight) {
		this.resizeWidth = resizeWidth;
		this.resizeHeight = resizeHeight;
	}

	public @NotNull ByteBuffer read() {
		final RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
		if (target.width != width || target.height != height) {
			this.width = target.width;
			this.height = target.height;
			this.buffer = ByteBuffer.allocateDirect(width * height * 3);
		} else {
			this.buffer.rewind();
		}

		RenderSystem.pushMatrix();
		target.blitToScreen(this.width, this.height);
		RenderSystem.popMatrix();
		GL11.glReadPixels(0, 0, this.width, this.height, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, this.buffer);

		return this.buffer;
	}

	public @NotNull ByteBuffer resize() {
		if (this.resizeBuffer == null) {
			this.resizeBuffer = ByteBuffer.allocateDirect(resizeWidth * resizeHeight * 3);
		} else {
			this.resizeBuffer.rewind();
		}

		if (this.buffer == null) {
			return this.resizeBuffer; // no data
		}

		final double wr = (double) this.width / this.resizeWidth;
		final double hr = (double) this.height / this.resizeHeight;
		for (int y = 0; y < this.resizeHeight; y++) {
			for (int x = 0; x < this.resizeWidth; x++) {
				final int srcX = Math.min((int) (x * wr), this.width - 1);
				final int srcY = Math.min(this.height - (int) (y * hr), this.height - 1); // flip Y here, since glReadPixels returns an upside down image

				final int srcIdx = (srcY * this.width + srcX) * 3;
				final int destIdx = (y * this.resizeWidth + x) * 3;

				this.resizeBuffer.put(destIdx, this.buffer.get(srcIdx));
				this.resizeBuffer.put(destIdx + 1, this.buffer.get(srcIdx + 1));
				this.resizeBuffer.put(destIdx + 2, this.buffer.get(srcIdx + 2));
			}
		}

		return this.resizeBuffer;
	}
}
