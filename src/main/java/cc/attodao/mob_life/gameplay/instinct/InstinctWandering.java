package cc.attodao.mob_life.gameplay.instinct;

import cc.attodao.mob_life.config.MorphConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

/** Direction-biased vanilla land-position selection used by wander and player intervention. */
final class InstinctWandering {
  private static final int CANDIDATE_COUNT = 12;
  private static final double MINIMUM_HORIZONTAL_MOVEMENT_SQR = 1.0E-4;
  private static final float DIRECTION_INTENT_WEIGHT = 0.7F;
  private static final float PRIMARY_CONE_DEGREES = 15.0F;
  private static final float FALLBACK_CONE_DEGREES = 45.0F;

  private InstinctWandering() {}

  static Vec3 destination(
      PathfinderMob shadow,
      MorphConfig.Wander wander,
      float headingYaw,
      float intentStrength,
      Vec3 anchor) {
    Vec3 direction = directionFromYaw(headingYaw);
    float directionWeight = Mth.lerp(intentStrength, wander.gazeWeight(), DIRECTION_INTENT_WEIGHT);
    float primaryConeDegrees = Mth.lerp(intentStrength, 180.0F, PRIMARY_CONE_DEGREES);
    float fallbackConeDegrees = Mth.lerp(intentStrength, 180.0F, FALLBACK_CONE_DEGREES);
    Vec3 primary = null;
    Vec3 fallback = null;
    Vec3 unrestricted = null;
    double primaryScore = Double.NEGATIVE_INFINITY;
    double fallbackScore = Double.NEGATIVE_INFINITY;
    double unrestrictedScore = Double.NEGATIVE_INFINITY;
    for (int index = 0; index < CANDIDATE_COUNT; index++) {
      Vec3 candidate =
          index == 0
              ? LandRandomPos.getPosTowards(
                  shadow,
                  wander.horizontalRange(),
                  wander.verticalRange(),
                  shadow.position().add(direction.scale(wander.horizontalRange())))
              : LandRandomPos.getPos(shadow, wander.horizontalRange(), wander.verticalRange());
      if (candidate == null) {
        continue;
      }
      Vec3 fromAnchor =
          anchor == null ? Vec3.ZERO : candidate.subtract(anchor).multiply(1.0, 0.0, 1.0);
      if (anchor != null
          && fromAnchor.lengthSqr() > wander.horizontalRange() * wander.horizontalRange()) {
        continue;
      }
      Vec3 towardCandidate = candidate.subtract(shadow.position()).multiply(1.0, 0.0, 1.0);
      if (towardCandidate.lengthSqr() < MINIMUM_HORIZONTAL_MOVEMENT_SQR) {
        continue;
      }
      double alignment = direction.dot(towardCandidate.normalize());
      double distanceScore =
          1.0 - Math.min(1.0, towardCandidate.length() / Math.max(1.0, wander.horizontalRange()));
      double score =
          directionWeight * ((alignment + 1.0) * 0.5) + (1.0F - directionWeight) * distanceScore;
      if (score > unrestrictedScore) {
        unrestrictedScore = score;
        unrestricted = candidate;
      }
      double angle = Math.acos(Math.clamp(alignment, -1.0, 1.0)) * Mth.RAD_TO_DEG;
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
    float radians = yaw * Mth.DEG_TO_RAD;
    return new Vec3(-Mth.sin(radians), 0.0, Mth.cos(radians));
  }
}
