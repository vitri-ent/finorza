package io.pyke.vitri.finorza.inference;

import com.google.common.collect.ImmutableMap;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.pyke.vitri.finorza.inference.gui.ModOptionsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.OptionsScreen;

import java.util.Map;

public class FinorzaInferenceConfigMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ModOptionsScreen::new;
    }

    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        return ImmutableMap.of("minecraft", parent -> new OptionsScreen(parent, Minecraft.getInstance().options));
    }
}
