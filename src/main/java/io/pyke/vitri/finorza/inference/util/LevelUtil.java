package io.pyke.vitri.finorza.inference.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.Nullable;

public final class LevelUtil {
    private LevelUtil() {
        throw new UnsupportedOperationException();
    }

    public static void load(@Nullable String id) {
        final Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level != null) {
            minecraft.level.disconnect();
            if (minecraft.isLocalServer()) {
                minecraft.clearLevel(new GenericDirtMessageScreen(new TranslatableComponent("menu.savingLevel")));
            } else {
                minecraft.clearLevel();
            }
        }
        if (id != null) {
            minecraft.forceSetScreen(new GenericDirtMessageScreen(new TranslatableComponent("selectWorld.data_read")));
            minecraft.loadLevel(id);
        }
    }
}
