package rearth.oritech;

import io.wispforest.owo.registration.reflect.FieldRegistrationHandler;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.*;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.network.NetworkContent;

public class Oritech implements ModInitializer {
	public static final String MOD_ID = "oritech";
    public static final Logger LOGGER = LoggerFactory.getLogger("oritech");

	@Override
	public void onInitialize() {

		LOGGER.info("Hello Fabric world!");

		FieldRegistrationHandler.register(ItemContent.class, MOD_ID, false);
		FieldRegistrationHandler.register(BlockContent.class, MOD_ID, false);
		FieldRegistrationHandler.register(BlockEntitiesContent.class, MOD_ID, false);
		ItemGroups.registerItemGroup();
		RecipeContent.initialize();
		NetworkContent.registerChannels();
		ParticleContent.registerParticles();
	}
}