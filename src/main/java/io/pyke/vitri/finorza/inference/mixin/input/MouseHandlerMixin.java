package io.pyke.vitri.finorza.inference.mixin.input;

import io.pyke.vitri.finorza.inference.client.AgentControl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin implements MouseHandlerAccessor {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private boolean useOriginalMethod = false;
    @Unique
    private Set<Integer> buttonsPressed = new HashSet<>();

    @Shadow
    public abstract double xpos();

    @Shadow
    public abstract double ypos();

    @Shadow(prefix = "original$")
    protected abstract void original$onPress(long handle, int button, int action, int mods);

    @Shadow(prefix = "original$")
    protected abstract void original$onScroll(long handle, double xoffset, double yoffset);

    @Shadow(prefix = "original$")
    protected abstract void original$onMove(long handle, double xpos, double ypos);

    @ModifyVariable(method = "onPress(JIII)V", at = @At(value = "HEAD"), argsOnly = true)
    private long injectOnPressHandle(long handle) {
        return this.minecraft.getWindow().getWindow();
    }

    @Inject(method = "onPress(JIII)V", at = @At(value = "HEAD"), cancellable = true)
    private void injectOnPress(long handle, int button, int action, int mods, CallbackInfo ci) {
        if (action == 0) {
            buttonsPressed.remove(button);
        } else {
            buttonsPressed.add(button);
        }
        if (!useOriginalMethod && AgentControl.hasAgentControl()) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "onScroll(JDD)V", at = @At(value = "HEAD"), argsOnly = true)
    private long injectOnScrollHandle(long handle) {
        return this.minecraft.getWindow().getWindow();
    }

    @Inject(method = "onScroll(JDD)V", at = @At(value = "HEAD"), cancellable = true)
    private void injectOnScroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {
        if (!useOriginalMethod && AgentControl.hasAgentControl()) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "onMove(JDD)V", at = @At(value = "HEAD"), argsOnly = true)
    private long injectOnMoveHandle(long handle) {
        return this.minecraft.getWindow().getWindow();
    }

    @Inject(method = "onMove(JDD)V", at = @At(value = "HEAD"), cancellable = true)
    private void injectOnMove(long handle, double xpos, double ypos, CallbackInfo ci) {
        if (!useOriginalMethod && AgentControl.hasAgentControl()) {
            ci.cancel();
        }
    }

    @Redirect(method = "turnPlayer()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;isMouseGrabbed()Z"))
    private boolean injectIsMouseGrabbed(MouseHandler instance) {
        return true;
    }

    @Redirect(method = "turnPlayer()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isWindowActive()Z"))
    private boolean injectIsWindowActiveTurnPlayer(Minecraft minecraft) {
        return true;
    }

    @ModifyArg(method = "turnPlayer()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/SmoothDouble;getNewDeltaValue(DD)D"), index = 1)
    private double injectTurnPlayerSmoothCameraDouble(double sensitivity) {
        return sensitivity * 35.;
    }

    @Redirect(method = "onMove(JDD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isWindowActive()Z"))
    private boolean injectIsWindowActiveOnMove(Minecraft minecraft) {
        return true;
    }

    @Override
    public void vitri$onMove(double xpos, double ypos) {
        try {
            useOriginalMethod = true;
            double scaleFactor = 1.0;
            if (this.minecraft.screen != null) {
                double retinaFactor = (double) minecraft.getMainRenderTarget().viewWidth / minecraft.getWindow().getWidth();
                scaleFactor = minecraft.getWindow().getGuiScale() / retinaFactor;
            }
            double newX = this.xpos() + (xpos * (2400. / 360)) * scaleFactor;
            double newY = this.ypos() + (ypos * (2400. / 360)) * scaleFactor;
            if (this.minecraft.screen != null) {
                newX = Math.min(Math.max(newX, 0), this.minecraft.getWindow().getWidth());
                newY = Math.min(Math.max(newY, 0), this.minecraft.getWindow().getHeight());
            }
            this.original$onMove(this.minecraft.getWindow().getWindow(), newX, newY);
        } finally {
            useOriginalMethod = false;
        }
    }

    @Override
    public void vitri$onPress(int button, int action, int mods) {
        try {
            useOriginalMethod = true;
            this.original$onPress(this.minecraft.getWindow().getWindow(), button, action, mods);
        } finally {
            useOriginalMethod = false;
        }
    }

    /**
     * @author decahedron
     * @reason Prevent `cursorEntered` from setting `ignoreFirstMove = true`
     */
    @Overwrite
    public void cursorEntered() {
        // null
    }

    @Redirect(method = "grabMouse()V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MouseHandler;ignoreFirstMove:Z", opcode = Opcodes.PUTFIELD))
    private void injectGrabMouseIgnoreFirstMove(MouseHandler instance, boolean value) {
        // null
    }

    @Redirect(method = "onMove(JDD)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MouseHandler;ignoreFirstMove:Z", opcode = Opcodes.GETFIELD))
    private boolean injectOnMoveIgnoreFirstMove(MouseHandler instance) {
        return false;
    }
}
