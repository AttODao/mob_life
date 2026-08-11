package cc.attodao.mob_life.client.mixin.render;

import cc.attodao.mob_life.client.state.ClientOutlineState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMorphOutlineMixin {
  @Inject(method = "extractRenderState", at = @At("TAIL"))
  private void mobLife$applyPredatorOrPreyOutline(
      Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
    int color = ClientOutlineState.color(entity.getId());
    if (color != 0) {
      state.outlineColor = color;
    }
  }
}
