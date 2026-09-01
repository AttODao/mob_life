package cc.attodao.mob_life.gameplay.awkwardness;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.gameplay.targeting.MorphNearbyEntities;
import cc.attodao.mob_life.gameplay.targeting.MorphPredation;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/** Species and environmental sources that add awkwardness over time. */
public final class MorphAwkwardnessBehavior {

  public static final float UNFAVORABLE_LIGHT_GAIN_PER_SECOND = 0.2F;
  private static final int BRIGHT_LIGHT_LEVEL = 8;

  private MorphAwkwardnessBehavior() {}

  public static float threatGainPerSecond(ServerPlayer player, MorphType morph) {
    if (morph != MorphType.RABBIT || !isRecognizedThreat(player, morph)) {
      return 0.0F;
    }
    // Rabbit AvoidEntity goals are an immediate response to foxes, wolves, and similar threats.
    return 0.5F;
  }

  /** Returns the fixed circadian mismatch cost using the player's local vanilla light level. */
  public static float unfavorableLightGainPerSecond(
      ServerPlayer player, MorphConfig.SleepSchedule schedule) {
    if (schedule == null) {
      return 0.0F;
    }

    int lightLevel = player.level().getMaxLocalRawBrightness(player.blockPosition());
    return switch (schedule) {
      case DAY -> lightLevel >= BRIGHT_LIGHT_LEVEL ? UNFAVORABLE_LIGHT_GAIN_PER_SECOND : 0.0F;
      case NORMAL -> lightLevel < BRIGHT_LIGHT_LEVEL ? UNFAVORABLE_LIGHT_GAIN_PER_SECOND : 0.0F;
      case NEVER -> 0.0F;
    };
  }

  private static boolean isRecognizedThreat(ServerPlayer player, MorphType morph) {
    // Only react to a real mob target acquired by vanilla goal logic, never a raw radius match.
    return MorphNearbyEntities.living(player, 128.0).stream()
        .filter(Mob.class::isInstance)
        .map(Mob.class::cast)
        .anyMatch(mob -> mob.getTarget() == player && MorphPredation.isPredatorFor(mob, morph));
  }
}
