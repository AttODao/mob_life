package cc.attodao.mob_life.gameplay.targeting;

import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardness;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class MorphPredation {
  private MorphPredation() {}

  public static boolean isPredatorFor(Entity entity, MorphType morph) {
    if (morph == null || entity instanceof TamableAnimal tamable && tamable.isTame()) {
      return false;
    }
    MorphType entityMorph =
        entity instanceof net.minecraft.world.entity.player.Player player
            ? MorphRelations.morphOf(player)
            : null;
    Identifier id =
        BuiltInRegistries.ENTITY_TYPE.getKey(
            entityMorph != null ? entityMorph.entityType() : entity.getType());
    return MorphConfigManager.get(morph).combat().predators().contains(id.toString());
  }

  /**
   * This is only the morph relation predicate. Range, LOS, sensing, {@code canAttack}, target
   * visibility, and continue conditions are evaluated by {@link MorphPredatorTargetGoal}.
   */
  public static boolean isEligibleTarget(Mob mob, ServerPlayer player) {
    MorphType morph = MorphRelations.morphOf(player);
    return morph != null
        && player.isAlive()
        && !player.isSpectator()
        && !MorphRelations.isSameSpecies(mob, morph)
        && isPredatorFor(mob, morph);
  }

  public static boolean isPredatorForPlayer(Mob mob, ServerPlayer player, MorphType morph) {
    return morph != null
        && !MorphRelations.isSameSpecies(mob, morph)
        && isPredatorFor(mob, morph)
        && player.isAlive()
        && !player.isSpectator();
  }

  public static double morphDetectionRange(Mob mob, ServerPlayer player, MorphType morph) {
    if (morph == null) {
      return 0.0;
    }
    return mob.getAttributeValue(Attributes.FOLLOW_RANGE)
        * MorphAwkwardness.hostileDetectionScale(player)
        * MorphConfigManager.get(morph).combat().hostileDetectionMultiplier();
  }

  public static boolean isWithinMorphDetectionRange(Mob mob, ServerPlayer player, MorphType morph) {
    double range = morphDetectionRange(mob, player, morph);
    return range > 0.0 && mob.distanceToSqr(player) <= range * range;
  }
}
