package io.pyke.vitri.finorza.inference.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ConnectScreenWithCallback extends Screen {
    private static final AtomicInteger UNIQUE_THREAD_ID = new AtomicInteger(0);
    private static final Logger LOGGER = LogManager.getLogger();
    private Connection connection;
    private boolean aborted;
    private final Screen parent;
    private Component status = new TranslatableComponent("connect.connecting");
    private long lastNarration = -1L;

    private final Consumer<Exception> onError;
    private final Consumer<Integer> onSuccess;

    public ConnectScreenWithCallback(Screen screen, Minecraft minecraft, ServerData serverData, Consumer<Exception> onError, Consumer<Integer> onSuccess) {
        super(NarratorChatListener.NO_TITLE);
        this.minecraft = minecraft;
        this.parent = screen;
        this.onError = onError;
        this.onSuccess = onSuccess;
        ServerAddress serverAddress = ServerAddress.parseString(serverData.ip);
        minecraft.clearLevel();
        minecraft.setCurrentServer(serverData);
        this.connect(serverAddress.getHost(), serverAddress.getPort());
    }

    private void connect(final String string, final int i) {
        LOGGER.info("Connecting to {}, {}", string, i);
        Thread thread = new Thread("Server Connector #" + UNIQUE_THREAD_ID.incrementAndGet()) {
            @Override
            public void run() {
                InetAddress inetAddress = null;
                try {
                    if (ConnectScreenWithCallback.this.aborted) {
                        return;
                    }
                    inetAddress = InetAddress.getByName(string);
                    ConnectScreenWithCallback.this.connection = Connection.connectToServer(
                            inetAddress,
                            i,
                            ConnectScreenWithCallback.this.minecraft.options.useNativeTransport()
                    );
                    ConnectScreenWithCallback.this.connection.setListener(
                            new ClientHandshakePacketListenerImpl(
                                    ConnectScreenWithCallback.this.connection,
                                    ConnectScreenWithCallback.this.minecraft,
                                    ConnectScreenWithCallback.this.parent,
                                    ConnectScreenWithCallback.this::updateStatus
                            )
                    );
                    ConnectScreenWithCallback.this.connection.send(
                            new ClientIntentionPacket(string, i, ConnectionProtocol.LOGIN)
                    );
                    ConnectScreenWithCallback.this.connection.send(
                            new ServerboundHelloPacket(ConnectScreenWithCallback.this.minecraft.getUser().getGameProfile())
                    );
                    ConnectScreenWithCallback.this.onSuccess.accept(0);
                } catch (UnknownHostException unknownHostException) {
                    if (ConnectScreenWithCallback.this.aborted) {
                        return;
                    }
                    LOGGER.error("Couldn't connect to server", unknownHostException);
                    ConnectScreenWithCallback.this.onError.accept(unknownHostException);
                    ConnectScreenWithCallback.this.minecraft.execute(
                            () -> ConnectScreenWithCallback.this.minecraft.setScreen(
                                    new DisconnectedScreen(
                                            ConnectScreenWithCallback.this.parent,
                                            CommonComponents.CONNECT_FAILED,
                                            new TranslatableComponent("disconnect.genericReason", "Unknown host")
                                    )
                            )
                    );
                } catch (Exception exception) {
                    if (ConnectScreenWithCallback.this.aborted) {
                        return;
                    }
                    LOGGER.error("Couldn't connect to server", exception);
                    ConnectScreenWithCallback.this.onError.accept(exception);
                    final String reason = inetAddress == null ? exception.toString() : exception.toString().replaceAll(inetAddress + ":" + i, "");
                    ConnectScreenWithCallback.this.minecraft.execute(
                            () -> ConnectScreenWithCallback.this.minecraft.setScreen(
                                    new DisconnectedScreen(
                                            ConnectScreenWithCallback.this.parent,
                                            CommonComponents.CONNECT_FAILED,
                                            new TranslatableComponent("disconnect.genericReason", reason)
                                    )
                            )
                    );
                }
            }
        };
        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        thread.start();
    }

    private void updateStatus(Component component) {
        this.status = component;
    }

    @Override
    public void tick() {
        if (this.connection != null) {
            if (this.connection.isConnected()) {
                this.connection.tick();
            } else {
                this.connection.handleDisconnection();
            }
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        this.addButton(
                new Button(
                        this.width / 2 - 100,
                        this.height / 4 + 120 + 12,
                        200,
                        20,
                        CommonComponents.GUI_CANCEL,
                        button -> {
                            this.aborted = true;
                            if (this.connection != null) {
                                this.connection.disconnect(new TranslatableComponent("connect.aborted"));
                            }
                            this.minecraft.setScreen(this.parent);
                        }
                )
        );
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(poseStack);
        final long l = Util.getMillis();
        if (l - this.lastNarration > 2000L) {
            this.lastNarration = l;
            NarratorChatListener.INSTANCE.sayNow(new TranslatableComponent("narrator.joining").getString());
        }
        ConnectScreenWithCallback.drawCenteredString(poseStack, this.font, this.status, this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        super.render(poseStack, mouseX, mouseY, partialTicks);
    }
}

