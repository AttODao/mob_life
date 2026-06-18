package cc.attodao.mob_life.client.screen;

import cc.attodao.mob_life.config.MobLifeConfig;
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
                "mob_life.config.category.gameplay",
                toggle(
                    "mob_life.config.player_morph_enabled",
                    MobLifeConfig.defaultPlayerMorphEnabled(),
                    MobLifeConfig::playerMorphEnabled,
                    MobLifeConfig::setPlayerMorphEnabled)))
        .category(
            category(
                "mob_life.config.category.rendering",
                toggle(
                    "mob_life.config.shader_enabled",
                    MobLifeConfig.defaultShaderEnabled(),
                    MobLifeConfig::shaderEnabled,
                    MobLifeConfig::setShaderEnabled)))
        .category(
            category(
                "mob_life.config.category.inventory",
                toggle(
                    "mob_life.config.hotbar_limit_enabled",
                    MobLifeConfig.defaultHotbarLimitEnabled(),
                    MobLifeConfig::hotbarLimitEnabled,
                    MobLifeConfig::setHotbarLimitEnabled),
                toggle(
                    "mob_life.config.inventory_slot_limit_enabled",
                    MobLifeConfig.defaultInventorySlotLimitEnabled(),
                    MobLifeConfig::inventorySlotLimitEnabled,
                    MobLifeConfig::setInventorySlotLimitEnabled),
                toggle(
                    "mob_life.config.offhand_limit_enabled",
                    MobLifeConfig.defaultOffhandLimitEnabled(),
                    MobLifeConfig::offhandLimitEnabled,
                    MobLifeConfig::setOffhandLimitEnabled)))
        .category(
            category(
                "mob_life.config.category.movement",
                toggle(
                    "mob_life.config.mining_speed_change_enabled",
                    MobLifeConfig.defaultMiningSpeedChangeEnabled(),
                    MobLifeConfig::miningSpeedChangeEnabled,
                    MobLifeConfig::setMiningSpeedChangeEnabled),
                toggle(
                    "mob_life.config.reach_change_enabled",
                    MobLifeConfig.defaultReachChangeEnabled(),
                    MobLifeConfig::reachChangeEnabled,
                    MobLifeConfig::setReachChangeEnabled)))
        .category(
            category(
                "mob_life.config.category.debug",
                toggle(
                    "mob_life.config.awkwardness_debug_enabled",
                    MobLifeConfig.defaultShowAwkwardnessDebug(),
                    MobLifeConfig::showAwkwardnessDebug,
                    MobLifeConfig::setShowAwkwardnessDebug)))
        .save(MobLifeConfig::save)
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
