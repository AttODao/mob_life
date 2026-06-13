package cc.attodao.mob_life.client;

import cc.attodao.mob_life.client.screen.MobLifeOptionsScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class MobLifeModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return MobLifeOptionsScreen::new;
	}
}
