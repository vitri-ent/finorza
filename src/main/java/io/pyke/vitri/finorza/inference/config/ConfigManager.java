package io.pyke.vitri.finorza.inference.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.pyke.vitri.finorza.inference.FinorzaInference;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.GsonHelper;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Locale;

public class ConfigManager {
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

	@SuppressWarnings("unchecked")
	private static void load() {
		prepareConfigFile();

		try {
			if (!file.exists()) {
				save();
			}

			if (file.exists()) {
				BufferedReader br = new BufferedReader(new FileReader(file));
				JsonObject json = GsonHelper.parse(br);

				for (Field field : Config.class.getDeclaredFields()) {
					if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())) {
						if (BooleanConfigOption.class.isAssignableFrom(field.getType())) {
							JsonPrimitive primitive = json.getAsJsonPrimitive(field.getName().toLowerCase(Locale.ROOT));
							if (primitive != null && primitive.isBoolean()) {
								BooleanConfigOption option = (BooleanConfigOption) field.get(null);
								ConfigOptionStorage.setBoolean(option.getKey(), primitive.getAsBoolean());
							}
						} else if (EnumConfigOption.class.isAssignableFrom(
							field.getType()) && field.getGenericType() instanceof ParameterizedType) {
							JsonPrimitive primitive = json.getAsJsonPrimitive(field.getName().toLowerCase(Locale.ROOT));
							if (primitive != null && primitive.isString()) {
								Type generic = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
								if (generic instanceof Class<?>) {
									EnumConfigOption<?> option = (EnumConfigOption<?>) field.get(null);
									Enum<?> found = null;
									for (Enum<?> value : ((Class<Enum<?>>) generic).getEnumConstants()) {
										if (value.name().toLowerCase(Locale.ROOT).equals(primitive.getAsString())) {
											found = value;
											break;
										}
									}
									if (found != null) {
										ConfigOptionStorage.setEnumTypeless(option.getKey(), found);
									}
								}
							}
						}
					}
				}
			}
		} catch (FileNotFoundException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public static void save() {
		prepareConfigFile();

		JsonObject config = new JsonObject();

		try {
			for (Field field : Config.class.getDeclaredFields()) {
				if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())) {
					if (BooleanConfigOption.class.isAssignableFrom(field.getType())) {
						BooleanConfigOption option = (BooleanConfigOption) field.get(null);
						config.addProperty(field.getName().toLowerCase(Locale.ROOT),
						                   ConfigOptionStorage.getBoolean(option.getKey())
						);
					} else if (EnumConfigOption.class.isAssignableFrom(
						field.getType()) && field.getGenericType() instanceof ParameterizedType) {
						Type generic = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
						if (generic instanceof Class<?>) {
							EnumConfigOption<?> option = (EnumConfigOption<?>) field.get(null);
							config.addProperty(field.getName().toLowerCase(Locale.ROOT),
							                   ConfigOptionStorage.getEnumTypeless(option.getKey(),
							                                                       (Class<Enum<?>>) generic
							                   ).name().toLowerCase(Locale.ROOT)
							);
						}
					}
				}
			}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		String jsonString = FinorzaInference.GSON.toJson(config);

		try (FileWriter fileWriter = new FileWriter(file)) {
			fileWriter.write(jsonString);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
