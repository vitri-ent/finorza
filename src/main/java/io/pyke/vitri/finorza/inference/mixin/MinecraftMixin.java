package io.pyke.vitri.finorza.inference.mixin;

import java.util.OptionalInt;
import java.util.function.Function;

import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Option;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.VirtualScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import io.pyke.vitri.finorza.inference.config.Config;
import io.pyke.vitri.finorza.inference.config.ConfigManager;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    private IntegratedServer singleplayerServer;

    @Shadow
    @Final
    private Window window;

    @Shadow
    public ClientLevel level;

    @Overwrite // uncap frame rate limit if synchronizeIntegratedServer is enabled
    private int getFramerateLimit() {
        if (this.level == null) {
            return 60;
        } else if (ConfigManager.getConfig().synchronizeIntegratedServer) {
            return (int)Option.FRAMERATE_LIMIT.getMaxValue();
        }

        return this.window.getFramerateLimit();
    }

    @Redirect(
        method = "<init>(Lnet/minecraft/client/main/GameConfig;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/VirtualScreen;newWindow(Lcom/mojang/blaze3d/platform/DisplayData;Ljava/lang/String;Ljava/lang/String;)Lcom/mojang/blaze3d/platform/Window;"
        )
    )
    private Window redirectNewWindow(VirtualScreen instance, DisplayData screenSize, String videoModeName, String title) {
        Config.WindowSize windowSize = ConfigManager.getConfig().windowSize;
        return instance.newWindow(new DisplayData(windowSize.width, windowSize.height, OptionalInt.empty(), OptionalInt.empty(), false), videoModeName, title);
    }

    @Redirect(
            method = "doLoadLevel(Ljava/lang/String;Lnet/minecraft/core/RegistryAccess$RegistryHolder;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/Minecraft$ExperimentalDialogType;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;spin(Ljava/util/function/Function;)Lnet/minecraft/server/MinecraftServer;"
            )
    )
    @SuppressWarnings("unchecked")
    private <S extends MinecraftServer> S redirectCreateIntegratedServer(Function<Thread, S> threadFunction) {
        if (ConfigManager.getConfig().synchronizeIntegratedServer) {
            final IntegratedServer server = (IntegratedServer) threadFunction.apply(Thread.currentThread());
            server.initServer();
            server.tickServer(server::haveTime);
            server.mayHaveDelayedTasks = true;
            server.waitUntilNextTick();
            server.isReady = true;

            return (S) server;
        }

        return MinecraftServer.spin(threadFunction);
    }

    @Inject(
            method = "runTick(Z)V",
            at = @At(value = "INVOKE", shift = At.Shift.BEFORE, target = "Lnet/minecraft/client/Minecraft;tick()V"),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void injectTick(boolean renderLevel, CallbackInfo ci, long l, int i, int j) {
        // tick server if synchronizeIntegratedServer is enabled
        if (this.singleplayerServer != null && ConfigManager.getConfig().synchronizeIntegratedServer) {
            this.singleplayerServer.tickServer(() -> false);
            this.singleplayerServer.mayHaveDelayedTasks = true;
            while (this.singleplayerServer.pollTask());
            for (final ServerLevel world : this.singleplayerServer.getAllLevels()) {
                while (world.getChunkSource().pollTask());
            }
            this.singleplayerServer.isReady = true;
        }
    }

    @Redirect(
            method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/server/IntegratedServer;isShutdown()Z")
    )
    private boolean redirectIsShutdown(IntegratedServer instance) {
        return ConfigManager.getConfig().synchronizeIntegratedServer || instance.isShutdown();
    }
}
