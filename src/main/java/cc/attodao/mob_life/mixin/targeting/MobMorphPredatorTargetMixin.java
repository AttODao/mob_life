package cc.attodao.mob_life.mixin.targeting;

import cc.attodao.mob_life.gameplay.targeting.MorphPredatorTargetGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMorphPredatorTargetMixin {
  @Shadow @Final protected GoalSelector targetSelector;

  @Inject(method = "<init>", at = @At("TAIL"))
  private void mobLife$addMorphPredatorTargetGoal(CallbackInfo ci) {
    // Keep retaliation and species-specific target goals ahead of this compatibility target.
    targetSelector.addGoal(4, new MorphPredatorTargetGoal((Mob) (Object) this));
  }
}
