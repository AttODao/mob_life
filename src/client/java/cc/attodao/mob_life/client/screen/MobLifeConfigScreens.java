package cc.attodao.mob_life.client.screen;

import cc.attodao.mob_life.client.config.ClientMobLifeConfig;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MobLifeConfigScreens {
  private static final String YACL_MOD_ID = "yet_another_config_lib_v3";

  private MobLifeConfigScreens() {}

  public static boolean isAvailable() {
    return FabricLoader.getInstance().isModLoaded(YACL_MOD_ID);
  }

  public static Screen create(Screen parent) {
    return YetAnotherConfigLib.createBuilder()
        .title(Component.translatable("mob_life.config.title"))
        .category(
            category(
                "mob_life.config.category.rendering",
                toggle(
                    "mob_life.config.shader_enabled",
                    ClientMobLifeConfig.defaultShaderEnabled(),
                    ClientMobLifeConfig::shaderEnabled,
                    ClientMobLifeConfig::setShaderEnabled)))
        .category(
            category(
                "mob_life.config.category.debug",
                toggle(
                    "mob_life.config.awkwardness_debug_enabled",
                    ClientMobLifeConfig.defaultShowAwkwardnessDebug(),
                    ClientMobLifeConfig::showAwkwardnessDebug,
                    ClientMobLifeConfig::setShowAwkwardnessDebug)))
        .save(ClientMobLifeConfig::save)
        .build()
        .generateScreen(parent);
  }

  private static ConfigCategory category(String translationKey, Option<?>... options) {
    var builder = ConfigCategory.createBuilder().name(Component.translatable(translationKey));
    for (Option<?> option : options) {
      builder.option(option);
    }
    return builder.build();
  }

  private static Option<Boolean> toggle(
      String translationKey,
      boolean defaultValue,
      Supplier<Boolean> getter,
      Consumer<Boolean> setter) {
    return Option.<Boolean>createBuilder()
        .name(Component.translatable(translationKey))
        .binding(defaultValue, getter::get, setter)
        .controller(TickBoxControllerBuilder::create)
        .build();
  }
}
