package cc.attodao.mob_life.client.mixin.gameplay;

import cc.attodao.mob_life.client.state.ClientMorphState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LocalPlayerMorphFluidMovementMixin {
  @Inject(method = "jumpInLiquid", at = @At("HEAD"), cancellable = true)
  private void mobLife$slowMorphWaterAscent(TagKey<Fluid> fluid, CallbackInfo ci) {
    if (fluid.equals(FluidTags.WATER) && mobLife$applyClientVerticalWaterInput(0.04)) {
      ci.cancel();
    }
  }

  @Inject(method = "goDownInWater", at = @At("HEAD"), cancellable = true)
  private void mobLife$slowMorphWaterDescent(CallbackInfo ci) {
    if (mobLife$applyClientVerticalWaterInput(-0.04)) {
      ci.cancel();
    }
  }

  private boolean mobLife$applyClientVerticalWaterInput(double amount) {
    if (!((Object) this instanceof LocalPlayer player) || ClientMorphState.morph() == null) {
      return false;
    }

    Vec3 movement = player.getDeltaMovement();
    player.setDeltaMovement(
        movement.x, movement.y + amount * ClientMorphState.waterMovementInputScale(), movement.z);
    return true;
  }
}
