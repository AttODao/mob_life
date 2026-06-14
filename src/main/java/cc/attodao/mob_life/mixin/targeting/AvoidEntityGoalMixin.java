package cc.attodao.mob_life.mixin.targeting;

import cc.attodao.mob_life.gameplay.targeting.MorphPredation;
import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.server.ServerMorphManager;
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

  @Inject(method = "canUse", at = @At("RETURN"), cancellable = true)
  private void mobLife$doNotFleeFromPreyMorph(CallbackInfoReturnable<Boolean> cir) {
    MorphType morph = ServerMorphManager.activeMorph();
    if (cir.getReturnValue()
        && morph != null
        && toAvoid instanceof ServerPlayer player
        && MorphPredation.isPredatorForPlayer(mob, player, morph)) {
      cir.setReturnValue(false);
    }
  }
}
