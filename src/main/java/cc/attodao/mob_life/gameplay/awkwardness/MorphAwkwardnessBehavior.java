package cc.attodao.mob_life.gameplay.awkwardness;

import cc.attodao.mob_life.gameplay.instinct.InstinctManager;
import cc.attodao.mob_life.gameplay.targeting.MorphNearbyEntities;
import cc.attodao.mob_life.gameplay.targeting.MorphPredation;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/** Small species-specific adjustments grounded in the source mob's social and threat behavior. */
public final class MorphAwkwardnessBehavior {

  private MorphAwkwardnessBehavior() {}

  public static float herdDecayMultiplier(MorphType morph) {
    if (morph == null) {
      return 1.0F;
    }
    return switch (morph) {
      // Herd animals are markedly calmer near their own kind.
      case COW, SHEEP -> 1.75F;
      // Wolves are social, but retain independent hunting behavior.
      case WOLF -> 0.75F;
      // Cats do not derive a meaningful social penalty from solitary behavior.
      case CAT -> 0.0F;
      default -> 1.0F;
    };
  }

  public static float threatGainPerSecond(ServerPlayer player, MorphType morph) {
    if (morph != MorphType.RABBIT || !isRecognizedThreat(player, morph)) {
      return 0.0F;
    }
    // Rabbit AvoidEntity goals are an immediate response to foxes, wolves, and similar threats.
    return 0.5F;
  }

  private static boolean isRecognizedThreat(ServerPlayer player, MorphType morph) {
    if (InstinctManager.isThreatResponse(player)) {
      return true;
    }
    // Only react to a real mob target acquired by vanilla goal logic, never a raw radius match.
    return MorphNearbyEntities.living(player, 128.0).stream()
        .filter(Mob.class::isInstance)
        .map(Mob.class::cast)
        .anyMatch(mob -> mob.getTarget() == player && MorphPredation.isPredatorFor(mob, morph));
  }
}
