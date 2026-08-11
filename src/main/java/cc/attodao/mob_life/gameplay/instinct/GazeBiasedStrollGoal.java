package cc.attodao.mob_life.gameplay.instinct;

import cc.attodao.mob_life.config.MorphConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

final class GazeBiasedStrollGoal extends RandomStrollGoal {
  private static final int CANDIDATE_COUNT = 6;

  private final ServerPlayer player;
  private final MorphConfig.Wander config;

  GazeBiasedStrollGoal(
      PathfinderMob mob, ServerPlayer player, MorphConfig.Wander config, double speedModifier) {
    super(mob, speedModifier, config.intervalTicks(), true);
    this.player = player;
    this.config = config;
  }

  @Override
  protected Vec3 getPosition() {
    Vec3 gaze = player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
    Vec3 best = null;
    double bestScore = Double.NEGATIVE_INFINITY;
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
      double score = config.gazeWeight() * alignment + (1.0F - config.gazeWeight()) * nativeScore;
      if (score > bestScore) {
        bestScore = score;
        best = candidate;
      }
    }
    return best;
  }
}
