package io.pyke.vitri.finorza.inference.config;

import java.util.function.BiConsumer;

import net.minecraft.client.BooleanOption;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import io.pyke.vitri.finorza.inference.FinorzaInference;

public class BooleanConfigOption extends BooleanOption {
	private final String key;
	private final String translationKey;
	private final boolean defaultValue;

	public BooleanConfigOption(String key, boolean defaultValue) {
		super(key, ignored -> ConfigOptionStorage.getBoolean(key),
		      (ignored, value) -> ConfigOptionStorage.setBoolean(key, value)
		);
		ConfigOptionStorage.setBoolean(key, defaultValue);
		this.key = key;
		this.translationKey = FinorzaInference.MOD_ID + ".option." + key;
		this.defaultValue = defaultValue;
	}

	public BooleanConfigOption(String key, boolean defaultValue, BiConsumer<Options, Boolean> setter) {
		super(key, ignored -> ConfigOptionStorage.getBoolean(key), (ignored, value) -> {
			ConfigOptionStorage.setBoolean(key, value);
			setter.accept(ignored, value);
		});
		ConfigOptionStorage.setBoolean(key, defaultValue);
		this.key = key;
		this.translationKey = FinorzaInference.MOD_ID + ".option." + key;
		this.defaultValue = defaultValue;
	}

	public String getKey() {
		return key;
	}

	public boolean getValue() {
		return ConfigOptionStorage.getBoolean(key);
	}

	public void setValue(boolean value) {
		ConfigOptionStorage.setBoolean(key, value);
	}

	public void toggleValue() {
		ConfigOptionStorage.toggleBoolean(key);
	}

	@Override
	public Component getMessage(Options options) {
		return new TranslatableComponent(
			translationKey, new TranslatableComponent(translationKey + "." + ConfigOptionStorage.getBoolean(key)));
	}

	public boolean getDefaultValue() {
		return defaultValue;
	}
}
