package cc.attodao.mob_life.gameplay.instinct;

import cc.attodao.mob_life.mixin.instinct.LivingEntityDamageStateAccessor;
import cc.attodao.mob_life.mixin.instinct.MobGoalSelectorAccessor;
import cc.attodao.mob_life.mixin.instinct.PanicGoalInvoker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;

/** Resolves damage reactions through the source mob's native panic and retaliation goals. */
final class InstinctDamageResponse {

  private InstinctDamageResponse() {}

  static Result evaluate(PathfinderMob shadow, DamageSource source, long gameTime) {
    LivingEntity previousAttacker = shadow.getLastHurtByMob();
    int previousAttackerTimestamp = shadow.getLastHurtByMobTimestamp();

    LivingEntityDamageStateAccessor damageState = (LivingEntityDamageStateAccessor) shadow;
    damageState.mobLife$setLastDamageSource(source);
    damageState.mobLife$setLastDamageStamp(gameTime);
    // This is vanilla's own NO_ANGER, causing-entity, and wind-charge filtering.
    damageState.mobLife$invokeResolveMobResponsibleForDamage(source);

    boolean panicking =
        ((MobGoalSelectorAccessor) shadow)
            .mobLife$getGoalSelector().getAvailableGoals().stream()
                .map(WrappedGoal::getGoal)
                .filter(PanicGoal.class::isInstance)
                .map(PanicGoal.class::cast)
                // canUse also rolls a random destination. The native trigger predicate identifies
                // whether this damage changes the mob to a panic response; pathing remains native.
                .anyMatch(goal -> ((PanicGoalInvoker) goal).mobLife$shouldPanic());

    LivingEntity attacker = shadow.getLastHurtByMob();
    boolean attackerStateChanged =
        attacker != previousAttacker
            || shadow.getLastHurtByMobTimestamp() != previousAttackerTimestamp;
    boolean retaliating =
        attackerStateChanged
            && attacker != null
            && ((MobGoalSelectorAccessor) shadow)
                .mobLife$getTargetSelector().getAvailableGoals().stream()
                    // A running HurtByTargetGoal keeps its current target exactly as vanilla does;
                    // the new timestamp is considered only after that goal stops.
                    .filter(wrapped -> !wrapped.isRunning())
                    .map(WrappedGoal::getGoal)
                    .filter(HurtByTargetGoal.class::isInstance)
                    .map(HurtByTargetGoal.class::cast)
                    .anyMatch(HurtByTargetGoal::canUse);
    boolean resettingUniversalAnger =
        attackerStateChanged
            && ((MobGoalSelectorAccessor) shadow)
                .mobLife$getTargetSelector().getAvailableGoals().stream()
                    .filter(wrapped -> !wrapped.isRunning())
                    .map(WrappedGoal::getGoal)
                    .filter(ResetUniversalAngerTargetGoal.class::isInstance)
                    .map(ResetUniversalAngerTargetGoal.class::cast)
                    .anyMatch(ResetUniversalAngerTargetGoal::canUse);

    return new Result(panicking, retaliating ? attacker : null, resettingUniversalAnger);
  }

  record Result(
      boolean panicking, LivingEntity retaliationTarget, boolean resettingUniversalAnger) {
    boolean changesBehavior() {
      return panicking || retaliationTarget != null || resettingUniversalAnger;
    }
  }
}
