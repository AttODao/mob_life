package cc.attodao.mob_life.mixin.targeting;

import cc.attodao.mob_life.gameplay.targeting.MorphPredation;
import cc.attodao.mob_life.gameplay.targeting.MorphRelations;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvoidEntityGoal.class)
public abstract class AvoidEntityGoalMixin {
  @Shadow protected PathfinderMob mob;

  @Shadow @Nullable protected LivingEntity toAvoid;

  @Inject(
      method = {"canUse", "canContinueToUse"},
      at = @At("RETURN"),
      cancellable = true)
  private void mobLife$doNotFleeFromPreyMorph(CallbackInfoReturnable<Boolean> cir) {
    if (!cir.getReturnValue()) {
      return;
    }
    if (!(toAvoid instanceof ServerPlayer player)) {
      return;
    }
    MorphType morph = MorphRelations.morphOf(player);
    if (morph != null && MorphPredation.isPredatorForPlayer(mob, player, morph)) {
      cir.setReturnValue(false);
    }
  }
}
