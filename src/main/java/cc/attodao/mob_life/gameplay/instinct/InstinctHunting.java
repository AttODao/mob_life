package cc.attodao.mob_life.gameplay.instinct;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.gameplay.combat.MorphAttackDamage;
import cc.attodao.mob_life.mixin.instinct.MobGoalSelectorAccessor;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphEntityFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Native target-selector based prey discovery shared by Instinct entry and an active shadow. */
final class InstinctHunting {
  private static final int TARGET_SELECTOR_SAMPLE_TICKS = 10;

  private InstinctHunting() {}

  static boolean hasNearbyNativePrey(
      ServerPlayer player, MorphDefinition definition, MorphConfig config) {
    if (config.instinct().hunting().prey().isEmpty()) {
      return false;
    }

    Entity entity = MorphEntityFactory.create(definition, player.level());
    if (!(entity instanceof Mob shadow)
        || shadow.isNoAi()
        || !MorphAttackDamage.hasAttackAi(definition.type(), shadow)) {
      return false;
    }

    shadow.snapTo(player.position(), player.getYRot(), 0.0F);
    shadow.getSensing().tick();
    var targetSelector = ((MobGoalSelectorAccessor) shadow).mobLife$getTargetSelector();
    // Goal selectors intentionally sample at their own randomized interval. Repeating one normal
    // interval avoids replacing their discovery logic with a radius scan.
    for (int tick = 0; tick < TARGET_SELECTOR_SAMPLE_TICKS; tick++) {
      targetSelector.tick();
      LivingEntity target = shadow.getTarget();
      if (target != null
          && target != player
          && target.isAlive()
          && InstinctRelations.nutrition(target, definition.type(), config).isPresent()) {
        return true;
      }
      shadow.setTarget(null);
    }
    return false;
  }
}
