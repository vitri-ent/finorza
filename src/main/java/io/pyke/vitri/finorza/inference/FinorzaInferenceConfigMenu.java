package io.pyke.vitri.finorza.inference;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import io.pyke.vitri.finorza.inference.api.WindowAccessor;
import io.pyke.vitri.finorza.inference.config.Config;
import io.pyke.vitri.finorza.inference.config.ConfigManager;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TranslatableComponent;

public class FinorzaInferenceConfigMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            Config defaultConfig = new Config();
            Config config = ConfigManager.getConfig();

            AtomicInteger newControllerPort = new AtomicInteger(config.controlPort);

            ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(new TranslatableComponent("finorza-inference.display").withStyle(s -> s.withBold(true)))
                .setSavingRunnable(() -> {
                    ConfigManager.save();

                    int newControllerPortValue = newControllerPort.get();
                    if (newControllerPortValue != FinorzaInference.remoteControlServer.getPort()) {
                        FinorzaInference.remoteControlServer.shutdownNow();
                        FinorzaInference.LOGGER.info("Shutdown old RPC");

                        FinorzaInference.remoteControlServer = FinorzaInference.createControlServer(newControllerPortValue);
                        try {
                            FinorzaInference.remoteControlServer.start();
                            FinorzaInference.LOGGER.info("RPC started on port " + newControllerPortValue);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .transparentBackground();
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            ConfigCategory rendering = builder.getOrCreateCategory(new TranslatableComponent("finorza-inference.option.rendering.display"));
            rendering.addEntry(
                entryBuilder.startEnumSelector(
                    new TranslatableComponent("finorza-inference.option.rendering.observationFrameSize.display"),
                    Config.ObservationFrameSize.class,
                    config.observationFrameSize
                )
                    .setDefaultValue(defaultConfig.observationFrameSize)
                    .setEnumNameProvider(en -> new TranslatableComponent("finorza-inference.option.rendering.observationFrameSize." + en.toString().toLowerCase()))
                    .setSaveConsumer(v -> config.observationFrameSize = v)
                    .build()
            );
            rendering.addEntry(
                entryBuilder.startTextDescription(
                    new TranslatableComponent(
                        "finorza-inference.option.rendering.observationFrameSize.description",
                        new TranslatableComponent("finorza-inference.option.rendering.observationFrameSize.minerl")
                            .withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)),
                        new TranslatableComponent("finorza-inference.option.rendering.observationFrameSize.act_rvit")
                            .withStyle(s -> s.withBold(true).withColor(ChatFormatting.GREEN)),
                        new TranslatableComponent("finorza-inference.option.rendering.observationFrameSize.native")
                            .withStyle(s -> s.withBold(true).withColor(ChatFormatting.AQUA))
                    )
                )
                    .build()
            );
            rendering.addEntry(
                entryBuilder.startEnumSelector(
                    new TranslatableComponent("finorza-inference.option.rendering.cursorSize.display"),
                    Config.CursorSize.class,
                    config.cursorSize
                )
                    .setDefaultValue(defaultConfig.cursorSize)
                    .setEnumNameProvider(en -> new TranslatableComponent("finorza-inference.option.rendering.cursorSize." + en.toString().toLowerCase()))
                    .setSaveConsumer(v -> config.cursorSize = v)
                    .build()
            );
            rendering.addEntry(
                entryBuilder.startEnumSelector(
                    new TranslatableComponent("finorza-inference.option.rendering.windowSize.display"),
                    Config.WindowSize.class,
                    config.windowSize
                )
                    .setDefaultValue(defaultConfig.windowSize)
                    .setEnumNameProvider(en -> new TranslatableComponent("finorza-inference.option.rendering.windowSize." + en.toString().toLowerCase()))
                    .setSaveConsumer(v -> {
                        config.windowSize = v;
                        ((WindowAccessor)(Object) Minecraft.getInstance().getWindow()).vitri$resize(v.width, v.height);
                    })
                    .build()
            );

            ConfigCategory rpc = builder.getOrCreateCategory(new TranslatableComponent("finorza-inference.option.rpc.display"));
            rpc.addEntry(
                entryBuilder.startIntField(
                    new TranslatableComponent("finorza-inference.option.rpc.controlPort.display"),
                    config.controlPort
                )
                    .setDefaultValue(defaultConfig.controlPort)
                    .setSaveConsumer(newControllerPort::set)
                    .build()
            );
            rpc.addEntry(
                entryBuilder.startBooleanToggle(
                    new TranslatableComponent("finorza-inference.option.rpc.compressObservation.display"),
                    config.compressObservation
                )
                    .setDefaultValue(defaultConfig.compressObservation)
                    .setSaveConsumer(v -> config.compressObservation = v)
                        .build()
            );
            rpc.addEntry(
                entryBuilder.startTextDescription(
                    new TranslatableComponent(
                        "finorza-inference.option.rpc.compressObservation.description",
                        new TranslatableComponent("finorza-inference.option.rendering.observationFrameSize.display").withStyle(s -> s.withBold(true)),
                        new TranslatableComponent("finorza-inference.option.rendering.observationFrameSize.native").withStyle(s -> s.withBold(true).withColor(ChatFormatting.AQUA))
                    )
                )
                    .build()
            );

            ConfigCategory world = builder.getOrCreateCategory(new TranslatableComponent("finorza-inference.option.world.display"));
            world.addEntry(
                entryBuilder.startBooleanToggle(
                    new TranslatableComponent("finorza-inference.option.world.synchronizeTick.display"),
                    config.synchronizeIntegratedServer
                )
                    .setDefaultValue(defaultConfig.synchronizeIntegratedServer)
                    .setSaveConsumer(v -> config.synchronizeIntegratedServer = v)
                    .build()
            );
            world.addEntry(entryBuilder.startTextDescription(new TranslatableComponent("finorza-inference.option.world.synchronizeTick.description")).build());

            return builder.build();
        };
    }
}
