package io.pyke.vitri.finorza.inference.api;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelSummary;

import io.pyke.vitri.finorza.inference.client.AgentControl;
import io.pyke.vitri.finorza.inference.client.EnvironmentRecorder;
import io.pyke.vitri.finorza.inference.gui.ConnectScreenWithCallback;
import io.pyke.vitri.finorza.inference.proto.RemoteControlServiceGrpc;
import io.pyke.vitri.finorza.inference.proto.VitriMcrl;

public class RemoteControlService extends RemoteControlServiceGrpc.RemoteControlServiceImplBase {
	private final Minecraft minecraft = Minecraft.getInstance();
	private VitriMcrl.ActRequest lastAction = null;

	private static int vitriKeyToCode(VitriMcrl.Key key) {
		switch (key) {
			case Forward -> {
				return 87;
			}
			case Back -> {
				return 83;
			}
			case Left -> {
				return 65;
			}
			case Right -> {
				return 68;
			}
			case Jump -> {
				return 32;
			}
			case Sneak -> {
				return 340;
			}
			case Sprint -> {
				return 341;
			}
			case Esc -> {
				return 256;
			}
			case Inventory -> {
				return 69;
			}
			case SwapHands -> {
				return 70;
			}
			case Drop -> {
				return 81;
			}
			case Hotbar1 -> {
				return 49;
			}
			case Hotbar2 -> {
				return 50;
			}
			case Hotbar3 -> {
				return 51;
			}
			case Hotbar4 -> {
				return 52;
			}
			case Hotbar5 -> {
				return 53;
			}
			case Hotbar6 -> {
				return 54;
			}
			case Hotbar7 -> {
				return 55;
			}
			case Hotbar8 -> {
				return 56;
			}
			case Hotbar9 -> {
				return 57;
			}
			default -> {
				throw new RuntimeException();
			}
		}
	}

	private static int vitriButtonToCode(VitriMcrl.MouseButton button) {
		switch (button) {
			case Attack -> {
				return 0;
			}
			case Use -> {
				return 1;
			}
			case PickItem -> {
				return 2;
			}
			default -> {
				throw new RuntimeException();
			}
		}
	}

	@Override
	public void listSingleplayerWorlds(
		Empty request, StreamObserver<VitriMcrl.SingleplayerWorlds> response
	) {
		List<VitriMcrl.SingleplayerWorld> worlds = new ArrayList<>();
		try {
			for (LevelSummary level : minecraft.getLevelSource().getLevelList()) {
				VitriMcrl.SingleplayerWorld world = VitriMcrl.SingleplayerWorld.newBuilder()
					.setIcon(ByteString.copyFrom(Files.readAllBytes(level.getIcon().toPath())))
					.setName(level.getLevelName()).setId(level.getLevelId()).build();
				worlds.add(world);
			}
		} catch (LevelStorageException | IOException exception) {
			response.onError(exception);
		}
		response.onNext(VitriMcrl.SingleplayerWorlds.newBuilder().addAllWorlds(worlds).build());
		response.onCompleted();
	}

	@Override
	public void enterSingleplayerWorld(
		VitriMcrl.EnterSingleplayerWorldRequest request, StreamObserver<Empty> response
	) {
		if (minecraft.getLevelSource().levelExists(request.getId())) {
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
	public void enterMultiplayerServer(
		VitriMcrl.EnterMultiplayerServerRequest request, StreamObserver<Empty> response
	) {
		AgentControl.dispatchMainThread(() -> {
			AgentControl.disconnect();
			minecraft.setScreen(new ConnectScreenWithCallback(new TitleScreen(), minecraft, new ServerData(
				I18n.get("selectServer.defaultName", new Object[0]), request.getAddr(), false), response::onError,
			                                                  (c) -> {
				                                                  response.onNext(Empty.getDefaultInstance());
				                                                  response.onCompleted();
			                                                  }
			));
		});
	}

	@Override
	public void setHumanControl(VitriMcrl.SetHumanControlRequest request, StreamObserver<Empty> response) {
		boolean enable = request.getEnable();
		AgentControl.setHumanControl(enable);
		response.onNext(Empty.getDefaultInstance());
		response.onCompleted();
	}

	@Override
	public void observe(Empty request, StreamObserver<VitriMcrl.ClientObservation> response) {
		boolean isGuiOpen = minecraft.screen != null;
		boolean isPaused = isGuiOpen && !(minecraft.screen instanceof ChatScreen || minecraft.screen instanceof AbstractContainerScreen<?>);
		VitriMcrl.ClientObservation.Builder builder = VitriMcrl.ClientObservation.newBuilder().setDone(false)
			.setIsGuiOpen(isGuiOpen).setIsPaused(isPaused || !AgentControl.hasAgentControl());
		if (!isPaused) {
			EnvironmentRecorder recorder = EnvironmentRecorder.getInstance();
			AgentControl.dispatchAndAwait(recorder::getFrame, (frame) -> {
				ByteBuffer resized = AgentControl.resizeFrame(frame);
				builder.setObservation(ByteString.copyFrom(resized));
				response.onNext(builder.build());
				response.onCompleted();
			});
		} else {
			response.onNext(builder.build());
			response.onCompleted();
		}
	}

	@Override
	public void act(VitriMcrl.ActRequest request, StreamObserver<Empty> response) {
		VitriMcrl.Camera camera = request.getMouse().getCamera();
		((IMouseHandler) minecraft.mouseHandler).vitri$onMove(camera.getDx(), camera.getDy());

		List<VitriMcrl.Key> keys = request.getKeys().getKeyList();
		Set<VitriMcrl.Key> releasedKeys = new HashSet<>();
		Set<VitriMcrl.Key> newlyHitKeys = new HashSet<>(keys);
		if (this.lastAction != null) {
			this.lastAction.getKeys().getKeyList().forEach(newlyHitKeys::remove);
			releasedKeys.addAll(this.lastAction.getKeys().getKeyList());
			keys.forEach(releasedKeys::remove);
		}

		int mods = 0;
		for (VitriMcrl.Key key : keys) {
			if (key == VitriMcrl.Key.Sneak) {
				mods |= 1;
			}
		}

		for (VitriMcrl.Key key : newlyHitKeys) {
			((IKeyboardHandler) minecraft.keyboardHandler).vitri$onKey(vitriKeyToCode(key), 0, 1, mods);
		}
		for (VitriMcrl.Key key : releasedKeys) {
			((IKeyboardHandler) minecraft.keyboardHandler).vitri$onKey(vitriKeyToCode(key), 0, 0, mods);
		}

		List<VitriMcrl.MouseButton> buttons = request.getMouse().getButtonList();
		Set<VitriMcrl.MouseButton> releasedButtons = new HashSet<>();
		Set<VitriMcrl.MouseButton> pressedButtons = new HashSet<>(buttons);
		if (this.lastAction != null) {
			this.lastAction.getMouse().getButtonList().forEach(pressedButtons::remove);
			releasedButtons.addAll(this.lastAction.getMouse().getButtonList());
			buttons.forEach(releasedButtons::remove);
		}

		for (VitriMcrl.MouseButton button : pressedButtons) {
			((IMouseHandler) minecraft.mouseHandler).vitri$onPress(vitriButtonToCode(button), 1, mods);
		}
		for (VitriMcrl.MouseButton button : releasedButtons) {
			((IMouseHandler) minecraft.mouseHandler).vitri$onPress(vitriButtonToCode(button), 0, mods);
		}

		this.lastAction = request;
		response.onNext(Empty.getDefaultInstance());
		response.onCompleted();
	}
}
