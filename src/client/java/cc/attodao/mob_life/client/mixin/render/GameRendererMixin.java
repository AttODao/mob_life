package cc.attodao.mob_life.client.mixin.render;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.client.state.ClientVisionPass;
import cc.attodao.mob_life.config.MobLifeConfig;
import cc.attodao.mob_life.morph.MorphType;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
  @Shadow @Nullable private Identifier postEffectId;

  @Shadow private boolean effectActive;

  @Shadow private Minecraft minecraft;

  @Shadow private CrossFrameResourcePool resourcePool;

  @Inject(method = "checkEntityPostEffect", at = @At("HEAD"), cancellable = true)
  private void mobLife$selectMorphVision(@Nullable Entity cameraEntity, CallbackInfo ci) {
    if (!MobLifeConfig.shaderEnabled()) {
      return;
    }

    MorphType morph = ClientMorphState.morph();
    if (morph == null) {
      return;
    }

    postEffectId =
        Identifier.fromNamespaceAndPath(MobLife.MOD_ID, morph.visionProfileId() + "_vision");
    effectActive = true;
    ci.cancel();
  }

  @Inject(
      method = "renderLevel",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;ZLnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;)V",
              shift = At.Shift.AFTER))
  private void mobLife$applyDistanceVisionAfterLevelRender(
      net.minecraft.client.DeltaTracker deltaTracker, CallbackInfo ci) {
    if (postEffectId == null || !effectActive || ClientMorphState.morph() == null) {
      return;
    }

    PostChain chain =
        minecraft
            .getShaderManager()
            .getPostChain(
                postEffectId, net.minecraft.client.renderer.LevelTargetBundle.MAIN_TARGETS);
    if (chain == null) {
      return;
    }

    mobLife$applyVisionPass(chain, true);
  }

  private void mobLife$applyVisionPass(PostChain chain, boolean distancePass) {
    if (ClientMorphState.morph() == null) {
      return;
    }

    ClientVisionPass.setDistancePass(distancePass);
    try {
      chain.process(minecraft.getMainRenderTarget(), resourcePool);
    } finally {
      ClientVisionPass.setDistancePass(false);
    }
  }
}
