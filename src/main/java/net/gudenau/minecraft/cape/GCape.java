package net.gudenau.minecraft.cape;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GCape implements ClientModInitializer {
	public static final String MODID = "gud_cape";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	@Override
	public void onInitializeClient() {
		SkinManager.init();
	}
}
