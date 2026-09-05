package cc.attodao.mob_life.client.mixin.gameplay;

import cc.attodao.mob_life.client.state.ClientMorphState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LocalPlayerMorphFluidMovementMixin {
  @Inject(method = "goDownInWater", at = @At("HEAD"), cancellable = true)
  private void mobLife$disableMorphWaterDescent(CallbackInfo ci) {
    if ((Object) this instanceof LocalPlayer && ClientMorphState.morph() != null) {
      ci.cancel();
    }
  }
}
