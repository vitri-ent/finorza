package io.pyke.vitri.finorza.inference.config.option;

import io.pyke.vitri.finorza.inference.FinorzaInference;
import io.pyke.vitri.finorza.inference.config.ConfigOptionStorage;
import net.minecraft.client.BooleanOption;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public class BooleanConfigOption extends BooleanOption {
    private final String key;
    private final String translationKey;
    private final boolean defaultValue;

    public BooleanConfigOption(@NotNull String key, boolean defaultValue) {
        this(key, defaultValue, (options, value) -> {});
    }

    public BooleanConfigOption(@NotNull String key, boolean defaultValue, @NotNull BiConsumer<Options, Boolean> callback) {
        super(key, options -> ConfigOptionStorage.getBoolean(key), (options, value) -> {
            ConfigOptionStorage.setBoolean(key, value);
            callback.accept(options, value);
        });

        this.key = key;
        this.translationKey = FinorzaInference.MOD_ID + ".option." + key;
        this.defaultValue = defaultValue;

        ConfigOptionStorage.setBoolean(key, defaultValue);
    }

    public @NotNull String getKey() {
        return this.key;
    }

    public boolean getValue() {
        return ConfigOptionStorage.getBoolean(this.key);
    }

    public void setValue(boolean value) {
        ConfigOptionStorage.setBoolean(this.key, value);
    }

    public void toggleValue() {
        ConfigOptionStorage.toggleBoolean(this.key);
    }

    public boolean getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public @NotNull Component getMessage(@NotNull Options options) {
        return new TranslatableComponent(
                this.translationKey,
                new TranslatableComponent(this.translationKey + "." + ConfigOptionStorage.getBoolean(key))
        );
    }
}
