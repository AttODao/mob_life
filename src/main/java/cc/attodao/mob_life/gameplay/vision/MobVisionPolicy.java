package cc.attodao.mob_life.gameplay.vision;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardness;
import java.util.List;
import net.minecraft.util.Mth;

/** Deterministic frame values and std140 layout for Mob vision rendering. */
public final class MobVisionPolicy {
  public static final String DISTANCE_UNIFORM = "DistanceBlur";
  public static final String INSTINCT_UNIFORM = "InstinctVignette";
  public static final List<String> DISTANCE_MEMBERS =
      List.of(
          "Parameters",
          "Effects",
          "DepthRange",
          "ConfigOverrides",
          "DynamicRedResponse",
          "DynamicGreenResponse",
          "DynamicBlueResponse",
          "VisionBehavior");
  public static final List<String> INSTINCT_MEMBERS = List.of("Color");
  public static final int VEC4_BYTES = 16;
  public static final int DISTANCE_UNIFORM_BYTES = DISTANCE_MEMBERS.size() * VEC4_BYTES;
  public static final int INSTINCT_UNIFORM_BYTES = INSTINCT_MEMBERS.size() * VEC4_BYTES;

  private static final float NEAR_PLANE = 0.05F;
  private static final float MIN_DISTANCE_EFFECT_START = 8.0F;
  private static final float PERIPHERAL_BLUR_MULTIPLIER = 16.0F;

  private MobVisionPolicy() {}

  public record Environment(
      float renderDistanceBlocks,
      float skyDarken,
      float awkwardness,
      boolean instinctActive,
      float instinctLevel) {}

  public record Vec4(float x, float y, float z, float w) {}

  public record DistanceUniform(
      Vec4 parameters,
      Vec4 effects,
      Vec4 depthRange,
      Vec4 configOverrides,
      Vec4 redResponse,
      Vec4 greenResponse,
      Vec4 blueResponse,
      Vec4 visionBehavior) {
    public List<Vec4> values() {
      return List.of(
          parameters,
          effects,
          depthRange,
          configOverrides,
          redResponse,
          greenResponse,
          blueResponse,
          visionBehavior);
    }
  }

  public record Frame(DistanceUniform distance, Vec4 instinctVignette) {}

  public static Frame derive(MorphConfig.Vision vision, Environment environment) {
    float peripheralStrength = Mth.clamp(2.0F - vision.fieldOfViewMultiplier(), 0.0F, 1.0F);
    float farPlane = Math.max(vision.fullFogDistance(), environment.renderDistanceBlocks());
    float skyBrightness = 1.0F - Mth.clamp(environment.skyDarken() / 15.0F, 0.0F, 1.0F);
    float interference = MorphAwkwardness.visionInterference(environment.awkwardness());
    float distanceStart = Math.max(vision.effectStartDistance(), MIN_DISTANCE_EFFECT_START);
    float distanceOffset = distanceStart - vision.effectStartDistance();
    float fullBlurDistance =
        Math.max(distanceStart + 1.0F, vision.fullBlurDistance() + distanceOffset);
    float fullDarkeningDistance =
        Math.max(fullBlurDistance + 1.0F, vision.fullDarkeningDistance() + distanceOffset);
    float fullFogDistance =
        Math.max(fullDarkeningDistance + 1.0F, vision.fullFogDistance() + distanceOffset);
    float peripheralBlurRadius =
        vision.peripheralBlurRadius() * peripheralStrength * PERIPHERAL_BLUR_MULTIPLIER;
    DistanceUniform distance =
        new DistanceUniform(
            new Vec4(distanceStart, fullBlurDistance, fullDarkeningDistance, fullFogDistance),
            new Vec4(
                vision.maximumBlurRadius() * (1.0F + interference),
                skyBrightness,
                1.0F,
                vision.hazeStrength()),
            new Vec4(NEAR_PLANE, farPlane, 0.0F, interference),
            new Vec4(
                vision.retainedSaturation(),
                vision.contrast(),
                vision.brightness(),
                peripheralBlurRadius),
            color(vision.redResponse()),
            color(vision.greenResponse()),
            color(vision.blueResponse()),
            new Vec4(vision.peripheralStart(), vision.lowLightBrightness(), 0.0F, 0.0F));
    float instinctStrength =
        environment.instinctActive()
            ? 0.55F * Mth.clamp(environment.instinctLevel() / 100.0F, 0.0F, 1.0F)
            : 0.0F;
    return new Frame(
        distance, new Vec4(63.0F / 255.0F, 40.0F / 255.0F, 24.0F / 255.0F, instinctStrength));
  }

  private static Vec4 color(MorphConfig.ColorResponse response) {
    return new Vec4(response.red(), response.green(), response.blue(), 0.0F);
  }
}
