package io.pyke.vitri.finorza.inference;

import java.io.IOException;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import io.pyke.vitri.finorza.inference.api.RemoteControlServer;

@Environment(EnvType.CLIENT)
public class FinorzaInference implements ClientModInitializer {
	public static final String MOD_ID = "finorza-inference";
	public static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(
		FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setPrettyPrinting().create();

	@Override
	public void onInitializeClient() {
		try {
			RemoteControlServer.getInstance().start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
