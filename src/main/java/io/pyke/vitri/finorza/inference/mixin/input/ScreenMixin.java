package io.pyke.vitri.finorza.inference.mixin.input;

import io.pyke.vitri.finorza.inference.api.KeyboardHandlerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Screen.class)
public class ScreenMixin {
    @Overwrite // need this override for container GUIs
    public static boolean hasShiftDown() {
        final KeyboardHandlerAccessor handler = ((KeyboardHandlerAccessor) Minecraft.getInstance().keyboardHandler);
        return handler.vitri$isKeyPressed(340) || handler.vitri$isKeyPressed(344);
    }
}
