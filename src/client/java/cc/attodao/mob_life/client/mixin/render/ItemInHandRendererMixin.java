package cc.attodao.mob_life.client.mixin.render;

import cc.attodao.mob_life.client.render.FirstPersonMorphHandRenderer;
import cc.attodao.mob_life.client.state.ClientInstinctState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
  @ModifyVariable(method = "renderArmWithItem", at = @At("HEAD"), argsOnly = true)
  private ItemStack mobLife$showEmptyInstinctHand(ItemStack stack) {
    return ClientInstinctState.enabled() ? ItemStack.EMPTY : stack;
  }

  @Inject(method = "renderPlayerArm", at = @At("HEAD"), cancellable = true)
  private void mobLife$renderMorphEmptyHand(
      PoseStack poseStack,
      SubmitNodeCollector collector,
      int lightCoords,
      float inverseArmHeight,
      float attackValue,
      HumanoidArm arm,
      CallbackInfo ci) {
    if (FirstPersonMorphHandRenderer.renderEmptyHand(
        poseStack, collector, lightCoords, inverseArmHeight, attackValue, arm)) {
      ci.cancel();
    }
  }

  @Inject(method = "renderMapHand", at = @At("HEAD"), cancellable = true)
  private void mobLife$renderMorphMapHand(
      PoseStack poseStack,
      SubmitNodeCollector collector,
      int lightCoords,
      HumanoidArm arm,
      CallbackInfo ci) {
    if (FirstPersonMorphHandRenderer.shouldHideHandWithItem()) {
      ci.cancel();
    }
  }
}
