package cc.attodao.mob_life.mixin.instinct;

import cc.attodao.mob_life.gameplay.food.MorphFoodCapacity;
import cc.attodao.mob_life.gameplay.instinct.InstinctAiContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.animal.rabbit.Rabbit$RaidGardenGoal")
public abstract class RabbitRaidGardenInstinctMixin extends MoveToBlockGoal {
  protected RabbitRaidGardenInstinctMixin(
      PathfinderMob mob, double speedModifier, int searchRange) {
    super(mob, speedModifier, searchRange);
  }

  @Inject(method = "canUse", at = @At("HEAD"))
  private void mobLife$accelerateHungryRabbitForage(CallbackInfoReturnable<Boolean> cir) {
    ServerPlayer owner = InstinctAiContext.owner(mob);
    if (owner != null && MorphFoodCapacity.isCriticallyHungry(owner)) {
      nextStartTick = 0;
    }
  }
}
