package cc.attodao.mob_life.mixin.instinct;

import cc.attodao.mob_life.gameplay.food.MorphFoodCapacity;
import cc.attodao.mob_life.gameplay.instinct.InstinctAiContext;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EatBlockGoal.class)
public abstract class EatBlockGoalInstinctMixin {
  @Shadow @Final private Mob mob;

  @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
  private void mobLife$accelerateHungrySheepForage(CallbackInfoReturnable<Boolean> cir) {
    ServerPlayer owner = InstinctAiContext.owner(mob);
    if (owner == null
        || InstinctAiContext.morph(mob) != MorphType.SHEEP
        || !MorphFoodCapacity.isCriticallyHungry(owner)) {
      return;
    }
    BlockPos pos = mob.blockPosition();
    if (mob.level().getBlockState(pos).is(BlockTags.EDIBLE_FOR_SHEEP)
        || mob.level().getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)) {
      cir.setReturnValue(true);
    }
  }
}
