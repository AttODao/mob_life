package cc.attodao.mob_life.gameplay.awkwardness;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public final class MorphAwkwardness {

  public static final float MINIMUM = 0.0F;
  public static final float MAXIMUM = 100.0F;
  public static final float SLEEP_THRESHOLD = 30.0F;
  public static final float HOSTILE_DETECTION_START = 30.0F;
  public static final float HOSTILE_THRESHOLD = 70.0F;
  public static final float ACTION_LOCK_THRESHOLD = 90.0F;

  private MorphAwkwardness() {}

  public static float get(Player player) {
    return ((AwkwardnessHolder) player).mobLife$getAwkwardness();
  }

  public static float set(Player player, float value) {
    float clamped = Mth.clamp(value, MINIMUM, MAXIMUM);
    ((AwkwardnessHolder) player).mobLife$setAwkwardness(clamped);
    return clamped;
  }

  public static float add(Player player, float amount) {
    return set(player, get(player) + amount);
  }

  public static float exhaustionMultiplier(Player player) {
    return 1.0F + (2.0F * get(player)) / MAXIMUM;
  }

  public static boolean canSleepWithoutBed(Player player) {
    return get(player) < SLEEP_THRESHOLD;
  }

  public static float hostileDetectionScale(Player player) {
    return Mth.clamp(
        (get(player) - HOSTILE_DETECTION_START) / (HOSTILE_THRESHOLD - HOSTILE_DETECTION_START),
        0.0F,
        1.0F);
  }

  public static boolean blocksWorldInteraction(Player player) {
    return get(player) >= ACTION_LOCK_THRESHOLD;
  }

  public static float visionInterference(float awkwardness) {
    return (1.0F
        + Mth.clamp((awkwardness - HOSTILE_THRESHOLD) / (MAXIMUM - HOSTILE_THRESHOLD), 0.0F, 1.0F));
  }

  public static float normalized(float awkwardness) {
    return Mth.clamp((awkwardness - MINIMUM) / (MAXIMUM - MINIMUM), 0.0F, 1.0F);
  }
}
