package cc.attodao.mob_life.gameplay.instinct;

import java.util.function.Supplier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

/** Direction-constrained vanilla land-position selection for player-directed wandering. */
final class InstinctWandering {
  private static final int CANDIDATE_COUNT = 12;
  private static final double MINIMUM_HORIZONTAL_MOVEMENT_SQR = 1.0E-4;

  private InstinctWandering() {}

  static Vec3 directionalDestination(
      PathfinderMob shadow,
      Supplier<Vec3> nativeDestination,
      float headingYaw,
      float maximumDeviationDegrees,
      Vec3 anchor,
      double maximumAnchorDistance) {
    Vec3 direction = directionFromYaw(headingYaw);
    for (int index = 0; index < CANDIDATE_COUNT; index++) {
      Vec3 candidate = nativeDestination.get();
      if (candidate == null) {
        continue;
      }
      Vec3 fromAnchor =
          anchor == null ? Vec3.ZERO : candidate.subtract(anchor).multiply(1.0, 0.0, 1.0);
      if (anchor != null
          && fromAnchor.lengthSqr() > maximumAnchorDistance * maximumAnchorDistance) {
        continue;
      }
      Vec3 towardCandidate = candidate.subtract(shadow.position()).multiply(1.0, 0.0, 1.0);
      if (towardCandidate.lengthSqr() < MINIMUM_HORIZONTAL_MOVEMENT_SQR) {
        continue;
      }
      double alignment = direction.dot(towardCandidate.normalize());
      double angle = Math.acos(Math.clamp(alignment, -1.0, 1.0)) * Mth.RAD_TO_DEG;
      if (angle > maximumDeviationDegrees) {
        continue;
      }
      return candidate;
    }
    return null;
  }

  private static Vec3 directionFromYaw(float yaw) {
    float radians = yaw * Mth.DEG_TO_RAD;
    return new Vec3(-Mth.sin(radians), 0.0, Mth.cos(radians));
  }
}
