package io.pyke.vitri.finorza.inference;

import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.pyke.vitri.finorza.inference.rpc.RemoteControlService;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.IOException;

@Environment(EnvType.CLIENT)
public class FinorzaInference implements ClientModInitializer {
    public static final int PORT = 24351;
    public static final String MOD_ID = "finorza-inference";

    @Override
    public void onInitializeClient() {
        try {
            ServerBuilder.forPort(PORT)
                    .addService(new RemoteControlService())
                    .addService(ProtoReflectionService.newInstance())
                    .build()
                    .start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start gRPC server", e);
        }
    }
}
