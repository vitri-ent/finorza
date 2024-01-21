package io.pyke.vitri.finorza.inference;

import java.io.IOException;

import com.mojang.blaze3d.platform.InputConstants;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import io.pyke.vitri.finorza.inference.config.ConfigManager;
import io.pyke.vitri.finorza.inference.rpc.RemoteControlService;

@Environment(EnvType.CLIENT)
public class FinorzaInference implements ClientModInitializer {
    public static final String MOD_ID = "finorza-inference";
    public static final Logger LOGGER = LogManager.getLogger("Finorza");
    public static Server remoteControlServer;

    public static KeyMapping agentControlToggle = KeyBindingHelper.registerKeyBinding(new KeyMapping(
        "finorza-inference.keybind.toggleAgentControl",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_PAGE_DOWN,
        "finorza-inference.display"
    ));

    public static Server createControlServer(int port) {
        return ServerBuilder.forPort(port)
            .addService(new RemoteControlService())
            .addService(ProtoReflectionService.newInstance())
            .build();
    }

    @Override
    public void onInitializeClient() {
        try {
            int port = ConfigManager.getConfig().controlPort;
            remoteControlServer = createControlServer(port);
            remoteControlServer.start();
            LOGGER.info("RPC started on port " + port);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start gRPC server", e);
        }
    }
}
