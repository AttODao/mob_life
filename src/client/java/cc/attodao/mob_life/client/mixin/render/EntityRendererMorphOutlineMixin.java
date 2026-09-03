package cc.attodao.mob_life.client.mixin.render;

import cc.attodao.mob_life.client.state.ClientPredatorOutlineState;
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
  private void mobLife$applyPredatorOutline(
      Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
    int entityId = ((EntityIdAccessor) entity).mobLife$getId();
    if (entityId == Entity.INVALID_ENTITY_ID || entity.level().getEntity(entityId) != entity) {
      return;
    }

    if (ClientPredatorOutlineState.contains(entityId)) {
      state.outlineColor = 0xFFFF3B30;
    } else if (ClientPredatorOutlineState.containsPrey(entityId)) {
      state.outlineColor = 0xFFFFD54A;
    }
  }
}
