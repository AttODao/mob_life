package cc.attodao.mob_life.client.mixin.gameplay;

import cc.attodao.mob_life.client.state.ClientInstinctState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftInstinctTargetMixin {
  @Shadow public HitResult hitResult;

  @Inject(method = "pick", at = @At("TAIL"))
  private void mobLife$clearInstinctBlockTarget(float partialTick, CallbackInfo ci) {
    if (!ClientInstinctState.enabled() || !(hitResult instanceof BlockHitResult blockHit)) {
      return;
    }

    hitResult =
        BlockHitResult.miss(
            blockHit.getLocation(), blockHit.getDirection(), blockHit.getBlockPos());
  }

  @Inject(method = "continueAttack", at = @At("HEAD"))
  private void mobLife$recordAttackAction(boolean held, CallbackInfo ci) {
    ClientInstinctState.recordAttackAction(held);
  }
}
