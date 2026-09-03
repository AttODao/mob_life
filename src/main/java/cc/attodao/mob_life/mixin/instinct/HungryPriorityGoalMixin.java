package cc.attodao.mob_life.mixin.instinct;

import cc.attodao.mob_life.gameplay.instinct.InstinctAiContext;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BreedGoal.class, FollowParentGoal.class, RandomStrollGoal.class, TemptGoal.class})
public abstract class HungryPriorityGoalMixin {
  @Inject(
      method = {"canUse", "canContinueToUse"},
      at = @At("HEAD"),
      cancellable = true)
  private void mobLife$prioritizeInstinctFood(CallbackInfoReturnable<Boolean> cir) {
    if (InstinctAiContext.hasHungryPriority()) {
      cir.setReturnValue(false);
    }
  }
}
