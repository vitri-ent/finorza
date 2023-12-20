package io.pyke.vitri.finorza.inference.config;

import java.util.Locale;
import java.util.function.BiConsumer;

import net.minecraft.client.CycleOption;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import io.pyke.vitri.finorza.inference.FinorzaInference;

public class EnumConfigOption<E extends Enum<E>> extends CycleOption {
	private final String key;
	private final String translationKey;
	private final Class<E> enumClass;
	private final E defaultValue;

	public EnumConfigOption(String key, E defaultValue) {
		super(key, (ignored, amount) -> ConfigOptionStorage.cycleEnum(key, defaultValue.getDeclaringClass()), null);
		ConfigOptionStorage.setEnum(key, defaultValue);
		this.key = key;
		this.translationKey = FinorzaInference.MOD_ID + ".option." + key;
		this.enumClass = defaultValue.getDeclaringClass();
		this.defaultValue = defaultValue;
	}

	public EnumConfigOption(String key, E defaultValue, BiConsumer<Options, E> setter) {
		super(key, (ignored, amount) -> {
			Class<E> clz = defaultValue.getDeclaringClass();
			ConfigOptionStorage.cycleEnum(key, clz);
			E currentValue = ConfigOptionStorage.getEnum(key, clz);
			setter.accept(ignored, currentValue);
		}, null);
		ConfigOptionStorage.setEnum(key, defaultValue);
		this.key = key;
		this.translationKey = FinorzaInference.MOD_ID + ".option." + key;
		this.enumClass = defaultValue.getDeclaringClass();
		this.defaultValue = defaultValue;
	}

	public String getKey() {
		return key;
	}


	public E getValue() {
		return ConfigOptionStorage.getEnum(key, enumClass);
	}

	public void setValue(E value) {
		ConfigOptionStorage.setEnum(key, value);
	}

	public void cycleValue() {
		ConfigOptionStorage.cycleEnum(key, enumClass);
	}

	public void cycleValue(int amount) {
		ConfigOptionStorage.cycleEnum(key, enumClass, amount);
	}

	@Override
	public Component getMessage(Options options) {
		return new TranslatableComponent(translationKey, new TranslatableComponent(
			translationKey + "." + ConfigOptionStorage.getEnum(key, enumClass).name().toLowerCase(Locale.ROOT)));
	}

	public E getDefaultValue() {
		return defaultValue;
	}
}
