package cc.attodao.mob_life.client.mixin.render;

import cc.attodao.mob_life.client.state.ClientMorphState;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapRenderStateExtractorMixin {

  @Inject(method = "extract", at = @At("TAIL"))
  private void mobLife$applyMorphNightVision(
      LightmapRenderState renderState, float tickDelta, CallbackInfo ci) {
    if (!ClientMorphState.nightVision()) {
      return;
    }

    renderState.nightVisionEffectIntensity = Math.max(renderState.nightVisionEffectIntensity, 1.0F);
  }
}
