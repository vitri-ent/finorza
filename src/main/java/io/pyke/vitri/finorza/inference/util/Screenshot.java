package io.pyke.vitri.finorza.inference.util;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

public class Screenshot { // NOT thread safe!
	private int resizeWidth, resizeHeight;
	private ByteBuffer resizeBuffer;
	private int width, height = 0;
	private ByteBuffer buffer;

	public Screenshot(int resizeWidth, int resizeHeight) {
		this.resizeWidth = resizeWidth;
		this.resizeHeight = resizeHeight;
	}

	public void setResizeSize(int resizeWidth, int resizeHeight) {
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

		GL11.glReadPixels(0, 0, this.width, this.height, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, this.buffer);

		return this.buffer;
	}

	private static float lerp(float s, float e, float t) {
		return s + (e - s) * t;
	}

	public @NotNull ByteBuffer resize(InterpolationMethod interpolationMethod) {
		if (this.resizeBuffer == null || this.resizeBuffer.capacity() != this.resizeWidth * this.resizeHeight * 3) {
			this.resizeBuffer = ByteBuffer.allocateDirect(resizeWidth * resizeHeight * 3);
		} else {
			this.resizeBuffer.rewind();
		}

		if (this.buffer == null) {
			return this.resizeBuffer; // no data
		}

		if (this.width == this.resizeWidth && this.height == this.resizeHeight) {
			// Only do the Y flip
			for (int y = 0; y < this.height - 1; y++) {
				this.resizeBuffer.put((this.height - 1 - y) * this.width * 3, this.buffer, y * this.width * 3, this.width * 3);
			}
			return this.resizeBuffer;
		}

		switch (interpolationMethod) {
			case LINEAR -> {
				throw new UnsupportedOperationException();
				// TODO: unfuck this
//				for (int x = 0; x < this.resizeWidth; x++) {
//					for (int y = 0; y < this.resizeHeight; y++) {
//						float gx = ((float) x + 0.5f) / this.resizeWidth * (this.width - 1);
//						float gy = ((float) y + 0.5f) / this.resizeHeight * (this.height - 1);
//						int gxi = Math.round(gx);
//						int gyi = Math.round(gy);
//						float tx = gx - gxi;
//						float ty = gy - gyi;
//
//						for (int c = 0; c < 3; c++) {
//							float c00 = (float)this.buffer.get(((gxi + (gyi * this.width)) * 3) + c);
//							float c10 = (float)this.buffer.get(((gxi + 1 + (gyi * this.width)) * 3) + c);
//							float c01 = (float)this.buffer.get(((gxi + (Math.min(this.height - 1, (gyi + 1)) * this.width)) * 3) + c);
//							float c11 = (float)this.buffer.get(((gxi + 1 + (Math.min(this.height - 1, (gyi + 1)) * this.width)) * 3) + c);
//							this.resizeBuffer.put(
//								((x + ((this.resizeHeight - 1 - y) * this.resizeWidth)) * 3) + c,
//								(byte)lerp(lerp(c00, c10, tx), lerp(c01, c11, tx), ty)
//							);
//						}
//					}
//				}
			}
			case NEAREST_NEIGHBOR -> {
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
			}
		}

		return this.resizeBuffer;
	}

	public enum InterpolationMethod {
		LINEAR,
		NEAREST_NEIGHBOR
	}
}
