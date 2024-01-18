package io.pyke.vitri.finorza.inference.mixin;

import java.util.function.Function;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
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

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    private IntegratedServer singleplayerServer;

    @Shadow
    @Final
    private Window window;

    @Shadow
    public ClientLevel level;

    @Overwrite // override frame rate limit to 20 (server ticks per second) if SYNCHRONIZE_INTEGRATED_SERVER is enabled
    private int getFramerateLimit() {
        if (this.level == null) {
            return 60;
        } else if (Config.SYNCHRONIZE_INTEGRATED_SERVER.getValue()) {
            return 20;
        }

        return this.window.getFramerateLimit();
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
        if (Config.SYNCHRONIZE_INTEGRATED_SERVER.getValue()) {
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
        // tick server if SYNCHRONIZE_INTEGRATED_SERVER is enabled
        if (this.singleplayerServer != null && Config.SYNCHRONIZE_INTEGRATED_SERVER.getValue()) {
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
        return Config.SYNCHRONIZE_INTEGRATED_SERVER.getValue() || instance.isShutdown();
    }
}
