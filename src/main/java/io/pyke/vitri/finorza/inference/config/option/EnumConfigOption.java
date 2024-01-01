package io.pyke.vitri.finorza.inference.config.option;

import io.pyke.vitri.finorza.inference.FinorzaInference;
import io.pyke.vitri.finorza.inference.config.ConfigOptionStorage;
import net.minecraft.client.CycleOption;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.function.BiConsumer;

public class EnumConfigOption<E extends Enum<E>> extends CycleOption {
    private final String key;
    private final String translationKey;
    private final Class<E> enumClass;
    private final E defaultValue;

    public EnumConfigOption(@NotNull String key, @NotNull E defaultValue) {
        this(key, defaultValue, (options, e) -> {});
    }

    public EnumConfigOption(@NotNull String key, @NotNull E defaultValue, @NotNull BiConsumer<Options, E> callback) {
        super(key, (options, amount) -> {
            final Class<E> clz = defaultValue.getDeclaringClass();
            ConfigOptionStorage.cycleEnum(key, clz);

            final E currentValue = ConfigOptionStorage.getEnum(key, clz);
            callback.accept(options, currentValue);
        }, null);

        this.key = key;
        this.translationKey = FinorzaInference.MOD_ID + ".option." + key;
        this.enumClass = defaultValue.getDeclaringClass();
        this.defaultValue = defaultValue;

        ConfigOptionStorage.setEnum(key, defaultValue);
    }

    public @NotNull String getKey() {
        return this.key;
    }

    public @NotNull E getValue() {
        return ConfigOptionStorage.getEnum(this.key, this.enumClass);
    }

    public void setValue(@NotNull E value) {
        ConfigOptionStorage.setEnum(this.key, value);
    }

    public void cycleValue() {
        ConfigOptionStorage.cycleEnum(this.key, this.enumClass);
    }

    public void cycleValue(int amount) {
        ConfigOptionStorage.cycleEnum(this.key, this.enumClass, amount);
    }

    public @NotNull E getDefaultValue() {
        return defaultValue;
    }

    @Override
    public @NotNull Component getMessage(@NotNull Options options) {
        return new TranslatableComponent(
                this.translationKey,
                new TranslatableComponent(
                        this.translationKey + "." + ConfigOptionStorage.getEnum(this.key, this.enumClass).name().toLowerCase(Locale.ROOT)
                )
        );
    }
}
