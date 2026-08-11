package cc.attodao.mob_life.gameplay.instinct;

import cc.attodao.mob_life.config.MorphConfig;
import java.util.function.DoubleSupplier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

final class GazeBiasedStrollGoal extends RandomStrollGoal {
  private static final int CANDIDATE_COUNT = 12;
  private static final float DIRECTION_INTENT_WEIGHT = 0.7F;
  private static final float DIRECTION_INTENT_PRIMARY_CONE_DEGREES = 15.0F;
  private static final float DIRECTION_INTENT_FALLBACK_CONE_DEGREES = 45.0F;

  private final DoubleSupplier directionYaw;
  private final DoubleSupplier directionIntentStrength;
  private final MorphConfig.Wander config;

  GazeBiasedStrollGoal(
      PathfinderMob mob,
      DoubleSupplier directionYaw,
      DoubleSupplier directionIntentStrength,
      MorphConfig.Wander config,
      double speedModifier) {
    super(mob, speedModifier, config.intervalTicks(), true);
    this.directionYaw = directionYaw;
    this.directionIntentStrength = directionIntentStrength;
    this.config = config;
  }

  @Override
  protected Vec3 getPosition() {
    Vec3 gaze = directionFromYaw((float) directionYaw.getAsDouble());
    float intentStrength = Math.clamp((float) directionIntentStrength.getAsDouble(), 0.0F, 1.0F);
    float directionWeight = Mth.lerp(intentStrength, config.gazeWeight(), DIRECTION_INTENT_WEIGHT);
    float primaryConeDegrees =
        Mth.lerp(intentStrength, 180.0F, DIRECTION_INTENT_PRIMARY_CONE_DEGREES);
    float fallbackConeDegrees =
        Mth.lerp(intentStrength, 180.0F, DIRECTION_INTENT_FALLBACK_CONE_DEGREES);
    Vec3 primary = null;
    Vec3 fallback = null;
    Vec3 unrestricted = null;
    double primaryScore = Double.NEGATIVE_INFINITY;
    double fallbackScore = Double.NEGATIVE_INFINITY;
    double unrestrictedScore = Double.NEGATIVE_INFINITY;
    for (int index = 0; index < CANDIDATE_COUNT; index++) {
      Vec3 candidate =
          index == 0 && gaze.lengthSqr() > 1.0E-4
              ? LandRandomPos.getPosTowards(
                  mob,
                  config.horizontalRange(),
                  config.verticalRange(),
                  mob.position().add(gaze.scale(config.horizontalRange())))
              : LandRandomPos.getPos(mob, config.horizontalRange(), config.verticalRange());
      if (candidate == null) {
        continue;
      }

      Vec3 direction = candidate.subtract(mob.position()).multiply(1.0, 0.0, 1.0);
      double alignment =
          gaze.lengthSqr() < 1.0E-4 || direction.lengthSqr() < 1.0E-4
              ? 0.5
              : (gaze.dot(direction.normalize()) + 1.0) * 0.5;
      double nativeScore =
          1.0 - Math.min(1.0, direction.length() / Math.max(1.0, config.horizontalRange()));
      double score = directionWeight * alignment + (1.0F - directionWeight) * nativeScore;
      if (score > unrestrictedScore) {
        unrestrictedScore = score;
        unrestricted = candidate;
      }
      double angle =
          Math.acos(Math.clamp(gaze.dot(direction.normalize()), -1.0, 1.0)) * Mth.RAD_TO_DEG;
      if (angle <= fallbackConeDegrees && score > fallbackScore) {
        fallbackScore = score;
        fallback = candidate;
      }
      if (angle <= primaryConeDegrees && score > primaryScore) {
        primaryScore = score;
        primary = candidate;
      }
    }
    return primary != null ? primary : fallback != null ? fallback : unrestricted;
  }

  private static Vec3 directionFromYaw(float yaw) {
    float radians = yaw * (float) (Math.PI / 180.0);
    return new Vec3(-Math.sin(radians), 0.0, Math.cos(radians));
  }
}
