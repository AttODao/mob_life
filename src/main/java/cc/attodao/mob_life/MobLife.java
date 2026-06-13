package cc.attodao.mob_life;

import cc.attodao.mob_life.network.MobLifeNetworking;
import cc.attodao.mob_life.server.MobLifeCommand;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MobLife implements ModInitializer {
	public static final String MOD_ID = "mob_life";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		MobLifeNetworking.registerPayloads();
		MobLifeCommand.register();
		ServerMorphManager.registerEvents();
	}
}
