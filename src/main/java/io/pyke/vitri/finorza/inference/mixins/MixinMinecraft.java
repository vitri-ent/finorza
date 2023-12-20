package io.pyke.vitri.finorza.inference.mixins;

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
public class MixinMinecraft {
	@Shadow
	private IntegratedServer singleplayerServer;
	@Shadow
	@Final
	private Window window;
	@Shadow
	public ClientLevel level;

	//	@Redirect(
	//			method = "runTick(Z)V",
	//			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;turnPlayer()V")
	//	)
	//	private void redirectTurnPlayer(MouseHandler instance) {
	//		// mouse events are handled elsewhere; disable this so it doesn't interfere with env control
	//	}

	/**
	 * @author decahedron
	 * @reason Override frame rate limit to 20 (server ticks per second) if `synchronizeIntegratedServer` is enabled.
	 */
	@Overwrite
	private int getFramerateLimit() {
		if (this.level == null) {
			return 60;
		} else if (Config.SYNCHRONIZE_INTEGRATED_SERVER.getValue()) {
			return 20;
		} else {
			return this.window.getFramerateLimit();
		}
	}

	@Redirect(
		method = "doLoadLevel(Ljava/lang/String;Lnet/minecraft/core/RegistryAccess$RegistryHolder;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/Minecraft$ExperimentalDialogType;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/MinecraftServer;spin(Ljava/util/function/Function;)Lnet/minecraft/server/MinecraftServer;"
		)
	)
	private <S extends MinecraftServer> S redirectCreateIntegratedServer(Function<Thread, S> threadFunction) {
		if (Config.SYNCHRONIZE_INTEGRATED_SERVER.getValue()) {
			IntegratedServer server = (IntegratedServer) threadFunction.apply(Thread.currentThread());
			server.initServer();
			server.tickServer(server::haveTime);
			server.mayHaveDelayedTasks = true;
			server.waitUntilNextTick();
			server.isReady = true;
			return (S) server;
		} else {
			return MinecraftServer.spin(threadFunction);
		}
	}

	@Inject(
		method = "runTick(Z)V", at = @At(
		value = "INVOKE", shift = At.Shift.BEFORE, target = "Lnet/minecraft/client/Minecraft;tick()V"
	), locals = LocalCapture.CAPTURE_FAILHARD
	)
	private void injectTick(
		boolean renderLevel, CallbackInfo ci, long l, int i, int j
	) {
		// Run server tick routines if `synchronizeIntegratedServer` is enabled.
		if (Config.SYNCHRONIZE_INTEGRATED_SERVER.getValue() && this.singleplayerServer != null) {
			this.singleplayerServer.tickServer(() -> false);
			this.singleplayerServer.mayHaveDelayedTasks = true;
			while (this.singleplayerServer.pollTask())
				;
			for (ServerLevel world : this.singleplayerServer.getAllLevels()) {
				while (world.getChunkSource().pollTask())
					;
			}
			this.singleplayerServer.isReady = true;
		}
	}

	@Redirect(
		method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/server/IntegratedServer;isShutdown()Z")
	)
	private boolean redirectIsShutdown(IntegratedServer instance) {
		if (Config.SYNCHRONIZE_INTEGRATED_SERVER.getValue()) {
			return true;
		} else {
			return instance.isShutdown();
		}
	}
}
