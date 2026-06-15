package cc.attodao.mob_life.client.mixin.render;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.client.state.ClientVisionPass;
import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardness;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.util.Mth;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PostPass.class)
public abstract class PostPassDistanceUniformMixin {
  private static final int DISTANCE_BLUR_UBO_SIZE = 128;
  private static final float NEAR_PLANE = 0.05F;

  @Inject(method = "addToFrame", at = @At("HEAD"))
  private void mobLife$updateDistanceBlur(CallbackInfo ci) {
    if (ClientMorphState.morph() == null) {
      return;
    }

    PostPassAccessor accessor = (PostPassAccessor) this;
    if (!accessor.mobLife$getName().startsWith(MobLife.MOD_ID + ":")) {
      return;
    }

    Map<String, GpuBuffer> customUniforms = accessor.mobLife$getCustomUniforms();
    GpuBuffer buffer = customUniforms.get("DistanceBlur");
    if (buffer == null) {
      return;
    }
    if ((buffer.usage() & GpuBuffer.USAGE_COPY_DST) == 0
        || buffer.size() < DISTANCE_BLUR_UBO_SIZE) {
      GpuBuffer writableBuffer =
          RenderSystem.getDevice()
              .createBuffer(
                  () -> "Mob Life dynamic distance blur uniform",
                  GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                  DISTANCE_BLUR_UBO_SIZE);
      customUniforms.put("DistanceBlur", writableBuffer);
      buffer.close();
      buffer = writableBuffer;
    }

    Minecraft client = Minecraft.getInstance();
    MorphConfig.Vision vision = MorphConfigManager.get(ClientMorphState.morph()).vision();
    float farPlane =
        Math.max(vision.fullFogDistance(), client.options.getEffectiveRenderDistance() * 16.0F);
    float skyBrightness =
        client.level == null
            ? 1.0F
            : 1.0F - Mth.clamp(client.level.getSkyDarken() / 15.0F, 0.0F, 1.0F);
    float interference = MorphAwkwardness.visionInterference(ClientMorphState.awkwardness());

    try (MemoryStack stack = MemoryStack.stackPush()) {
      Std140Builder data =
          Std140Builder.onStack(stack, DISTANCE_BLUR_UBO_SIZE)
              .putVec4(
                  vision.effectStartDistance(),
                  vision.fullBlurDistance(),
                  vision.fullDarkeningDistance(),
                  vision.fullFogDistance())
              .putVec4(
                  vision.maximumBlurRadius() * (1.0F + interference),
                  skyBrightness,
                  vision.peripheralEdgeBrightness(),
                  vision.hazeStrength())
              .putVec4(
                  NEAR_PLANE,
                  farPlane,
                  ClientVisionPass.isDistancePass() ? 1.0F : 0.0F,
                  interference)
              .putVec4(
                  vision.retainedSaturation(),
                  vision.contrast(),
                  vision.brightness(),
                  vision.peripheralBlurRadius())
              .putVec4(
                  vision.redResponse().red(),
                  vision.redResponse().green(),
                  vision.redResponse().blue(),
                  0.0F)
              .putVec4(
                  vision.greenResponse().red(),
                  vision.greenResponse().green(),
                  vision.greenResponse().blue(),
                  0.0F)
              .putVec4(
                  vision.blueResponse().red(),
                  vision.blueResponse().green(),
                  vision.blueResponse().blue(),
                  0.0F)
              .putVec4(vision.peripheralStart(), vision.lowLightBrightness(), 0.0F, 1.0F);
      RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), data.get());
    }
  }
}
