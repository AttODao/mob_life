package cc.attodao.mob_life.gameplay.instinct;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardness;
import cc.attodao.mob_life.gameplay.targeting.MorphNearbyEntities;
import cc.attodao.mob_life.gameplay.targeting.MorphPredation;
import cc.attodao.mob_life.mixin.instinct.MobGoalSelectorAccessor;
import cc.attodao.mob_life.morph.MorphDefinition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

/** Vanilla panic/avoid integration for the threat and flee phase of Instinct. */
final class InstinctThreats {

  private InstinctThreats() {}

  static boolean hasNearbyTargetingPredator(
      ServerPlayer player, MorphDefinition definition, MorphConfig config) {
    if (config.combat().predators().isEmpty()
        || config.instinct().senses().predatorRange() <= 0.0
        || MorphAwkwardness.hostileDetectionScale(player) <= 0.0F) {
      return false;
    }

    // The compatibility target goal has already applied the predator's native follow range,
    // TargetingConditions, LOS, canAttack, sensing memory, and randomized acquisition interval.
    return MorphNearbyEntities.living(player, config.instinct().senses().predatorRange()).stream()
        .filter(Mob.class::isInstance)
        .map(Mob.class::cast)
        .anyMatch(
            predator ->
                predator.getTarget() == player
                    && MorphPredation.isPredatorForPlayer(predator, player, definition.type())
                    && MorphPredation.isWithinMorphDetectionRange(
                        predator, player, definition.type()));
  }

  static void installAvoidance(
      PathfinderMob shadow, ServerPlayer player, MorphDefinition definition, MorphConfig config) {
    if (config.combat().predators().isEmpty()
        || config.instinct().senses().predatorRange() <= 0.0) {
      return;
    }
    ((MobGoalSelectorAccessor) shadow)
        .mobLife$getGoalSelector()
        .addGoal(
            3,
            new AvoidEntityGoal<>(
                shadow,
                LivingEntity.class,
                (float) config.instinct().senses().predatorRange(),
                1.0,
                1.33,
                candidate ->
                    candidate instanceof Mob predator
                        && InstinctRelations.isPredator(predator, definition.type())
                        && MorphPredation.isWithinMorphDetectionRange(
                            predator, player, definition.type())));
  }

  static boolean isFleeing(PathfinderMob shadow, boolean panicking) {
    return panicking || isRunning(shadow, AvoidEntityGoal.class);
  }

  static boolean isRunning(PathfinderMob shadow, Class<? extends Goal> goalClass) {
    return ((MobGoalSelectorAccessor) shadow)
        .mobLife$getGoalSelector().getAvailableGoals().stream()
            .filter(WrappedGoal::isRunning)
            .map(WrappedGoal::getGoal)
            .anyMatch(goalClass::isInstance);
  }
}
