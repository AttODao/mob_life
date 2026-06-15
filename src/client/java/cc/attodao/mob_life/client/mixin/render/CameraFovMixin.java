package cc.attodao.mob_life.client.mixin.render;

import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.config.MorphConfigManager;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraFovMixin {
  @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
  private void mobLife$applyMorphFieldOfView(
      float partialTicks, CallbackInfoReturnable<Float> cir) {
    if (ClientMorphState.morph() == null) {
      return;
    }

    float multiplier =
        MorphConfigManager.get(ClientMorphState.morph()).vision().fieldOfViewMultiplier();
    cir.setReturnValue(Math.clamp(cir.getReturnValueF() * multiplier, 30.0F, 150.0F));
  }
}
