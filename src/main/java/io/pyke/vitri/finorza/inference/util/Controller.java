package io.pyke.vitri.finorza.inference.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.TextComponent;
import org.jetbrains.annotations.NotNull;

public class Controller {
    private boolean humanControl = true;

    private Controller() {
    }

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

    public void setHumanControl(boolean enabled) {
        SystemToast.addOrUpdate(
                Minecraft.getInstance().getToasts(),
                SystemToast.SystemToastIds.TUTORIAL_HINT,
                new TextComponent("Finorza"),
                new TextComponent(enabled ? "Disabled agent controls" : "Control relinquished")
        );
        this.humanControl = enabled;
    }
}
