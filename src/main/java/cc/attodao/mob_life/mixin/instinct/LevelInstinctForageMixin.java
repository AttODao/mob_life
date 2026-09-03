package cc.attodao.mob_life.mixin.instinct;

import cc.attodao.mob_life.gameplay.instinct.InstinctAiContext;
import cc.attodao.mob_life.gameplay.instinct.MorphInstinct;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarrotBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelInstinctForageMixin {
  @Inject(
      method =
          "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
      at = @At("RETURN"))
  private void mobLife$rewardConsumedForage(
      BlockPos pos,
      BlockState replacement,
      int flags,
      int recursionLeft,
      CallbackInfoReturnable<Boolean> cir) {
    ServerPlayer owner = InstinctAiContext.owner();
    MorphType morph = InstinctAiContext.morph();
    if (owner == null || !cir.getReturnValueZ()) {
      return;
    }
    boolean forageReplacement =
        morph == MorphType.SHEEP && replacement.is(Blocks.DIRT)
            || morph == MorphType.RABBIT
                && (replacement.isAir() || replacement.getBlock() instanceof CarrotBlock);
    if (forageReplacement) {
      MorphInstinct.onForageConsumed(owner);
    }
  }

  @Inject(
      method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",
      at = @At("RETURN"))
  private void mobLife$rewardDestroyedSheepForage(
      BlockPos pos,
      boolean dropBlock,
      net.minecraft.world.entity.Entity breaker,
      int recursionLeft,
      CallbackInfoReturnable<Boolean> cir) {
    ServerPlayer owner = InstinctAiContext.owner();
    if (owner != null && InstinctAiContext.morph() == MorphType.SHEEP && cir.getReturnValueZ()) {
      MorphInstinct.onForageConsumed(owner);
    }
  }
}
