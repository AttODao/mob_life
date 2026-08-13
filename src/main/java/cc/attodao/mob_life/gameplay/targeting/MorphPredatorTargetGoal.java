package cc.attodao.mob_life.gameplay.targeting;

import cc.attodao.mob_life.server.ServerMorphManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

/**
 * Lets vanilla target selection discover a transformed player. The parent goal owns targeting
 * conditions, follow range, LOS/sensing memory, {@code canAttack}, and its normal tick interval.
 */
public final class MorphPredatorTargetGoal extends NearestAttackableTargetGoal<ServerPlayer> {

  public MorphPredatorTargetGoal(Mob mob) {
    super(
        mob,
        ServerPlayer.class,
        10,
        true,
        false,
        (target, level) ->
            target instanceof ServerPlayer player && MorphPredation.isEligibleTarget(mob, player));
  }

  @Override
  public boolean canUse() {
    return ServerMorphManager.hasMobForm() && super.canUse();
  }

  @Override
  public boolean canContinueToUse() {
    return mob.getTarget() instanceof ServerPlayer player
        && MorphPredation.isEligibleTarget(mob, player)
        && super.canContinueToUse();
  }
}
