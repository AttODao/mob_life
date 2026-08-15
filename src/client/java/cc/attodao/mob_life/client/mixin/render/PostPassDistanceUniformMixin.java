package cc.attodao.mob_life.client.mixin.render;

import cc.attodao.mob_life.client.state.ClientInstinctState;
import cc.attodao.mob_life.client.state.ClientMorphState;
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
  private static final float MIN_DISTANCE_EFFECT_START = 8.0F;
  private static final float PERIPHERAL_BLUR_MULTIPLIER = 16.0F;

  @Inject(method = "addToFrame", at = @At("HEAD"))
  private void mobLife$updateDistanceBlur(CallbackInfo ci) {
    if (ClientMorphState.morph() == null) {
      return;
    }

    PostPassAccessor accessor = (PostPassAccessor) this;
    Map<String, GpuBuffer> customUniforms = accessor.mobLife$getCustomUniforms();
    if (!customUniforms.containsKey("DistanceBlur")) {
      return;
    }
    GpuBuffer distanceBlur =
        mobLife$writableUniform(customUniforms, "DistanceBlur", DISTANCE_BLUR_UBO_SIZE);
    if (distanceBlur == null) {
      return;
    }

    Minecraft client = Minecraft.getInstance();
    MorphConfig config = MorphConfigManager.get(ClientMorphState.morph());
    MorphConfig.Vision vision = config.vision();
    MorphConfig.VisualEffect visualEffect = config.instinct().visualEffect();
    float instinctStrength =
        visualEffect.enabled() ? visualEffect.strength() * ClientInstinctState.visualBlend() : 0.0F;
    float peripheralStrength = Mth.clamp(2.0F - vision.fieldOfViewMultiplier(), 0.0F, 1.0F);
    float farPlane =
        Math.max(vision.fullFogDistance(), client.options.getEffectiveRenderDistance() * 16.0F);
    float skyBrightness =
        client.level == null
            ? 1.0F
            : 1.0F - Mth.clamp(client.level.getSkyDarken() / 15.0F, 0.0F, 1.0F);
    float interference = MorphAwkwardness.visionInterference(ClientMorphState.awkwardness());
    float instinctLevel = ClientInstinctState.instinctLevelRatio();
    float distanceStart = Math.max(vision.effectStartDistance(), MIN_DISTANCE_EFFECT_START);
    float distanceOffset = distanceStart - vision.effectStartDistance();
    float fullBlurDistance =
        Math.max(distanceStart + 1.0F, vision.fullBlurDistance() + distanceOffset);
    float fullDarkeningDistance =
        Math.max(fullBlurDistance + 1.0F, vision.fullDarkeningDistance() + distanceOffset);
    float fullFogDistance =
        Math.max(fullDarkeningDistance + 1.0F, vision.fullFogDistance() + distanceOffset);
    float peripheralBlurRadius =
        vision.peripheralBlurRadius()
            * peripheralStrength
            * PERIPHERAL_BLUR_MULTIPLIER
            * (1.0F - 0.25F * instinctStrength);
    float peripheralEdgeBrightness = 1.0F;
    try (MemoryStack stack = MemoryStack.stackPush()) {
      Std140Builder distanceBlurData =
          Std140Builder.onStack(stack, DISTANCE_BLUR_UBO_SIZE)
              .putVec4(distanceStart, fullBlurDistance, fullDarkeningDistance, fullFogDistance)
              .putVec4(
                  vision.maximumBlurRadius()
                      * (1.0F + interference)
                      * (1.0F - 0.25F * instinctStrength),
                  skyBrightness,
                  peripheralEdgeBrightness,
                  vision.hazeStrength())
              .putVec4(NEAR_PLANE, farPlane, 0.0F, interference)
              .putVec4(
                  vision.retainedSaturation(),
                  vision.contrast(),
                  vision.brightness(),
                  peripheralBlurRadius)
              .putVec4(
                  vision.redResponse().red(),
                  vision.redResponse().green(),
                  vision.redResponse().blue(),
                  0.0F)
              .putVec4(
                  vision.greenResponse().red(),
                  vision.greenResponse().green(),
                  vision.greenResponse().blue(),
                  instinctStrength)
              .putVec4(
                  vision.blueResponse().red(),
                  vision.blueResponse().green(),
                  vision.blueResponse().blue(),
                  instinctLevel)
              .putVec4(
                  vision.peripheralStart(),
                  vision.lowLightBrightness(),
                  0.0F,
                  ClientInstinctState.interventionBlockedVisualBlend());
      var encoder = RenderSystem.getDevice().createCommandEncoder();
      encoder.writeToBuffer(distanceBlur.slice(), distanceBlurData.get());
    }
  }

  private static GpuBuffer mobLife$writableUniform(
      Map<String, GpuBuffer> customUniforms, String name, int size) {
    GpuBuffer buffer = customUniforms.get(name);
    if (buffer == null) {
      return null;
    }
    if ((buffer.usage() & GpuBuffer.USAGE_COPY_DST) != 0 && buffer.size() >= size) {
      return buffer;
    }

    GpuBuffer writableBuffer =
        RenderSystem.getDevice()
            .createBuffer(
                () -> "Mob Life dynamic " + name + " uniform",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                size);
    customUniforms.put(name, writableBuffer);
    buffer.close();
    return writableBuffer;
  }
}
