package io.pyke.vitri.finorza.inference.client;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.mojang.blaze3d.pipeline.RenderCall;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import io.pyke.vitri.finorza.inference.api.Producer;

public class AgentControl {
	public static final Minecraft minecraft = Minecraft.getInstance();
	public static final ByteBuffer frameResizeBuffer = ByteBuffer.allocateDirect(
		EnvironmentRecorder.ML_WIDTH * EnvironmentRecorder.ML_HEIGHT * 3);
	private static boolean humanControl = true;

	public static void dispatchMainThread(RenderCall call) {
		RenderSystem.recordRenderCall(call);
	}

	public static void disconnect() {
		if (minecraft.level != null) {
			minecraft.level.disconnect();
			if (minecraft.isLocalServer()) {
				minecraft.clearLevel(new GenericDirtMessageScreen(new TranslatableComponent("menu.savingLevel")));
			} else {
				minecraft.clearLevel();
			}
		}
	}

	public static void loadLevel(String levelId) {
		AgentControl.disconnect();

		minecraft.forceSetScreen(new GenericDirtMessageScreen(new TranslatableComponent("selectWorld.data_read")));
		minecraft.loadLevel(levelId);
	}

	public static boolean hasAgentControl() {
		return !humanControl;
	}

	public static void setHumanControl(boolean enable) {
		SystemToast.addOrUpdate(minecraft.getToasts(), SystemToast.SystemToastIds.TUTORIAL_HINT,
		                        new TextComponent("Finorza"),
		                        new TextComponent(enable ? "Disabled agent controls" : "Control relinquished")
		);
		humanControl = enable;
	}

	public static <T> void dispatchAndAwait(Producer<T> producer, Consumer<T> consumer) {
		final AtomicReference<T> ref = new AtomicReference<>();
		AgentControl.dispatchMainThread(() -> {
			ref.set(producer.produce());
		});
		while (true) {
			T t = ref.get();
			if (t != null) {
				consumer.accept(t);
				break;
			}
		}
	}

	public static ByteBuffer resizeFrame(EnvironmentRecorder.Frame frame) {
		frameResizeBuffer.rewind();

		// this is actually an order of magnitude faster than using OpenCV Imgproc.resize, not sure why
		double wr = (double) frame.width / EnvironmentRecorder.ML_WIDTH;
		double hr = (double) frame.height / EnvironmentRecorder.ML_HEIGHT;
		for (int y = 0; y < EnvironmentRecorder.ML_HEIGHT; y++) {
			for (int x = 0; x < EnvironmentRecorder.ML_WIDTH; x++) {
				int srcX = (int) (x * wr);
				// flip Y here since glReadPixels returns an upside down image
				int srcY = frame.height - (int) (y * hr);

				srcX = Math.min(srcX, frame.width - 1);
				srcY = Math.min(srcY, frame.height - 1);

				int srcIdx = (srcY * frame.width + srcX) * 3;
				int destIdx = (y * EnvironmentRecorder.ML_WIDTH + x) * 3;

				frameResizeBuffer.put(destIdx, frame.pixels.get(srcIdx));
				frameResizeBuffer.put(destIdx + 1, frame.pixels.get(srcIdx + 1));
				frameResizeBuffer.put(destIdx + 2, frame.pixels.get(srcIdx + 2));
			}
		}

		return frameResizeBuffer;
	}
}
