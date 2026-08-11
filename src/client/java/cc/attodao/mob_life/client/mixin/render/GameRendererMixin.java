package cc.attodao.mob_life.client.mixin.render;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.config.MobLifeConfig;
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
  private static final Identifier BASE_VISION_POST_EFFECT = MobLife.id("vision_base");
  private static final Identifier DISTANCE_VISION_POST_EFFECT = MobLife.id("vision_distance");

  @Shadow @Nullable private Identifier postEffectId;

  @Shadow private boolean effectActive;

  @Shadow private Minecraft minecraft;

  @Shadow private CrossFrameResourcePool resourcePool;

  @Inject(method = "checkEntityPostEffect", at = @At("HEAD"), cancellable = true)
  private void mobLife$selectMorphVision(@Nullable Entity cameraEntity, CallbackInfo ci) {
    if (!MobLifeConfig.shaderEnabled()) {
      return;
    }

    if (ClientMorphState.morph() == null) {
      return;
    }

    postEffectId = BASE_VISION_POST_EFFECT;
    effectActive = true;
    ci.cancel();
  }

  @Inject(
      method = "renderLevel",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/renderer/LevelRenderer;render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
              shift = At.Shift.AFTER))
  private void mobLife$applyDistanceVisionAfterLevelRender(
      net.minecraft.client.DeltaTracker deltaTracker, CallbackInfo ci) {
    if (!effectActive || ClientMorphState.morph() == null) {
      return;
    }

    PostChain chain =
        minecraft
            .getShaderManager()
            .getPostChain(
                DISTANCE_VISION_POST_EFFECT,
                net.minecraft.client.renderer.LevelTargetBundle.MAIN_TARGETS);
    if (chain == null) {
      return;
    }

    chain.process(minecraft.gameRenderer.mainRenderTarget(), resourcePool);
  }
}
