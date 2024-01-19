package io.pyke.vitri.finorza.inference.rpc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.mojang.blaze3d.systems.RenderSystem;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelSummary;
import org.jetbrains.annotations.NotNull;

import io.pyke.vitri.finorza.inference.api.KeyboardHandlerAccessor;
import io.pyke.vitri.finorza.inference.api.MouseHandlerAccessor;
import io.pyke.vitri.finorza.inference.gui.ConnectScreenWithCallback;
import io.pyke.vitri.finorza.inference.rpc.proto.v1.*;
import io.pyke.vitri.finorza.inference.util.Controller;
import io.pyke.vitri.finorza.inference.util.LevelUtil;
import io.pyke.vitri.finorza.inference.util.Screenshot;

public class RemoteControlService extends RemoteControlServiceGrpc.RemoteControlServiceImplBase {
    private final Screenshot screenshot;
    private final Minecraft minecraft;
    private ActRequest lastAction = null;

    public RemoteControlService() {
        this(new Screenshot(640, 360), Minecraft.getInstance());
    }

    public RemoteControlService(@NotNull Screenshot screenshot, @NotNull Minecraft minecraft) {
        this.screenshot = screenshot;
        this.minecraft = minecraft;
    }

    private static int vitriKeyToCode(Key key) {
        return switch (key) {
            case FORWARD -> 87;
            case BACK -> 83;
            case LEFT -> 65;
            case RIGHT -> 68;
            case JUMP -> 32;
            case SNEAK -> 340;
            case SPRINT -> 341;
            case ESC -> 256;
            case INVENTORY -> 69;
            case SWAP_HANDS -> 70;
            case DROP -> 81;
            case HOTBAR_1 -> 49;
            case HOTBAR_2 -> 50;
            case HOTBAR_3 -> 51;
            case HOTBAR_4 -> 52;
            case HOTBAR_5 -> 53;
            case HOTBAR_6 -> 54;
            case HOTBAR_7 -> 55;
            case HOTBAR_8 -> 56;
            case HOTBAR_9 -> 57;
            default -> throw new AssertionError();
        };
    }

    @Override
    public void listWorlds(Empty request, StreamObserver<ListWorldsResponse> response) {
        try {
            final List<World> worlds = new ArrayList<>();
            for (final LevelSummary level : minecraft.getLevelSource().getLevelList()) {
                final World world = World.newBuilder()
                        .setIcon(ByteString.copyFrom(Files.readAllBytes(level.getIcon().toPath())))
                        .setName(level.getLevelName())
                        .setId(level.getLevelId())
                        .build();

                worlds.add(world);
            }

            response.onNext(ListWorldsResponse.newBuilder().addAllWorlds(worlds).build());
            response.onCompleted();
        } catch (LevelStorageException | IOException e) {
            response.onError(e);
        }
    }

    @Override
    public void enterWorld(EnterWorldRequest request, StreamObserver<Empty> response) {
        if (this.minecraft.getLevelSource().levelExists(request.getId())) {
            this.minecraft.execute(() -> {
                LevelUtil.load(request.getId());
                response.onNext(Empty.getDefaultInstance());
                response.onCompleted();
            });
        } else {
            response.onError(new Exception("World with ID '" + request.getId() + "' not found."));
        }
    }

    @Override
    public void enterServer(EnterServerRequest request, StreamObserver<Empty> response) {
        this.minecraft.execute(() -> {
            LevelUtil.load(null);
            this.minecraft.setScreen(
                    new ConnectScreenWithCallback(
                            new TitleScreen(),
                            this.minecraft,
                            new ServerData(I18n.get("selectServer.defaultName"), request.getAddr(), false),
                            response::onError,
                            (c) -> {
                                response.onNext(Empty.getDefaultInstance());
                                response.onCompleted();
                            }
                    ));
        });
    }

    @Override
    public void updateHumanControl(UpdateHumanControlRequest request, StreamObserver<Empty> response) {
        Controller.getInstance().setHumanControl(request.getEnabled());
        response.onNext(Empty.getDefaultInstance());
        response.onCompleted();
    }

    @Override
    public void observe(Empty request, StreamObserver<ObserveResponse> plainResponse) {
        final boolean isGuiOpen = minecraft.screen != null;
        final boolean isPaused = isGuiOpen && !(minecraft.screen instanceof ChatScreen || minecraft.screen instanceof AbstractContainerScreen<?>);
        final ObserveResponse.Builder builder = ObserveResponse.newBuilder()
                .setDone(false)
                .setGuiOpen(isGuiOpen)
                .setPaused(isPaused || !Controller.getInstance().hasAgentControl());

        final ServerCallStreamObserver<ObserveResponse> response = (ServerCallStreamObserver<ObserveResponse>)plainResponse;

        if (!isPaused) {
            response.setCompression("gzip");
            synchronized (this.screenshot) {
                final CompletableFuture<ByteBuffer> frameFuture = new CompletableFuture<>();
                RenderSystem.recordRenderCall(() -> frameFuture.complete(this.screenshot.read()));

                frameFuture.join(); // just join onto the gRPC thread
                builder.setData(ByteString.copyFrom(this.screenshot.resize(Screenshot.InterpolationMethod.NEAREST_NEIGHBOR)));
            }
        }

        response.onNext(builder.build());
        response.onCompleted();
    }

    @Override
    public void act(ActRequest request, StreamObserver<Empty> response) {
        final Camera camera = request.getMouse().getCamera();
        ((MouseHandlerAccessor) minecraft.mouseHandler).vitri$onMove(camera.getDx(), camera.getDy());

        final List<Key> keys = request.getKeyboard().getKeyList();
        final Set<Key> releasedKeys = new HashSet<>();
        final Set<Key> newlyHitKeys = new HashSet<>(keys);
        if (this.lastAction != null) {
            final List<Key> lastKeys = this.lastAction.getKeyboard().getKeyList();
            lastKeys.forEach(newlyHitKeys::remove);
            releasedKeys.addAll(lastKeys);
            keys.forEach(releasedKeys::remove);
        }

        int mods = 0;
        for (final Key key : keys) {
            if (key == Key.SNEAK) {
                mods |= 1;
            }
        }

        for (final Key key : newlyHitKeys) {
            ((KeyboardHandlerAccessor) minecraft.keyboardHandler).vitri$onKey(vitriKeyToCode(key), 0, 1, mods);
        }
        for (final Key key : releasedKeys) {
            ((KeyboardHandlerAccessor) minecraft.keyboardHandler).vitri$onKey(vitriKeyToCode(key), 0, 0, mods);
        }

        final List<MouseButton> buttons = request.getMouse().getButtonList();
        final Set<MouseButton> releasedButtons = new HashSet<>();
        final Set<MouseButton> pressedButtons = new HashSet<>(buttons);
        if (this.lastAction != null) {
            final List<MouseButton> lastButtons = this.lastAction.getMouse().getButtonList();
            lastButtons.forEach(pressedButtons::remove);
            releasedButtons.addAll(lastButtons);
            buttons.forEach(releasedButtons::remove);
        }

        for (final MouseButton button : pressedButtons) {
            ((MouseHandlerAccessor) minecraft.mouseHandler).vitri$onPress(button.getNumber(), 1, mods);
        }
        for (final MouseButton button : releasedButtons) {
            ((MouseHandlerAccessor) minecraft.mouseHandler).vitri$onPress(button.getNumber(), 0, mods);
        }

        this.lastAction = request;
        response.onNext(Empty.getDefaultInstance());
        response.onCompleted();
    }
}
