package io.pyke.vitri.finorza.inference.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import com.google.gson.annotations.SerializedName;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Option;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.TextComponent;

import io.pyke.vitri.finorza.inference.mixin.render.WindowAccessor;
import io.pyke.vitri.finorza.inference.gui.ModOptionsScreen;

public class Config {
	public static final BooleanConfigOption SYNCHRONIZE_INTEGRATED_SERVER = new BooleanConfigOption(
		"synchronizeIntegratedServer", false, (ignored, value) -> {
		Minecraft mc = Minecraft.getInstance();
		if (mc.hasSingleplayerServer()) {
			mc.level.disconnect();
			mc.clearLevel(new GenericDirtMessageScreen(new TextComponent("Saving world")));
			mc.setScreen(new ModOptionsScreen(new TitleScreen()));
		}
	});
	public static final EnumConfigOption<CursorSize> CURSOR_SIZE = new EnumConfigOption<>(
		"cursorSize", CursorSize.PX_16);
	public static final EnumConfigOption<WindowSize> WINDOW_SIZE = new EnumConfigOption<>(
		"windowSize", WindowSize.R_360, (ignored, value) -> {
		((WindowAccessor) (Object) Minecraft.getInstance().getWindow()).vitri$resize(
			value.width, value.height);
	});

	public static Option[] asOptions() {
		ArrayList<Option> options = new ArrayList<>();
		for (Field field : Config.class.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(
				field.getModifiers()) && Option.class.isAssignableFrom(field.getType())) {
				try {
					options.add((Option) field.get(null));
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return options.toArray(Option[]::new);
	}

	public enum CursorSize {
		@SerializedName("4x4") PX_4(4), @SerializedName("8x8") PX_8(8), @SerializedName("16x16") PX_16(16);

		public final int size;

		CursorSize(int size) {
			this.size = size;
		}
	}

	public enum WindowSize {
		@SerializedName("360p") R_360(640, 360), @SerializedName("720p") R_720(1280, 720), @SerializedName(
			"1080p"
		) R_1080(1920, 1080);

		public final int width;
		public final int height;

		WindowSize(int width, int height) {
			this.width = width;
			this.height = height;
		}
	}
}
