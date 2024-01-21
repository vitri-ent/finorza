package io.pyke.vitri.finorza.inference.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.NotNull;

public class Controller {
    private boolean humanControl = true;

    private Controller() {}

    public static @NotNull Controller getInstance() {
        class Instance {
            static final Controller INSTANCE = new Controller();
        }
        return Instance.INSTANCE;
    }

    public boolean hasAgentControl() {
        return !this.humanControl;
    }

    public void toggleHumanControl() {
        this.setHumanControl(!this.humanControl);
    }

    public boolean shouldBlockControls() {
        return this.hasAgentControl() && ScreenUtil.isAgentControllableScreen(Minecraft.getInstance().screen);
    }

    public void setHumanControl(boolean enabled) {
        SystemToast.addOrUpdate(
            Minecraft.getInstance().getToasts(),
            SystemToast.SystemToastIds.TUTORIAL_HINT,
            new TranslatableComponent("finorza-inference.display").withStyle(s -> s.withBold(true)),
            enabled
                ? new TranslatableComponent("finorza-inference.alert.control.human").withStyle(s -> s.withColor(ChatFormatting.GREEN))
                : new TranslatableComponent("finorza-inference.alert.control.agent").withStyle(s -> s.withColor(ChatFormatting.RED))
        );
        this.humanControl = enabled;
    }
}
