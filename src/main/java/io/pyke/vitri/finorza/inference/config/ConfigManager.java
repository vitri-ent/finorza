package io.pyke.vitri.finorza.inference.config;

import io.pyke.vitri.finorza.inference.FinorzaInference;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Modifier;

public class ConfigManager {
	private static final Gson GSON = new GsonBuilder()
		.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
		.setPrettyPrinting()
		.excludeFieldsWithModifiers(Modifier.PRIVATE)
		.create();

	private static Config config = null;

	public static @NotNull Config getConfig() {
		if (ConfigManager.config == null) {
			ConfigManager.initializeConfig();
		}
		return ConfigManager.config;
	}

	private static File file;

	private static void prepareConfigFile() {
		if (file != null) {
			return;
		}
		file = new File(FabricLoader.getInstance().getConfigDir().toFile(), FinorzaInference.MOD_ID + ".json");
	}

	public static void initializeConfig() {
		load();
	}

	private static void load() {
		prepareConfigFile();

		try {
			if (!file.exists()) {
				save();
			}

			BufferedReader br = new BufferedReader(new FileReader(file));
			JsonObject json = GsonHelper.parse(br);
			ConfigManager.config = GSON.fromJson(json, Config.class);
			FinorzaInference.LOGGER.info("Loaded config from " + file.getAbsolutePath());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (ConfigManager.config == null) {
				FinorzaInference.LOGGER.warn("Loading default config");
				ConfigManager.config = new Config();
			}
		}
	}

	public static void save() {
		prepareConfigFile();

		if (ConfigManager.config == null) {
			FinorzaInference.LOGGER.warn("Loading default config");
			ConfigManager.config = new Config();
		}
		String jsonString = GSON.toJson(ConfigManager.config);

		try (FileWriter fileWriter = new FileWriter(file)) {
			FinorzaInference.LOGGER.warn("Saving config to " + file.getAbsolutePath());
			fileWriter.write(jsonString);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
