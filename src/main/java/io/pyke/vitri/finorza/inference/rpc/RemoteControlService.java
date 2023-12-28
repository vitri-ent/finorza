package io.pyke.vitri.finorza.inference.rpc;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import io.pyke.vitri.finorza.inference.mixin.input.KeyboardHandlerAccessor;
import io.pyke.vitri.finorza.inference.mixin.input.MouseHandlerAccessor;
import io.pyke.vitri.finorza.inference.client.AgentControl;
import io.pyke.vitri.finorza.inference.client.EnvironmentRecorder;
import io.pyke.vitri.finorza.inference.gui.ConnectScreenWithCallback;
import io.pyke.vitri.finorza.inference.rpc.proto.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelSummary;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RemoteControlService extends RemoteControlServiceGrpc.RemoteControlServiceImplBase {
    private final Minecraft client;
    private ActRequest lastAction = null;

    public RemoteControlService() {
        this(Minecraft.getInstance());
    }

    public RemoteControlService(@NotNull Minecraft client) {
        this.client = client;
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
            for (final LevelSummary level : client.getLevelSource().getLevelList()) {
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
        if (client.getLevelSource().levelExists(request.getId())) {
            AgentControl.dispatchMainThread(() -> {
                AgentControl.loadLevel(request.getId());
                response.onNext(Empty.getDefaultInstance());
                response.onCompleted();
            });
        } else {
            response.onError(new Exception("World with ID '" + request.getId() + "' not found."));
        }
    }

    @Override
    public void enterServer(EnterServerRequest request, StreamObserver<Empty> response) {
        AgentControl.dispatchMainThread(() -> {
            AgentControl.disconnect();
            client.setScreen(new ConnectScreenWithCallback(new TitleScreen(), client, new ServerData(
                    I18n.get("selectServer.defaultName"), request.getAddr(), false),
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
        AgentControl.setHumanControl(request.getEnabled());
        response.onNext(Empty.getDefaultInstance());
        response.onCompleted();
    }

    @Override
    public void observe(Empty request, StreamObserver<ObserveResponse> response) {
        final boolean isGuiOpen = client.screen != null;
        final boolean isPaused = isGuiOpen && !(client.screen instanceof ChatScreen || client.screen instanceof AbstractContainerScreen<?>);
        final ObserveResponse.Builder builder = ObserveResponse.newBuilder()
                .setDone(false)
                .setGuiOpen(isGuiOpen)
                .setPaused(isPaused || !AgentControl.hasAgentControl());

        if (!isPaused) {
            final EnvironmentRecorder recorder = EnvironmentRecorder.getInstance();
            AgentControl.dispatchAndAwait(recorder::getFrame, (frame) -> {
                builder.setData(ByteString.copyFrom(AgentControl.resizeFrame(frame)));
                response.onNext(builder.build());
                response.onCompleted();
            });
        } else {
            response.onNext(builder.build());
            response.onCompleted();
        }
    }

    @Override
    public void act(ActRequest request, StreamObserver<Empty> response) {
        final Camera camera = request.getMouse().getCamera();
        ((MouseHandlerAccessor) client.mouseHandler).vitri$onMove(camera.getDx(), camera.getDy());

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
            ((KeyboardHandlerAccessor) client.keyboardHandler).vitri$onKey(vitriKeyToCode(key), 0, 1, mods);
        }
        for (final Key key : releasedKeys) {
            ((KeyboardHandlerAccessor) client.keyboardHandler).vitri$onKey(vitriKeyToCode(key), 0, 0, mods);
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
            ((MouseHandlerAccessor) client.mouseHandler).vitri$onPress(button.getNumber(), 1, mods);
        }
        for (final MouseButton button : releasedButtons) {
            ((MouseHandlerAccessor) client.mouseHandler).vitri$onPress(button.getNumber(), 0, mods);
        }

        this.lastAction = request;
        response.onNext(Empty.getDefaultInstance());
        response.onCompleted();
    }
}
